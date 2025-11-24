package com.bebeis.skillweaver.core.config

import com.bebeis.skillweaver.core.domain.member.ExperienceLevel
import com.bebeis.skillweaver.core.domain.member.LearningPreference
import com.bebeis.skillweaver.core.domain.member.LearningStyle
import com.bebeis.skillweaver.core.domain.member.Member
import com.bebeis.skillweaver.core.domain.member.TargetTrack
import com.bebeis.skillweaver.core.domain.technology.KnowledgeSource
import com.bebeis.skillweaver.core.domain.technology.RelationType
import com.bebeis.skillweaver.core.domain.technology.TechRelationship
import com.bebeis.skillweaver.core.domain.technology.Technology
import com.bebeis.skillweaver.core.domain.technology.TechnologyCategory
import com.bebeis.skillweaver.core.domain.technology.TechnologyKnowledge
import com.bebeis.skillweaver.core.storage.member.MemberRepository
import com.bebeis.skillweaver.core.storage.technology.TechRelationshipRepository
import com.bebeis.skillweaver.core.storage.technology.TechnologyKnowledgeRepository
import com.bebeis.skillweaver.core.storage.technology.TechnologyRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("local")
class TestDataInit(
    private val technologyRepository: TechnologyRepository,
    private val technologyKnowledgeRepository: TechnologyKnowledgeRepository,
    private val techRelationshipRepository: TechRelationshipRepository,
    private val memberRepository: MemberRepository
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(TestDataInit::class.java)

    @Transactional
    override fun run(@Suppress("UnusedParameter") args: ApplicationArguments) {
        val technologiesByKey = seedTechnologies()
        seedTechnologyKnowledge(technologiesByKey)
        seedTechnologyRelationships(technologiesByKey)
        seedTestMembers()
    }

    private fun seedTechnologies(): Map<String, Technology> {
        val seedTechnologies = listOf(
            Technology(
                key = "kotlin",
                displayName = "Kotlin",
                category = TechnologyCategory.LANGUAGE,
                ecosystem = "JVM",
                officialSite = "https://kotlinlang.org"
            ),
            Technology(
                key = "java",
                displayName = "Java",
                category = TechnologyCategory.LANGUAGE,
                ecosystem = "JVM",
                officialSite = "https://www.oracle.com/java/"
            ),
            Technology(
                key = "spring-boot",
                displayName = "Spring Boot",
                category = TechnologyCategory.FRAMEWORK,
                ecosystem = "Spring",
                officialSite = "https://spring.io/projects/spring-boot"
            ),
            Technology(
                key = "react",
                displayName = "React",
                category = TechnologyCategory.LIBRARY,
                ecosystem = "JavaScript",
                officialSite = "https://react.dev"
            ),
            Technology(
                key = "nextjs",
                displayName = "Next.js",
                category = TechnologyCategory.FRAMEWORK,
                ecosystem = "React",
                officialSite = "https://nextjs.org"
            ),
            Technology(
                key = "typescript",
                displayName = "TypeScript",
                category = TechnologyCategory.LANGUAGE,
                ecosystem = "JavaScript",
                officialSite = "https://www.typescriptlang.org"
            ),
            Technology(
                key = "mysql",
                displayName = "MySQL",
                category = TechnologyCategory.DATABASE,
                ecosystem = "SQL",
                officialSite = "https://www.mysql.com"
            ),
            Technology(
                key = "postgresql",
                displayName = "PostgreSQL",
                category = TechnologyCategory.DATABASE,
                ecosystem = "SQL",
                officialSite = "https://www.postgresql.org"
            ),
            Technology(
                key = "docker",
                displayName = "Docker",
                category = TechnologyCategory.DEVOPS,
                ecosystem = "DevOps",
                officialSite = "https://www.docker.com"
            ),
            Technology(
                key = "aws",
                displayName = "AWS",
                category = TechnologyCategory.PLATFORM,
                ecosystem = "Cloud",
                officialSite = "https://aws.amazon.com"
            )
        )

        val existingByKey = technologyRepository.findByKeyIn(seedTechnologies.map { it.key })
            .associateBy { it.key }
        val technologiesToInsert = seedTechnologies.filterNot { existingByKey.containsKey(it.key) }

        if (technologiesToInsert.isEmpty()) {
            logger.info("TestDataInit: technology seed already present. Skipping initialization.")
        }

        val savedTechnologies = if (technologiesToInsert.isNotEmpty()) {
            technologyRepository.saveAll(technologiesToInsert).also {
                logger.info(
                    "TestDataInit: added {} technology records for local profile: {}",
                    it.size,
                    it.map { technology -> technology.key }
                )
            }
        } else {
            emptyList()
        }

        return existingByKey + savedTechnologies.associateBy { it.key }
    }

    private fun seedTechnologyKnowledge(technologiesByKey: Map<String, Technology>) {
        val knowledgeSeeds = listOf(
            TechnologyKnowledgeSeed(
                key = "kotlin",
                summary = """
                    JVM 기반의 모던 언어로 null-safety와 간결한 문법을 제공하며, 코루틴을 통해 비동기 코드를 단순하게 작성할 수 있습니다.
                """.trimIndent(),
                learningTips = """
                    표준 라이브러리와 코루틴 빌더를 함께 연습하고, 자바 코드와의 상호 운용성을 경험해 보세요. Gradle Kotlin DSL 설정을 직접 다뤄보면 프로젝트 전반에 익숙해집니다.
                """.trimIndent()
            ),
            TechnologyKnowledgeSeed(
                key = "java",
                summary = """
                    엔터프라이즈 개발에서 널리 쓰이는 범용 언어로, 풍부한 생태계와 JVM 생태계를 기반으로 합니다.
                """.trimIndent(),
                learningTips = """
                    최신 LTS 버전의 기능(Stream, Record, Switch 패턴)을 중심으로 학습하고, JVM 메모리 모델과 GC 옵션을 함께 살펴보면 운영 환경 이해도가 높아집니다.
                """.trimIndent()
            ),
            TechnologyKnowledgeSeed(
                key = "spring-boot",
                summary = """
                    Spring 기반 애플리케이션을 빠르게 구축하도록 돕는 프레임워크로, 자동 설정과 스타터 의존성을 통해 프로덕션 준비를 단순화합니다.
                """.trimIndent(),
                learningTips = """
                    작은 REST API를 만들며 Actuator, Profile, ConfigurationProperties를 직접 사용해보세요. 컨테이너 환경에서의 설정 분리와 헬스체크 구성을 함께 연습하면 좋습니다.
                """.trimIndent()
            ),
            TechnologyKnowledgeSeed(
                key = "react",
                summary = """
                    선언적 UI를 만드는 컴포넌트 기반 라이브러리로, 훅(Hooks)을 통해 상태와 라이프사이클을 단순하게 다룹니다.
                """.trimIndent(),
                learningTips = """
                    상태 관리 접근(Redux, Zustand 등)을 비교하면서, React DevTools로 렌더링 원인을 추적해 보세요. 컴포넌트 단위 테스트와 스토리북 작성도 함께 익히면 실무 대비가 됩니다.
                """.trimIndent()
            ),
            TechnologyKnowledgeSeed(
                key = "nextjs",
                summary = """
                    React 기반의 풀스택 프레임워크로, 파일 기반 라우팅과 다양한 렌더링 전략(SSR, SSG, ISR)을 제공합니다.
                """.trimIndent(),
                learningTips = """
                    페이지/라우트 핸들러를 직접 만들어보고, 데이터 패칭(서버 액션, fetch 캐시 옵션)의 차이를 실험해 보세요. 배포 타깃(Vercel, Docker)별 설정을 비교하면 이해가 빨라집니다.
                """.trimIndent()
            ),
            TechnologyKnowledgeSeed(
                key = "typescript",
                summary = """
                    자바스크립트에 정적 타입을 더한 언어로, 규모 있는 프론트엔드 코드베이스의 안정성을 높여줍니다.
                """.trimIndent(),
                learningTips = """
                    좁혀나가기(narrowing) 패턴과 제네릭을 활용한 타입 설계를 연습하세요. tsconfig의 엄격도 옵션을 단계적으로 높이며 마이그레이션하는 경험이 큰 도움이 됩니다.
                """.trimIndent()
            ),
            TechnologyKnowledgeSeed(
                key = "mysql",
                summary = """
                    널리 사용되는 관계형 데이터베이스로, 복제와 파티셔닝을 포함한 다양한 운영 옵션을 제공합니다.
                """.trimIndent(),
                learningTips = """
                    EXPLAIN으로 쿼리 계획을 확인하고 인덱스 설계를 반복해 보세요. 트랜잭션 격리 수준과 락 동작을 실습하면 장애 대응 감각을 익힐 수 있습니다.
                """.trimIndent()
            ),
            TechnologyKnowledgeSeed(
                key = "postgresql",
                summary = """
                    확장성과 표준 SQL 지원이 뛰어난 오픈 소스 데이터베이스로, JSON, Window Function 등 풍부한 기능을 제공합니다.
                """.trimIndent(),
                learningTips = """
                    CTE와 윈도우 함수를 활용한 분석 쿼리를 작성하고, psql을 이용한 튜닝(ANALYZE, EXPLAIN)을 자주 실행해 보세요. 확장(PostGIS, pgvector)도 가볍게 시도해보면 좋습니다.
                """.trimIndent()
            ),
            TechnologyKnowledgeSeed(
                key = "docker",
                summary = """
                    애플리케이션을 컨테이너로 패키징하고 배포하는 플랫폼으로, 일관된 실행 환경을 제공합니다.
                """.trimIndent(),
                learningTips = """
                    Dockerfile 레이어링과 캐시 전략을 고민해보세요. docker compose로 로컬 개발 환경을 자동화하고, 이미지 스캔/멀티스테이지 빌드를 적용해보면 운영 품질이 올라갑니다.
                """.trimIndent()
            ),
            TechnologyKnowledgeSeed(
                key = "aws",
                summary = """
                    다양한 매니지드 서비스를 제공하는 클라우드 플랫폼으로, 네트워킹부터 애플리케이션 배포까지 전반을 지원합니다.
                """.trimIndent(),
                learningTips = """
                    IAM 최소 권한 원칙을 지키며 실습 프로젝트를 올려보세요. CloudWatch로 로깅/모니터링을 구성하고 비용 알림을 설정하면 운영 안정성과 비용 통제가 쉬워집니다.
                """.trimIndent()
            )
        )

        var insertedCount = 0
        knowledgeSeeds.forEach { seed ->
            val technology = technologiesByKey[seed.key]
            val technologyId = technology?.technologyId

            if (technologyId == null) {
                logger.warn("TestDataInit: technology {} not found for knowledge seed. Skipping.", seed.key)
                return@forEach
            }

            if (technologyKnowledgeRepository.findByTechnologyId(technologyId) != null) {
                return@forEach
            }

            technologyKnowledgeRepository.save(
                TechnologyKnowledge(
                    technologyId = technologyId,
                    summary = seed.summary,
                    learningTips = seed.learningTips,
                    sourceType = seed.sourceType
                )
            )
            insertedCount++
        }

        if (insertedCount > 0) {
            logger.info("TestDataInit: added {} technology knowledge records.", insertedCount)
        }
    }

    private fun seedTechnologyRelationships(technologiesByKey: Map<String, Technology>) {
        val relationshipSeeds = listOf(
            TechRelationshipSeed(
                fromKey = "kotlin",
                toKey = "spring-boot",
                relationType = RelationType.NEXT_STEP,
                weight = 4
            ),
            TechRelationshipSeed(
                fromKey = "java",
                toKey = "spring-boot",
                relationType = RelationType.NEXT_STEP,
                weight = 4
            ),
            TechRelationshipSeed(
                fromKey = "spring-boot",
                toKey = "mysql",
                relationType = RelationType.NEXT_STEP,
                weight = 3
            ),
            TechRelationshipSeed(
                fromKey = "spring-boot",
                toKey = "docker",
                relationType = RelationType.NEXT_STEP,
                weight = 3
            ),
            TechRelationshipSeed(
                fromKey = "docker",
                toKey = "aws",
                relationType = RelationType.NEXT_STEP,
                weight = 3
            ),
            TechRelationshipSeed(
                fromKey = "react",
                toKey = "nextjs",
                relationType = RelationType.NEXT_STEP,
                weight = 4
            ),
            TechRelationshipSeed(
                fromKey = "nextjs",
                toKey = "react",
                relationType = RelationType.PREREQUISITE,
                weight = 5
            ),
            TechRelationshipSeed(
                fromKey = "react",
                toKey = "typescript",
                relationType = RelationType.NEXT_STEP,
                weight = 2
            ),
            TechRelationshipSeed(
                fromKey = "mysql",
                toKey = "postgresql",
                relationType = RelationType.ALTERNATIVE,
                weight = 3
            )
        )

        var insertedCount = 0
        relationshipSeeds.forEach { seed ->
            val fromTechnology = technologiesByKey[seed.fromKey]
            val toTechnology = technologiesByKey[seed.toKey]
            val fromId = fromTechnology?.technologyId
            val toId = toTechnology?.technologyId

            if (fromId == null || toId == null) {
                logger.warn(
                    "TestDataInit: relationship seed skipped because technology not found. fromKey={}, toKey={}",
                    seed.fromKey,
                    seed.toKey
                )
                return@forEach
            }

            val exists = techRelationshipRepository.findByFromId(fromId)
                .any { it.toId == toId && it.relationType == seed.relationType }
            if (exists) {
                return@forEach
            }

            techRelationshipRepository.save(
                TechRelationship(
                    fromId = fromId,
                    toId = toId,
                    relationType = seed.relationType,
                    weight = seed.weight
                )
            )
            insertedCount++
        }

        if (insertedCount > 0) {
            logger.info("TestDataInit: added {} technology relationship records.", insertedCount)
        }
    }

    private fun seedTestMembers() {
        val memberSeeds = listOf(
            TestMemberSeed(
                name = "테스트 백엔드",
                email = "backend.tester@skillweaver.local",
                rawPassword = "test1234!",
                targetTrack = TargetTrack.BACKEND,
                experienceLevel = ExperienceLevel.ADVANCED,
                learningPreference = LearningPreference(
                    dailyMinutes = 120,
                    preferKorean = true,
                    learningStyle = LearningStyle.PROJECT_BASED,
                    weekendBoost = true
                )
            ),
            TestMemberSeed(
                name = "테스트 프론트엔드",
                email = "frontend.tester@skillweaver.local",
                rawPassword = "test1234!",
                targetTrack = TargetTrack.FRONTEND,
                experienceLevel = ExperienceLevel.INTERMEDIATE,
                learningPreference = LearningPreference(
                    dailyMinutes = 90,
                    preferKorean = false,
                    learningStyle = LearningStyle.VIDEO_FIRST,
                    weekendBoost = false
                )
            )
        )

        memberSeeds.forEach { seed ->
            if (memberRepository.existsByEmail(seed.email)) {
                logger.info("TestDataInit: member {} already exists. Skipping.", seed.email)
                return@forEach
            }

            val saved = memberRepository.save(
                Member.create(
                    name = seed.name,
                    email = seed.email,
                    rawPassword = seed.rawPassword,
                    targetTrack = seed.targetTrack,
                    experienceLevel = seed.experienceLevel,
                    learningPreference = seed.learningPreference
                )
            )
            logger.info(
                "TestDataInit: created test member id={} email={} (password={})",
                saved.memberId,
                seed.email,
                seed.rawPassword
            )
        }
    }

    private data class TechnologyKnowledgeSeed(
        val key: String,
        val summary: String?,
        val learningTips: String?,
        val sourceType: KnowledgeSource = KnowledgeSource.COMMUNITY
    )

    private data class TechRelationshipSeed(
        val fromKey: String,
        val toKey: String,
        val relationType: RelationType,
        val weight: Int
    )

    private data class TestMemberSeed(
        val name: String,
        val email: String,
        val rawPassword: String,
        val targetTrack: TargetTrack,
        val experienceLevel: ExperienceLevel,
        val learningPreference: LearningPreference
    )
}
