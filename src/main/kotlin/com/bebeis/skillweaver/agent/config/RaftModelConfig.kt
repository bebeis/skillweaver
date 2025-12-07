package com.bebeis.skillweaver.agent.config

import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * V3 RAFT (Retrieval Augmented Fine-Tuning) 설정
 *
 * RAFT가 활성화되면 파인튜닝된 모델을 사용합니다.
 * 파인튜닝된 모델은 도메인 특화 지식을 학습하여
 * 환각(Hallucination)을 줄이고 정확도를 높입니다.
 */
@Configuration
@ConditionalOnProperty(name = ["skillweaver.raft.enabled"], havingValue = "true")
class RaftModelConfig(
    private val openAiApi: OpenAiApi
) {

    @Value("\${skillweaver.raft.model-id}")
    private lateinit var raftModelId: String

    /**
     * RAFT 파인튜닝된 ChatModel Bean
     * skillweaver.raft.enabled=true 일 때 Primary로 등록됩니다.
     */
    @Bean
    @Primary
    fun raftChatModel(): ChatModel {
        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model(raftModelId)
                    .temperature(0.3) // 정확도 우선
                    .build()
            )
            .build()
    }
}
