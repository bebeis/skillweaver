# RAG 초기 지식 베이스 시드 가이드 (v8 AI Professor Edition)

## 개요
SkillWeaver RAG 시스템을 위한 검증된 초기 학습 데이터입니다. 백엔드, 프론트엔드, AI, 모바일, CS, DB가 포함되어 있으며, 특히 **AI 분야가 대학원 커리큘럼 수준**으로 고도화되었습니다.

---

## 시드 데이터 구성 (총 42개 파일)

### 🆕 AI / ML Academic (13개 분할)
AI 교수님 평가를 대비하여 이론, 수학, 최신 논문 트렌드를 반영했습니다.

**1. Foundations (수학 & 이론)**
- `ai-math.json`: 선형대수(Eigenvalue, SVD), 미적분(Hessian), 통계(MLE, Bayes).
- `ml-theory.json`: Bias-Variance Tradeoff, Optimization(Adam vs SGD), Regularization.
- `python-data.json`: NumPy Vectorization.

**2. Deep Learning Domains**
- `dl-vision.json`: ResNet(Skip Connection), **ViT(Vision Transformer)**, YOLO.
- `dl-nlp.json`: Word2Vec -> RNN -> Transformer -> BERT/GPT 계보.
- `ai-generative-art.json`: VAE -> GAN(Minimax) -> **Diffusion(DDPM)**.
- `ai-rl.json`: MDP, Q-Learning, **PPO(Actor-Critic)**.

**3. LLM & GenAI Core**
- `llm-architecture.json`: **Attention 수식**, RoPE, FlashAttention, SwiGLU.
- `llm-training.json`: Pre-training -> SFT -> **RLHF vs DPO**, LoRA/QLoRA.
- `langchain.json`: RAG Pipeline.
- `langgraph.json`: **Agentic Workflow** (Cyclic Graph).

**4. Engineering**
- `mlops.json`: Drift(Data/Concept), Feature Store.
- `tensorflow.json` / `pytorch.json` / `scikit-learn.json`: 프레임워크 활용.

### Other Sections
- **CS/DB (12)**: Network, OS, Algo, DS, MySQL, Redis 등 상세화.
- **Frontend (8)**: React, Next.js, etc.
- **Mobile (2)**: iOS, Android.
- **Spring (3)**: Framework, Data, Boot.
- **Lang (2)**: Java, Kotlin.
- **Infra (1)**: Docker.

---

## AI 데이터 학술적 특징

1. **Depth of Architecture**
   - 단순 사용법이 아닌 **SwiGLU, RoPE, FlashAttention** 같은 최신 아키텍처 디테일 포함.
2. **Mathematical Rigor**
   - Attention 메커니즘의 수식적 원리, 최적화 알고리즘(Hessian, Gradient) 언급.
3. **Latest Research**
   - 2023-2024 트렌드인 **DPO(Direct Preference Optimization)**, **Latent Diffusion** 포함.

---

## 사용 방법

**자동 임베딩 (개발 환경)**
```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```
*파일 개수: 42개. 예상 소요 시간: 1~2분.*

**수동 임베딩**
```bash
./gradlew bootRun --args='--seed-knowledge'
```
