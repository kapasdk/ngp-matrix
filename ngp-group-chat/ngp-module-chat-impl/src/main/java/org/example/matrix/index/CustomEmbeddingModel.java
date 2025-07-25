package org.example.matrix.index;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.map.MapUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class CustomEmbeddingModel implements EmbeddingModel {

    @Resource
    private RestTemplate restTemplate;

    @NonNull
    @Override
    public EmbeddingResponse call(@NonNull EmbeddingRequest request) {
        List<Embedding> embeddings = CollUtil.newArrayList();

        CollUtil.forEach(request.getInstructions(), (input, index) -> {
            Map<String, Object> requestBody = Map.of("text", input);

            Map<?, ?> response = restTemplate.postForObject("http://192.168.159.24:8080/vectors", requestBody, Map.class);
            List<Number> vectors = MapUtil.get(response, "vector", new TypeReference<>() {
            });
            int dim = MapUtil.getInt(response, "dim");

            float[] floatArray = new float[dim];
            CollUtil.forEach(vectors, (val, i) -> floatArray[i] = val.floatValue());

            embeddings.add(new Embedding(floatArray, index));
        });
        return new EmbeddingResponse(embeddings);
    }

    @NonNull
    @Override
    public float[] embed(@NonNull Document document) {
        String text = document.getText();

        EmbeddingOptions embeddingOptions = EmbeddingOptionsBuilder.builder()
                .withModel("sentence-transformers/multi-qa-MiniLM-L6-cos-v1")
                .withDimensions(384)
                .build();

        EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of(text), embeddingOptions);
        EmbeddingResponse embeddingResponse = this.call(embeddingRequest);

        Embedding result = embeddingResponse.getResult();
        return result.getOutput();
    }

}
