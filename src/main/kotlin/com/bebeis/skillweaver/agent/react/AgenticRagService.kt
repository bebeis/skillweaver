package com.bebeis.skillweaver.agent.react

import com.bebeis.skillweaver.agent.tools.KnowledgeSearchTool
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.core.CoreToolGroups
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service

/**
 * V3 Agentic RAG Service
 * 
 * ReAct (Reasoning and Acting) 패턴을 구현하여
 * 복잡한 질문에 대해 반복적인 추론과 행동을 수행합니다.
 */
@Service
@ConditionalOnBean(KnowledgeSearchTool::class)
class AgenticRagService(
    private val knowledgeSearchTool: KnowledgeSearchTool
) {
    private val log = LoggerFactory.getLogger(AgenticRagService::class.java)

    companion object {
        private const val MAX_ITERATIONS = 5
        private const val THOUGHT_PROMPT = """
You are an AI that uses the ReAct (Reasoning and Acting) pattern.
Given a user question and previous observations, decide what to do next.

Available actions:
- RAG_SEARCH: Search internal knowledge base for tech learning information
- WEB_SEARCH: Search the web for latest information
- FINAL_ANSWER: Generate final answer when you have enough information

Respond in JSON format:
{
  "thought": "Your reasoning about what information you need",
  "action": "RAG_SEARCH" | "WEB_SEARCH" | "FINAL_ANSWER",
  "actionInput": "query or topic to search (empty if FINAL_ANSWER)"
}

Question: %s

Previous observations:
%s

Think step by step. What should you do next?
"""

        private const val FINAL_ANSWER_PROMPT = """
Based on the following observations, provide a comprehensive answer to the user's question.

Question: %s

Observations gathered:
%s

Provide your answer with:
1. A clear, direct answer
2. Step-by-step reasoning showing how you arrived at the answer
3. Sources referenced

Respond in JSON format:
{
  "answer": "Your comprehensive answer",
  "reasoning": "Step-by-step explanation",
  "sources": ["source1", "source2"]
}
"""
    }

    /**
     * ReAct 루프를 실행하여 질문에 대한 답변을 생성합니다.
     */
    fun executeReActLoop(
        query: String,
        context: OperationContext
    ): AgenticAnswer {
        val state = ReActState(originalQuery = query)
        
        log.info("Starting ReAct loop for query: $query")
        
        while (state.canContinue()) {
            // 1. Reason: 다음 행동 결정
            val thought = reason(query, state, context)
            state.addThought(thought)
            log.debug("Iteration ${state.iteration}: $thought")
            
            // 2. Check if final answer is ready
            if (thought.action == ActionType.FINAL_ANSWER) {
                break
            }
            
            // 3. Act: 행동 수행
            val observation = act(thought, context)
            state.addObservation(observation)
            
            // 4. Update iteration
            (state as ReActState).copy(iteration = state.iteration + 1)
        }
        
        // Generate final answer
        return generateFinalAnswer(query, state, context)
    }

    private fun reason(
        query: String,
        state: ReActState,
        context: OperationContext
    ): ThoughtResult {
        val previousObservations = if (state.observations.isEmpty()) {
            "None yet"
        } else {
            state.getContext()
        }
        
        val prompt = THOUGHT_PROMPT.format(query, previousObservations)
        
        return context.ai()
            .withDefaultLlm()
            .createObject(prompt, ThoughtResult::class.java)
    }

    private fun act(
        thought: ThoughtResult,
        context: OperationContext
    ): Observation {
        return when (thought.action) {
            ActionType.RAG_SEARCH -> {
                val results = knowledgeSearchTool.searchKnowledge(thought.actionInput)
                val content = results.joinToString("\n\n") { 
                    "[${it.technology ?: "general"}] ${it.content}"
                }
                Observation.fromRagSearch(content.ifEmpty { "No relevant documents found" })
            }
            ActionType.WEB_SEARCH -> {
                // Web search는 Embabel의 CoreToolGroups.WEB을 사용
                // 여기서는 단순히 마커를 반환하고, 실제 웹 검색은 context.ai()에서 수행
                Observation.fromWebSearch("Web search for: ${thought.actionInput}")
            }
            ActionType.GRAPH_QUERY -> {
                // GraphRAG 구현 후 활성화
                Observation.fromGraphQuery("Graph query not yet implemented: ${thought.actionInput}")
            }
            ActionType.FINAL_ANSWER -> Observation.FINAL
        }
    }

    private fun generateFinalAnswer(
        query: String,
        state: ReActState,
        context: OperationContext
    ): AgenticAnswer {
        val prompt = FINAL_ANSWER_PROMPT.format(query, state.getContext())
        
        return try {
            context.ai()
                .withDefaultLlm()
                .createObject(prompt, AgenticAnswer::class.java)
        } catch (e: Exception) {
            log.error("Failed to generate final answer", e)
            AgenticAnswer(
                answer = "I apologize, but I couldn't generate a complete answer.",
                reasoning = "Error during answer generation: ${e.message}",
                sources = state.observations.map { it.source.name }
            )
        }
    }
}
