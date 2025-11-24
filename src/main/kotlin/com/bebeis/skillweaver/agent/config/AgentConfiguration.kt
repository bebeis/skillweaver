package com.bebeis.skillweaver.agent.config

import com.bebeis.skillweaver.agent.NewTechLearningAgent
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import com.bebeis.skillweaver.core.storage.member.MemberSkillRepository
import com.bebeis.skillweaver.core.storage.technology.TechnologyRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AgentConfiguration {
    
    @Bean
    fun newTechLearningAgent(
        memberRepository: MemberRepository,
        technologyRepository: TechnologyRepository,
        memberSkillRepository: MemberSkillRepository
    ): NewTechLearningAgent {
        return NewTechLearningAgent(
            memberRepository = memberRepository,
            technologyRepository = technologyRepository,
            memberSkillRepository = memberSkillRepository
        )
    }
}
