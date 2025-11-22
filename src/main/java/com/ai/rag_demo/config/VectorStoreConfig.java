package com.ai.rag_demo.config;

import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:rag-documents}")
    private String indexName;

    @Value("${spring.ai.vectorstore.elasticsearch.initialize-schema:true}")
    private boolean initializeSchema;

    @Value("${spring.ai.vectorstore.elasticsearch.dimensions:4096}")
    private int dimensions;

    /**
     * Batching strategy for embedding generation
     */
    @Bean
    @ConditionalOnMissingBean
    public BatchingStrategy batchingStrategy() {
        return new TokenCountBatchingStrategy();
    }

    /**
     * Configure Elasticsearch Vector Store for RAG with proper dimensions
     */
    @Bean
    public VectorStore vectorStore(RestClient restClient,
                                   @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
                                   BatchingStrategy batchingStrategy) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(indexName);
        options.setDimensions(dimensions);

        return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .options(options)
                .initializeSchema(initializeSchema)
                .batchingStrategy(batchingStrategy)
                .build();
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
