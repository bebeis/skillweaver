package com.bebeis.skillweaver.agent.config

import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Qdrant Vector Store 설정 - RAG 시스템을 위한 벡터 DB 연결
 * 
 * Profile "rag"가 활성화된 경우에만 빈이 등록됩니다.
 * 로컬 개발 시: docker-compose up -d qdrant 실행 필요
 */
@Configuration
@Profile("rag")
class QdrantConfig(
    @Value("\${qdrant.host:localhost}")
    private val host: String,
    
    @Value("\${qdrant.port:6334}")
    private val port: Int,
    
    @Value("\${qdrant.collection-name:skillweaver-knowledge}")
    private val collectionName: String,
    
    @Value("\${qdrant.use-tls:false}")
    private val useTls: Boolean
) {
    @Bean
    fun qdrantClient(): QdrantClient {
        val grpcClientBuilder = QdrantGrpcClient.newBuilder(host, port, useTls)
        return QdrantClient(grpcClientBuilder.build())
    }
    
    @Bean
    fun vectorStore(
        qdrantClient: QdrantClient,
        embeddingModel: EmbeddingModel
    ): VectorStore {
        // Spring AI 1.0.3 - QdrantVectorStore.builder() 패턴 사용
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
            .collectionName(collectionName)
            .initializeSchema(true)
            .build()
    }
}
