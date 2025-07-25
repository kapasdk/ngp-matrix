package org.example.matrix.index;

import cn.hutool.core.map.MapUtil;
import cn.hutool.extra.spring.SpringUtil;
import io.weaviate.client.WeaviateClient;
import lombok.experimental.UtilityClass;
import org.example.matrix.fc.Persistable2;
import org.example.matrix.fc.PersistenceHelper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.weaviate.WeaviateVectorStore;

import java.util.List;
import java.util.Map;

@UtilityClass
public class WeaviateVectorStoreFactory {
    private final Map<String, VectorStore> VECTOR_STORE_CACHE = MapUtil.newConcurrentHashMap();

    public <T extends Persistable2> VectorStore getVectorStore(WeaviateClient weaviateClient, Class<T> clazz) {
        String cacheKey = PersistenceHelper.getEntityName(clazz);

        return VECTOR_STORE_CACHE.computeIfAbsent(cacheKey, key -> {
            IndexHelper.service.build(clazz);

            EmbeddingModel embeddingModel = SpringUtil.getBean(EmbeddingModel.class);

            List<WeaviateVectorStore.MetadataField> filterMetadataFields = List.of(
                    WeaviateVectorStore.MetadataField.text("number"),
                    WeaviateVectorStore.MetadataField.text("name"),
                    WeaviateVectorStore.MetadataField.text("description")
            );
            return WeaviateVectorStore.builder(weaviateClient, embeddingModel)
                    .objectClass(PersistenceHelper.getEntityName(clazz))
                    .filterMetadataFields(filterMetadataFields)
                    .build();
        });
    }

}