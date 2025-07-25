package org.example.matrix.index;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.schema.model.DataType;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.matrix.fc.IdentityInfo;
import org.example.matrix.fc.KMEntityConverterEx;
import org.example.matrix.fc.Persistable2;
import org.example.matrix.fc.PersistenceHelper;
import org.example.matrix.services.StandardService;
import org.example.matrix.utils.function.FunctionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StandardIndexService extends StandardService implements IndexService {

    @Resource
    private ChatClient chatClient;
    @Resource
    private WeaviateClient weaviateClient;

    @Override
    public <T extends Persistable2> void build(Class<T> clazz) {
        Result<Boolean> existsResult = weaviateClient.schema().exists().withClassName(PersistenceHelper.getEntityName(clazz)).run();
        Assert.isFalse(existsResult.hasErrors(), () -> new IllegalArgumentException(existsResult.getError().toString()));

        if (existsResult.getResult()) {
            return;
        }

        List<Property> properties = CollUtil.newArrayList();
        properties.addAll(List.of(
                Property.builder().name("content").dataType(List.of(DataType.TEXT)).description("内容").build(),
                Property.builder().name("metadata").dataType(List.of(DataType.TEXT)).description("属性").build()
        ));

        List<Property> filterMetadataProperties = List.of(
                Property.builder().name("meta_number").dataType(List.of(DataType.TEXT)).description("编号").build(),
                Property.builder().name("meta_name").dataType(List.of(DataType.TEXT)).description("名称").build(),
                Property.builder().name("meta_description").dataType(List.of(DataType.TEXT)).description("描述").build()
        );
        properties.addAll(filterMetadataProperties);

        WeaviateClass weaviateClazz = WeaviateClass.builder().className(PersistenceHelper.getEntityName(clazz)).vectorizer("none").properties(properties).build();
        Result<Boolean> classCreatorResult = weaviateClient.schema().classCreator().withClass(weaviateClazz).run();
        Assert.isFalse(classCreatorResult.hasErrors(), () -> new IllegalArgumentException(classCreatorResult.getError().toString()));

    }

    @Override
    public <T extends Persistable2> void rebuild(Class<T> clazz) {
        Result<Boolean> existsResult = weaviateClient.schema().exists().withClassName(PersistenceHelper.getEntityName(clazz)).run();
        Assert.isFalse(existsResult.hasErrors(), () -> new IllegalArgumentException(existsResult.getError().toString()));

        if (existsResult.getResult()) {
            Result<Boolean> classDeleterResult = weaviateClient.schema().classDeleter().withClassName(PersistenceHelper.getEntityName(clazz)).run();
            Assert.isFalse(classDeleterResult.hasErrors(), () -> new IllegalArgumentException(classDeleterResult.getError().toString()));
        }
        this.build(clazz);
    }

    @Override
    public <T extends Persistable2> Document index(T entity) {
        if (this.indexed(entity)) {
            this.deindex(entity);
        }
        VectorStore vectorStore = WeaviateVectorStoreFactory.getVectorStore(this.weaviateClient, entity.getClass());
        Document document = EntityToDocumentConvertor.toDocument(entity);
        vectorStore.add(List.of(document));
        return document;
    }

    @Override
    public <T extends Persistable2> List<Document> index(List<? extends T> entities) {
        return entities.stream().map(this::index).toList();
    }

    @Override
    public <T extends Persistable2> void deindex(T entity) {
        if (!this.indexed(entity)) {
            return;
        }
        VectorStore vectorStore = WeaviateVectorStoreFactory.getVectorStore(this.weaviateClient, entity.getClass());
        String documentId = entity.getIdentityInfo().getUUIDformat();
        vectorStore.delete(List.of(documentId));
    }

    @Override
    public <T extends Persistable2> void deindex(List<? extends T> entities) {
        entities.forEach(this::deindex);
    }

    @Override
    public <T extends Persistable2> boolean indexed(T entity) {
        VectorStore vectorStore = WeaviateVectorStoreFactory.getVectorStore(this.weaviateClient, entity.getClass());
        WeaviateClient weaviateClient = (WeaviateClient) vectorStore.getNativeClient().orElseThrow();

        String documentId = entity.getIdentityInfo().getUUIDformat();
        Result<Boolean> checkerResult = weaviateClient.data().checker().withClassName(PersistenceHelper.getEntityName(entity)).withID(documentId).run();
        Assert.isFalse(checkerResult.hasErrors(), () -> new IllegalArgumentException(checkerResult.getError().toString()));
        return checkerResult.getResult();
    }

    @Override
    public <T extends Persistable2> List<T> search(Class<T> clazz, String query) {
        return this.search(clazz, query, 2000);
    }

    @Override
    public <T extends Persistable2> List<T> search(Class<T> clazz, String query, long limit) {
        VectorStore vectorStore = WeaviateVectorStoreFactory.getVectorStore(this.weaviateClient, clazz);

        String filterExpressionText = FunctionUtils.get(() -> {
            String template = """
                    分析用户查询，提取针对{entityName}实体的过滤条件。
                    实体可用字段: {entityFields}
                    用户查询: {query}
                    仅返回过滤条件表达式，使用 SpringAI VectorStore 规范的 textExpression 过滤表达式，不包含其他内容。
                    
                    语法规则：
                    - 支持的比较运算符：==、!=、>、<、>=、<=、IN、NIN
                    - 支持的逻辑运算符：AND、OR、NOT（优先级：() > NOT > AND > OR）
                    - 字符串值必须用双引号包裹
                    - 集合用[]表示（如 [1,2,3] 或 ['a','b']）
                    - 数字和布尔值直接书写（如 999、true）
                   
                    如果没有过滤条件，或者语法规则不支持，则返回空。
                    """;
            PromptTemplate promptTemplate = PromptTemplate.builder().template(template).variables(Map.of(
                    "entityName", PersistenceHelper.getEntityName(clazz),
                    "entityFields", "number, name, description",
                    "query", query
            )).build();
            Prompt prompt = promptTemplate.create();
            return chatClient.prompt(prompt).call().content();
        });

        String searchText = FunctionUtils.get(() -> {
            String template = """
                    提取用户查询中的核心搜索内容，忽略过滤条件。
                    用户查询: {query}

                    仅返回用于相似度搜索的核心文本，不包含其他内容。
                    """;
            PromptTemplate promptTemplate = PromptTemplate.builder().template(template).variables(Map.of("query", query)).build();
            Prompt prompt = promptTemplate.create();
            return chatClient.prompt(prompt).call().content();
        });

        SearchRequest searchRequest = SearchRequest.builder().query(searchText).filterExpression(filterExpressionText).topK((int) limit).build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        return documents.stream().<T>map(document -> {
            Map<String, Object> metadata = document.getMetadata();

            String classname = MapUtil.getStr(metadata, "classname");
            long id = MapUtil.getLong(metadata, "id");
            IdentityInfo identityInfo = IdentityInfo.build(classname, id);
            return KMEntityConverterEx.newInstance(identityInfo.getJavaClazz(), metadata);
        }).toList();
    }

}
