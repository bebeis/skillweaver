package com.bebeis.skillweaver.core.domain.agent

enum class SseEventType {
    AGENT_STARTED,      
    PLANNING_STARTED,   
    ACTION_STARTED,     
    ACTION_EXECUTED,    
    PROGRESS,           
    PATH_UPDATED,       
    AGENT_COMPLETED,    
    ERROR               
}
