package com.bebeis.skillweaver.agent.react

/**
 * ReAct (Reasoning and Acting) 패턴 데이터 클래스 모음
 * 
 * V3 Agentic RAG에서 사용됩니다.
 */

/**
 * 에이전트의 사고 결과
 * LLM이 다음 행동을 결정할 때 사용
 */
data class ThoughtResult(
    val thought: String,
    val action: ActionType,
    val actionInput: String
)

/**
 * 가능한 행동 타입
 */
enum class ActionType {
    RAG_SEARCH,     // 내부 지식 베이스 검색
    WEB_SEARCH,     // 웹 검색 (최신 정보)
    GRAPH_QUERY,    // 그래프 DB 쿼리 (관계 탐색)
    FINAL_ANSWER    // 최종 답변 생성
}

/**
 * 행동 수행 후 관찰 결과
 */
data class Observation(
    val source: ActionType,
    val content: String,
    val isRelevant: Boolean = true
) {
    companion object {
        fun fromRagSearch(content: String) = Observation(ActionType.RAG_SEARCH, content)
        fun fromWebSearch(content: String) = Observation(ActionType.WEB_SEARCH, content)
        fun fromGraphQuery(content: String) = Observation(ActionType.GRAPH_QUERY, content)
        val FINAL = Observation(ActionType.FINAL_ANSWER, "", true)
    }
}

/**
 * ReAct 루프의 전체 상태
 */
data class ReActState(
    val originalQuery: String,
    val thoughts: MutableList<ThoughtResult> = mutableListOf(),
    val observations: MutableList<Observation> = mutableListOf(),
    val iteration: Int = 0,
    val maxIterations: Int = 5
) {
    fun addThought(thought: ThoughtResult) {
        thoughts.add(thought)
    }
    
    fun addObservation(observation: Observation) {
        observations.add(observation)
    }
    
    fun canContinue(): Boolean = iteration < maxIterations
    
    fun getContext(): String {
        return observations.joinToString("\n\n---\n\n") { obs ->
            "[${obs.source}]\n${obs.content}"
        }
    }
}

/**
 * 최종 답변
 */
data class AgenticAnswer(
    val answer: String,
    val reasoning: String,
    val sources: List<String>
)
