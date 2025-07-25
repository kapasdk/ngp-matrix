package org.example.matrix.index;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeaviateConfig {

    @Bean
    public WeaviateClient weaviateClient() {
        Config config = new Config("http", "192.168.159.24:8087");
        return new WeaviateClient(config);
    }

}
