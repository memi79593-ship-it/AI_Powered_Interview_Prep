# 🎯 AI-Powered Interview Prep System

> Fully local AI — No API keys, No cloud costs, No rate limits.
>
> **Stack:** Ollama (Mistral LLM) → Flask AI Service → Spring Boot Backend

---

## 🏗️ Project Structure

```
ohiooo/
├── ai-service/                  ← DAY 1: Flask + Ollama
│   ├── app.py
│   └── requirements.txt
│
└── interview-service/           ← DAY 2: Spring Boot
    ├── pom.xml
    └── src/main/
        ├── java/com/interviewprep/interviewservice/
        │   ├── InterviewServiceApplication.java
        │   ├── config/
        │   │   └── AppConfig.java
        │   ├── controller/
        │   │   └── InterviewController.java
        │   ├── dto/
        │   │   └── GenerateRequest.java
        │   ├── entity/
        │   │   └── InterviewSession.java
        │   ├── enums/
        │   │   ├── RoleType.java
        │   │   └── QuestionType.java
        │   ├── repository/
        │   │   └── InterviewSessionRepository.java
        │   └── service/
        │       └── AIClientService.java
        └── resources/
            └── application.properties
```

---

## 🚀 How to Run (Full Stack)

### Step 1 – Start Ollama
```bash
ollama serve
# (In another terminal) pull the model if not done yet:
ollama pull mistral
```

### Step 2 – Start Flask AI Service
```bash
cd ai-service
pip install -r requirements.txt
python app.py
# Runs on http://localhost:5000
```

### Step 3 – Start Spring Boot
```bash
cd interview-service
mvn spring-boot:run
# Runs on http://localhost:8080
```

---

## 🧪 API Endpoints

### Flask AI Service (port 5000)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET  | `/health` | Health check |
| POST | `/generate-subjective` | Generate 5 subjective questions |
| POST | `/generate-mcq` | Generate 5 MCQ questions |

### Spring Boot (port 8080)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET  | `/api/interview/health` | Health check |
| POST | `/api/interview/generate-subjective` | Proxied through Flask → Ollama |
| POST | `/api/interview/generate-mcq` | Proxied through Flask → Ollama |

### Sample Request Body
```json
{
  "role": "Java Developer",
  "level": "medium"
}
```

---

## 🎭 Supported Roles

| Role | Subjective | MCQ | Coding |
|------|-----------|-----|--------|
| Java Developer | ✅ | ✅ | ✅ |
| Python Developer | ✅ | ✅ | ✅ |
| C Programmer | ✅ | ✅ | ✅ |
| C++ Programmer | ✅ | ✅ | ✅ |
| DevOps Engineer | ✅ | ✅ | ❌ |
| QA Engineer | ✅ | ✅ | ❌ |
| Data Analyst | ✅ | ✅ | ❌ |
| Web Developer | ✅ | ✅ | ❌ |

---

## 🔁 System Flow

```
User Request
    ↓
Spring Boot :8080  (/api/interview/generate-subjective)
    ↓
Flask AI Service :5000  (/generate-subjective)
    ↓
Ollama :11434  (mistral model)
    ↓
JSON Questions returned up the chain
```

---

## 🗄️ H2 Database Console

While Spring Boot is running, access the in-memory DB at:
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:interviewdb
Username: sa
Password: (empty)
```
