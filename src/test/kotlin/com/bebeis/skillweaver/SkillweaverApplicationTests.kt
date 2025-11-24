package com.bebeis.skillweaver

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
class SkillweaverApplicationTests {

	companion object {
		@JvmStatic
		@DynamicPropertySource
		fun properties(registry: DynamicPropertyRegistry) {
			// Embabel이 필요로 하는 환경 변수 제공
			registry.add("OPENAI_API_KEY") { "test-api-key" }
			registry.add("spring.jpa.open-in-view") { "false" }
		}
	}

	@Test
	fun contextLoads() {
		// Spring Context 로드 테스트
	}

}
