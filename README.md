# 🎯 AI-Powered Interview Prep System

Full-stack AI interview preparation platform with JWT authentication,
analytics dashboard, leaderboard, and PDF performance reports.

------------------------------------------------------------------------

## 🚀 Tech Stack

-   **Frontend:** React + Vite\
-   **Backend:** Spring Boot + MySQL\
-   **AI Service:** Flask + Google Gemini\
-   **Authentication:** JWT\
-   **Optional Code Execution:** JDoodle API

------------------------------------------------------------------------

## 🏗️ Architecture

Frontend (Vite :5173)\
↓\
Spring Boot Backend (:8080)\
↓\
Flask AI Service (:5000)\
↓\
Google Gemini API

------------------------------------------------------------------------

## 📂 Project Structure

AI_Interview_Prep/\
├── frontend/ \# React + Vite (Port 5173)\
├── interview-service/ \# Spring Boot (Port 8080)\
└── ai-service/ \# Flask AI (Port 5000)

------------------------------------------------------------------------

## 🔐 Key Features

-   JWT-based Authentication (Register/Login)\
-   Role-based Interview Generation\
-   Subjective + MCQ + Coding Support\
-   AI Evaluation & Confidence Scoring\
-   Leaderboard & Performance Analytics\
-   PDF Report Generation\
-   Rate Limiting for AI & Code Execution\
-   Admin Analytics Panel

------------------------------------------------------------------------

## 🎭 Supported Roles

-   Java Developer\
-   Python Developer\
-   C/C++ Developer\
-   DevOps Engineer\
-   QA Engineer\
-   Data Analyst\
-   Web Developer

------------------------------------------------------------------------

## 🧪 Core APIs

### 🔹 AI Service (Flask -- 5000)

-   GET /health\
-   POST /generate-subjective\
-   POST /evaluate-subjective\
-   POST /generate-mcq\
-   POST /evaluate-mcq\
-   POST /generate-coding\
-   POST /execute-code\
-   POST /generate-performance-summary

### 🔹 Backend (Spring Boot -- 8080)

-   /api/auth/\*\
-   /api/session/\*\
-   /api/dashboard/\*\
-   /api/profile/\*\
-   /api/leaderboard\
-   /api/report/{email} (PDF)

------------------------------------------------------------------------

## 🗄️ Database

MySQL\
jdbc:mysql://localhost:3306/interviewdb

Auto schema update:\
spring.jpa.hibernate.ddl-auto=update

------------------------------------------------------------------------

## ⚙️ How to Run

### 1️⃣ Start AI Service

cd ai-service\
pip install -r requirements.txt\
python app.py

------------------------------------------------------------------------

### 2️⃣ Start Spring Boot

cd interview-service\
mvn spring-boot:run

------------------------------------------------------------------------

### 3️⃣ Start Frontend

cd frontend\
npm install\
npm run dev

Open: http://localhost:5173

------------------------------------------------------------------------

## 🔑 Environment Configuration

### AI Service (.env)

GEMINI_API_KEY=your_key\
MODEL_NAME=gemini-2.5-flash\
RATE_AI_GENERATE=10 per minute\
RATE_CODE_EXEC=30 per minute\
JD_CLIENT_ID=your_id\
JD_CLIENT_SECRET=your_secret

### Backend (application.properties)

DB_USER=your_user\
DB_PASS=your_pass\
FLASK_URL=http://localhost:5000\
JWT_SECRET=your_secret\
JWT_EXPIRY_MS=86400000

------------------------------------------------------------------------

## 📊 System Flow

1.  User logs in (JWT issued)\
2.  Starts interview session\
3.  Backend requests AI generation\
4.  Flask calls Gemini API\
5.  Evaluation stored in MySQL\
6.  Dashboard + Leaderboard updated\
7.  PDF performance report generated

------------------------------------------------------------------------

## 🎥 Demo

(Add your YouTube / Google Drive demo link here)

------------------------------------------------------------------------

## 📌 Resume Description

Developed a full-stack AI-powered Interview Preparation Platform using
React, Spring Boot, Flask, MySQL, and Google Gemini API with JWT
authentication, analytics dashboard, leaderboard, and PDF reporting
system.
