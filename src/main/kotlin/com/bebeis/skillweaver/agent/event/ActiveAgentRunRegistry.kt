package com.bebeis.skillweaver.agent.event

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * 활성 Agent 실행의 ThreadId -> AgentRunId 매핑을 관리
 * Agent 내부에서 진행률 이벤트를 발행할 때 현재 스레드의 agentRunId를 조회
 */
@Component
class ActiveAgentRunRegistry {
    
    private val threadToRunId = ConcurrentHashMap<Long, Long>()
    
    /**
     * Agent 실행 시작 시 현재 스레드의 agentRunId 등록
     */
    fun register(agentRunId: Long) {
        threadToRunId[Thread.currentThread().id] = agentRunId
    }
    
    /**
     * Agent 실행 완료/실패 시 등록 해제
     */
    fun unregister() {
        threadToRunId.remove(Thread.currentThread().id)
    }
    
    /**
     * 현재 스레드의 agentRunId 조회
     */
    fun getCurrentRunId(): Long? {
        return threadToRunId[Thread.currentThread().id]
    }
    
    /**
     * 특정 agentRunId로 등록된 스레드가 있는지 확인
     */
    fun isActive(agentRunId: Long): Boolean {
        return threadToRunId.containsValue(agentRunId)
    }
}
