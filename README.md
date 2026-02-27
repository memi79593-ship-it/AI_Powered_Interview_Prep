# 🎯 AI-Powered Interview Prep System

> Full-stack interview prep with JWT auth, analytics, PDF reports.
>
> **Stack:** React + Vite (Frontend) → Spring Boot + MySQL (Backend) → Flask + Gemini (AI)

---

## 🏗️ Project Structure

```
AI_Interview_Prep/
├── frontend/                    ← React + Vite UI (port 5173)
│   ├── src/
│   │   ├── pages/               ← Login, Dashboard, Interview, etc.
│   │   ├── components/          ← Navbar
│   │   └── services/api.js      ← Calls backend at /api via Vite proxy
│   ├── package.json
│   └── vite.config.js           ← Proxies /api → http://localhost:8080
│
├── interview-service/           ← Spring Boot (port 8080)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/interviewprep/interviewservice/
│       │   ├── InterviewServiceApplication.java
│       │   ├── config/          ← Security, Password, App config
│       │   ├── controller/      ← Auth, Session, Dashboard, Report, etc.
│       │   ├── dto/             ← Request/response DTOs
│       │   ├── entity/          ← JPA entities
│       │   ├── enums/           ← Roles, Question types
│       │   ├── repository/      ← Spring Data repositories
│       │   └── service/         ← Domain services (AI client, scoring)
│       └── resources/application.properties
│
└── ai-service/                  ← Flask AI (port 5000)
    ├── app.py                   ← Registers blueprints, CORS, rate limits
    ├── routes/                  ← subjective, mcq, coding, analytics
    ├── services/ollama_service.py  ← Gemini-backed LLM calls
    ├── config.py                ← Env-based config (Gemini)
    └── requirements.txt
```

---

## 🚀 How to Run (Full Stack)

### Prerequisites
- Node.js 18+
- Python 3.10+
- Java 17, Maven
- MySQL 8+ running locally
- Google Gemini API key (for AI generation)
- Optional: JDoodle client id/secret (if enabling code execution)

### Step 1 – Configure environment
- ai-service/.env (create):
  - GEMINI_API_KEY=your_key
  - MODEL_NAME=gemini-2.5-flash
  - RATE_AI_GENERATE=10 per minute
  - RATE_CODE_EXEC=30 per minute
  - JD_CLIENT_ID=your_jdoodle_id
  - JD_CLIENT_SECRET=your_jdoodle_secret
- interview-service application.properties or environment:
  - DB_USER, DB_PASS
  - FLASK_URL=http://localhost:5000
  - JWT_SECRET, JWT_EXPIRY_MS

### Step 2 – Start Flask AI Service
```bash
cd ai-service
pip install -r requirements.txt
python app.py
# http://localhost:5000
```

### Step 3 – Start Spring Boot Backend
```bash
cd interview-service
mvn spring-boot:run
# http://localhost:8080
```

### Step 4 – Start Frontend (Vite)
```bash
cd frontend
npm install
npm run dev
# http://localhost:5173  (proxied /api → http://localhost:8080)
```

---

## 🧪 API Endpoints

### Flask AI Service (port 5000)
- GET `/health` – Health check
- POST `/generate-subjective` – Generate subjective questions
- POST `/evaluate-subjective` – Evaluate an answer
- POST `/generate-mcq` – Generate MCQs
- POST `/evaluate-mcq` – Evaluate MCQ answers
- POST `/generate-coding` – Generate coding problems (optional)
- POST `/execute-code` – Execute code via JDoodle (optional)
- POST `/generate-performance-summary` – Summarize performance

### Spring Boot (port 8080)
- GET `/api/interview/health` – Health check
- Auth: POST `/api/auth/register`, POST `/api/auth/login`, GET `/api/auth/validate`
- Sessions:
  - POST `/api/session/start`
  - GET `/api/session/{id}/questions`
  - POST `/api/session/submit-answer`
  - POST `/api/session/complete/{id}`
  - POST `/api/session/{id}/followup`
  - POST `/api/session/{sid}/questions/{qid}/model-answer`
- Dashboard:
  - GET `/api/dashboard/{email}`
  - GET `/api/dashboard/{email}/recommend/{role}`
  - GET `/api/dashboard/{email}/wtopics/{sid}`
  - POST `/api/dashboard/{email}/summary`
- Skill Profile:
  - GET `/api/profile/{email}/{role}`
  - GET `/api/profile/{email}`
- Leaderboard:
  - GET `/api/leaderboard`
  - GET `/api/leaderboard/{role}`
- Reports:
  - GET `/api/report/{email}` → PDF download
- Admin:
  - GET `/api/admin/users`
  - GET `/api/admin/analytics`

### Sample Generate Request (AI service)
```json
{
  "role": "Java Developer",
  "level": "medium"
}
```

---

## 🎭 Supported Roles

- Java Developer, Python Developer, C/C++, DevOps, QA, Data Analyst, Web Developer
- Subjective and MCQ generation supported across roles
- Coding generation/execution optional (requires JDoodle credentials)

---

## 🔁 System Flow

```
Frontend (Vite :5173) → Backend (Spring Boot :8080) → AI Service (Flask :5000) → Gemini
```

---

## 🗄️ Database
- MySQL JDBC: `jdbc:mysql://localhost:3306/interviewdb`
- Configure credentials via environment variables `DB_USER`, `DB_PASS`
- Tables auto-created/updated via `spring.jpa.hibernate.ddl-auto=update`
