package org.example.matrix.chat;

import cn.hutool.core.util.IdUtil;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.misc.model.Meta;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.matrix.fc.PersistenceHelper;
import org.example.matrix.index.IndexHelper;
import org.example.matrix.index.WeaviateVectorStoreFactory;
import org.example.matrix.services.StandardService;
import org.example.matrix.test.KMTest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class StandardChatService extends StandardService implements ChatService {

    @Resource
    private ChatClient chatClient;
    @Resource
    private WeaviateClient weaviateClient;

    @Override
    public void test(String input) {
        Flux<AssistantMessage> messages = chatClient
                .prompt(new Prompt(input))
                .options(DeepSeekChatOptions.builder().build())
                .stream().chatResponse()
                .map(chatResponse -> chatResponse.getResult().getOutput());
        messages.subscribe(
                message -> System.out.print(message.getText()),
                error -> System.err.println("发生错误: " + error),
                () -> System.out.println("\n\n所有消息处理完成\n\n")
        );
    }

    @Override
    public void test2(String input) {
        VectorStore vectorStore = WeaviateVectorStoreFactory.getVectorStore(this.weaviateClient, KMTest.class);
        WeaviateClient weaviateClient = (WeaviateClient) vectorStore.getNativeClient().orElseThrow();

        log.info("liveChecker: {}", weaviateClient.misc().liveChecker().run().getResult());
        log.info("readyChecker: {}", weaviateClient.misc().readyChecker().run().getResult());
    }

    @Override
    public void test3(String input) {
        List<KMTest> entities = PersistenceHelper.service.find(KMTest.class);
        IndexHelper.service.index(entities);
    }

    @Override
    public void test4(String input) {
        VectorStore vectorStore = WeaviateVectorStoreFactory.getVectorStore(this.weaviateClient, KMTest.class);

        Filter.Expression expression = new Filter.Expression(Filter.ExpressionType.AND,
                new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key("name"), new Filter.Value("我是来测试的")),
                new Filter.Expression(Filter.ExpressionType.IN, new Filter.Key("number"), new Filter.Value(List.of("TEST-S3", "TEST-3")))
        );
        SearchRequest searchRequest = SearchRequest.builder().query(input).filterExpression(expression).topK(5).build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        log.info("documents: {}", documents);
    }

    public static void main(String[] args) {
        Stream.iterate(0, n -> n + 1).limit(20).forEach(i -> log.info(IdUtil.fastUUID()));

        Config config = new Config("http", "192.168.159.24:8087");
        WeaviateClient client = new WeaviateClient(config);
        Result<Meta> meta = client.misc().metaGetter().run();
        if (meta.hasErrors()) {
            System.out.printf("Error: %s\n", meta.getError().getMessages());
        } else {
            System.out.printf("meta.hostname: %s\n", meta.getResult().getHostname());
            System.out.printf("meta.version: %s\n", meta.getResult().getVersion());
            System.out.printf("meta.modules: %s\n", meta.getResult().getModules());
        }
    }

}
