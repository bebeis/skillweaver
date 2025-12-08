package com.bebeis.skillweaver.agent.config

import com.bebeis.skillweaver.agent.tools.KnowledgeSearchTool
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * RAG 도구 그룹 설정
 *
 * VectorStore 빈이 존재할 때만 KnowledgeSearchTool이 활성화됩니다.
 * Agent에서 사용 시: context.ai().withToolObject(knowledgeSearchTool).create(...)
 */
@Configuration
@Profile("rag")
class RagToolGroupConfig(
    private val knowledgeSearchTool: KnowledgeSearchTool
) {

    companion object {
        const val KNOWLEDGE = "knowledge"
    }

    // KnowledgeSearchTool은 @Component로 자동 등록됨
    // Agent에서 사용법:
    // context.ai()
    //     .withDefaultLlm()
    //     .withToolObject(knowledgeSearchTool)
    //     .create<T>(prompt)
}
