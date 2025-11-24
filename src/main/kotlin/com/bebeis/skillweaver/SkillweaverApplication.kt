package com.bebeis.skillweaver

import com.embabel.agent.config.annotation.EnableAgents
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@EnableAgents
@SpringBootApplication
class SkillweaverApplication

fun main(args: Array<String>) {
	runApplication<SkillweaverApplication>(*args)
}
