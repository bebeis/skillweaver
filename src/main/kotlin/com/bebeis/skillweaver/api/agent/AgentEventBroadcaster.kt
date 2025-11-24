package com.bebeis.skillweaver.api.agent

import com.bebeis.skillweaver.api.agent.dto.AgentEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

@Component
class AgentEventBroadcaster {
    private val logger = LoggerFactory.getLogger(AgentEventBroadcaster::class.java)
    private val emitters: MutableMap<Long, MutableSet<SseEmitter>> = ConcurrentHashMap()
    private val defaultTimeoutMillis = 30 * 60 * 1000L

    fun subscribe(agentRunId: Long): SseEmitter {
        val emitter = SseEmitter(defaultTimeoutMillis)
        val runEmitters = emitters.computeIfAbsent(agentRunId) { CopyOnWriteArraySet() }
        runEmitters.add(emitter)

        emitter.onCompletion { removeEmitter(agentRunId, emitter) }
        emitter.onTimeout {
            emitter.complete()
            removeEmitter(agentRunId, emitter)
        }
        emitter.onError { removeEmitter(agentRunId, emitter) }

        logger.debug("Registered SSE subscriber for agentRunId={}", agentRunId)
        return emitter
    }

    fun broadcast(agentRunId: Long, eventName: String, event: AgentEventDto) {
        val targetEmitters = emitters[agentRunId] ?: return
        logger.debug(
            "Broadcasting SSE event='{}' seq={} to {} emitters for agentRunId={}",
            eventName,
            event.sequence,
            targetEmitters.size,
            agentRunId
        )
        val iterator = targetEmitters.iterator()
        while (iterator.hasNext()) {
            val emitter = iterator.next()
            try {
                emitter.send(SseEmitter.event().name(eventName).data(event))
            } catch (ex: Exception) {
                logger.warn("Failed to send SSE event '{}' for agentRunId={}", eventName, agentRunId, ex)
                emitter.completeWithError(ex)
                iterator.remove()
            }
        }

        if (targetEmitters.isEmpty()) {
            emitters.remove(agentRunId)
        }
    }

    fun complete(agentRunId: Long) {
        emitters.remove(agentRunId)?.forEach { emitter ->
            try {
                emitter.complete()
            } catch (_: Exception) {
                // ignore - emitter already completed or closed
            }
        }
        logger.debug("Completed all SSE emitters for agentRunId={}", agentRunId)
    }

    private fun removeEmitter(agentRunId: Long, emitter: SseEmitter) {
        val targetEmitters = emitters[agentRunId] ?: return
        targetEmitters.remove(emitter)
        if (targetEmitters.isEmpty()) {
            emitters.remove(agentRunId)
        }
    }
}
