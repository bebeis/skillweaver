# Skillweaver

## 간단 소개
- **설명**: Skillweaver는 Embabel 에이전트를 활용한 학습/에이전트 백엔드 서비스 프로젝트입니다. JVM(SPRING BOOT/Kotlin) 기반이며, 에이전트 구조와 API, 배포 파이프라인 템플릿을 포함합니다.
- **목표**: Embabel 에이전트 프레임워크를 활용한 에이전트 설계 템플릿 제공, 학습자 맞춤 기능 제공

## 목차
- **프로젝트 개요**
- **프로젝트 구조**
- **Embabel Agent 설계**
- **개발 방법**
- **배포 방법**
- **운영·모니터링**
- **참고 문서**

## 프로젝트 개요
- 핵심 도메인: 사용자(학습자) 프로필, 기술(Technologies), 에이전트(Agent) 관련 API
- API 버전: `/api/v1` 네임스페이스를 따름
- 응답 포맷: `ApiResponse<T>` 표준을 사용
- 구현 언어·플랫폼: Kotlin + Spring Boot (Gradle Kotlin DSL)

## 프로젝트 구조

### 주요 경로
- `src/main/kotlin/com/bebeis/skillweaver` : 애플리케이션 소스
- `src/main/resources/application.yml` : 환경 구성
- `build.gradle.kts`, `gradlew` : 빌드 스크립트
- `.github/workflows/cd.yml` : CD/CI 파이프라인 설정 (배포 워크플로우)

### 패키지 구조 (주요 패키지 설명)
- `com.bebeis.skillweaver` : 애플리케이션 진입점(`SkillweaverApplication.kt`)과 공용 설정/유틸
- `com.bebeis.skillweaver.api` : REST 컨트롤러와 외부와의 통신을 담당하는 계층(하위에 `agent`, `auth`, `technologies` 등 컨트롤러 패키지 존재)
- `com.bebeis.skillweaver.core` : 핵심 도메인 모델, 서비스, 리포지토리, 비즈니스 로직을 포함
- `com.bebeis.skillweaver.agent` : Embabel/Agent 관련 실행기, 어댑터, 툴 연동 코드(AgentRunner, ToolAdapter 등)
- `templates` 및 `resources` : 서버 사이드 템플릿과 정적 리소스

**설명**: 각 패키지는 책임이 분리되도록 설계되어 있으며, 컨트롤러(`api`) → 서비스(`core`) → 저장소(Repository)의 흐름을 따릅니다. Agent 관련 코드는 `agent` 패키지에 모아 에이전트 라이프사이클과 도구 연동을 캡슐화합니다.

## Embabel Agent 설계

### NewTechLearningAgent 설계 상세 (적응형 학습 계획 생성)

`NewTechLearningAgent`는 사용자의 경험 수준과 가용 시간에 따라 **동적으로 깊이를 조절(Adaptive Depth)**하는 에이전트입니다. 단일 파이프라인이 아닌, LLM의 판단에 따라 실행 경로가 달라지는 구조를 가집니다.

#### 1. 핵심 메커니즘: Adaptive Planning Strategy

* **프로필 분석 (`extractMemberProfile`)**: 사용자의 경험(Level), 학습 스타일, 가용 시간을 먼저 파악합니다.
* **깊이 결정 (`decideDepthPlan`)**: LLM이 사용자 프로필과 기술 난이도를 고려하여 4가지 모드 중 하나를 결정합니다.
    * **Quick**: 숙련자를 위한 속성 코스 (Gap 분석 생략, 3-4단계)
    * **Standard**: 일반적인 학습 경로 (간단한 Gap 체크, 5-7단계)
    * **Detailed**: 초심자를 위한 상세 가이드 (상세 Gap 분석, 리소스 풍부화, 8-12단계)
    * **Hybrid**: 위 단계들을 혼합 (예: 기초는 Detailed, 심화는 Quick)

#### 2. 실행 흐름 (Workflow)

* `@AchievesGoal` 어노테이션이 붙은 `buildAdaptivePlan`이 메인 진입점 역할을 하며, 결정된 `DepthPlan`에 따라 하위 `@Action`들을 조건부로 호출합니다.

#### 3. 주요 기술적 특징

* **병렬 리소스 수집 (`enrichWithResources`)**: `CompletableFuture`와 커스텀 `resourceExecutor`를 사용하여, 각 커리큘럼 단계(Step)에 필요한 학습 자료(Web, GitHub, YouTube)를 병렬로 검색·수집하여 응답 속도를 최적화했습니다.
* **하이브리드 경로 구성 (`composeHybridPlan`)**: 단일 난이도가 아닌, "기초(Detailed) -> 응용(Standard) -> 심화(Quick)"와 같이 구간별로 밀도를 다르게 설정하는 고도화된 로직을 포함합니다.
* **Tool Integration**: `CoreToolGroups.WEB` 등을 활용해 최신 기술 트렌드와 문서를 실시간으로 조사합니다.

#### 4. 주요 Action 설명

* `decideDepthPlan`: 사용자 메타데이터를 기반으로 최적의 분석/커리큘럼/리소스 깊이를 JSON 형태로 결정
* `quickGapCheck` vs `detailedGapAnalysis`: 사용자의 선수 지식 상태를 진단하는 심도를 다르게 적용
* `enrichWithResources`: 검색 도구를 사용하여 커리큘럼의 각 단계에 맞는 최적의 학습 리소스 매핑

### 코딩·아키텍처 컨벤션(요약)
- DTO 사용 규칙: Controller ↔ Service 간 DTO 사용 (`XxxServiceRequest`, `XxxServiceResponse`)
- Lombok 스타일 금지: DTO 제외 모든 클래스에 `@Data`/`@Setter` 사용 금지
- JPA: 연관관계 매핑 지양, 물리적 FK 제거 권장(논리 FK 사용)
- 예외 메시지와 코드: enum으로 관리
- 도메인 모델 패턴과 VO/일급 컬렉션 사용 지향

### 구성요소
- **Agent Controller**: 외부 요청(REST / WebSocket 등)을 받아 에이전트 실행 트리거 (`src/main/kotlin/com/bebeis/skillweaver/api/agent` 경로 예상)
- **Agent Service / Runner**: 에이전트의 핵심 로직(플랜 생성, 도구 호출, 상태 관리)을 수행
- **Tool Adapter(s)**: Embabel가 지원하는 툴 그룹(예: 검색, 외부 API 호출 등)을 연결하는 어댑터
- **Stream/Events**: 에이전트가 장기 실행 또는 대화형 작업을 수행할 때 스트리밍 응답을 제공

### 동작 플로우 (단계별)
1. 요청 수신 (REST 또는 Agent Stream client)
2. 요청을 Service 레이어의 AgentRunner로 전달
3. AgentRunner가 Embabel 에이전트 라이프사이클을 관리 (초기화 → 도구 선택 → 액션 수행 → 종료)
4. 결과를 DTO로 변환하여 응답

### 설계 고려사항
- 에이전트의 상태와 결과는 가능하면 불변(immutable) 형태로 전달
- 장기 실행 작업은 SSE로 처리

## 개발 가이드

### 요구사항
- JDK 21 이상 (Embabel 제약 사항)
- Gradle Wrapper 사용 (`./gradlew`)

### 빌드
```bash
./gradlew clean build -x test
```

### 실행 (개발)
```bash
./gradlew bootRun
# 또는
java -jar build/libs/skillweaver-<version>.jar
```

### 테스트
```bash
./gradlew test
```

### 로컬 API 예시
회원가입 예시:
```bash
curl -X POST http://localhost:8080/api/v1/auth/signup/email \
  -H "Content-Type: application/json" \
  -d '{"name":"테스트사용자","email":"test@example.com","password":"password123!","targetTrack":"BACKEND","experienceLevel":"INTERMEDIATE","learningPreference":{"learningStyle":"PROJECT_BASED","dailyMinutes":120,"preferKorean":true,"weekendBoost":true}}'
```

## 배포 가이드

### 도커 이미지 생성
```bash
docker build -t bebeis/skillweaver:latest .
```

### 컨테이너 실행
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SOME_SECRET=값 \
  bebeis/skillweaver:latest
```

### CI/CD
- `.github/workflows/cd.yml`에 자동 배포 워크플로우가 포함되어 있습니다. 워크플로우는 빌드 후 Docker 이미지 빌드/푸시 또는 클라우드에 배포하는 단계를 포함할 수 있습니다. (자세한 파이프라인 설정은 해당 파일 참조)

## 개발 규칙(중요)
- API 설계: `/api/v1` 네임스페이스 준수, `ApiResponse<T>` 반환
- Controller → Service → Repository 계층 분리 및 DTO 사용
- 예외 처리: Controller Advice로 일괄 처리
- 데이터베이스: 물리적 FK 사용 지양, 논리 FK로 관리