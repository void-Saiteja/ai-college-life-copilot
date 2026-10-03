# 🎓 AI College Life Copilot

> An All-In-One AI Academic Assistant, Smart Study Planner, RAG Document Search Engine, Attendance Calculator, and Career Readiness Platform for College Students.

---

## 📌 Project Overview
College students often struggle with managing fragmented course schedules, tracking minimum attendance requirements across multiple subjects, prioritizing urgent assignment deadlines, preparing for midterms, and searching through lengthy academic policy PDFs.

**AI College Life Copilot** resolves these challenges by combining real-time academic record synchronization with context-aware Artificial Intelligence, Retrieval-Augmented Generation (RAG), and career preparation tools into a modern, unified web application.

---

## ✨ Key Features

- 👤 **Role-Based Authentication**: Secure JWT & Bcrypt authentication with distinct `STUDENT` and `ADMIN` roles.
- 📊 **Interactive Student Dashboard**: Real-time overview of semester GPA, attendance status, upcoming deadlines, exam timetables, notifications, and AI shortcuts.
- 🤖 **Context-Aware AI Copilot**: Modern AI chat interface pre-synced with the student's profile, subjects, attendance %, and exam dates.
- 📄 **College Document RAG AI**: Upload PDF policy documents, chunk text, and perform vector similarity search with exact document page & source attribution.
- 📅 **AI Study Planner with Replan**: Generate personalized daily study timetables. The "Replan" feature dynamically shifts missed sessions without overwriting completed progress!
- 📈 **Attendance & Mathematical Projection Calculator**: Track subject attendance percentages and run projections (*"If I miss/attend X classes, what will my attendance become?"*).
- ✏️ **Assignments & Exam Manager**: Priority matrix filtering (High/Medium/Low) for homework and exam timetables.
- 💡 **AI MCQ Quiz Generator & Revision Tips**: Generate quizzes by topic and difficulty, receive instant score breakdowns, explanations, and celebratory confetti.
- 🕒 **Class Timetable & Schedule Manager**: Manage weekly lectures, labs, room assignments, instructor contacts, and day-by-day filter views with today's class alerts.
- 📚 **Study Notes & AI Study Lab**: Markdown notes repository with tagging, search, and 1-click conversion into AI executive summaries, interactive flashcards, and quizzes.
- 💵 **Student Budget & AI Cheap Meal Planner**: Track monthly target allowances, category spending breakdowns, and AI-recommended low-cost healthy student meals.
- 💼 **Career Resume & Mock Interview Practice**: Extract resume skills against target engineering roles (Frontend, Backend, Full Stack, Data Analyst, ML Eng) and practice interactive AI technical interviews.
- 🛡️ **Admin Control Console**: Manage campus documents, course catalog, FAQs, and global student announcements.

---

## 🛠️ Technology Stack

- **Frontend**: React 19, Vite, React Router DOM, Axios, Lucide React Icons, Canvas Confetti.
- **Backend**: Node.js (ES Modules), Express.js, JWT (`jsonwebtoken`), Password Hashing (`bcryptjs`), Multer File Parser.
- **Database**: MySQL 8.0+ / Aiven Cloud MySQL (SSL Pool Connection) with local JSON storage engine fallback.
- **AI & RAG Engine**: Gemini LLM API integration, Term Frequency Vector Embeddings, Cosine Similarity Ranking, Context Synthesis Engine.
- **Containerization**: Docker, Docker Compose, Multi-stage Nginx builds.

---

## 📁 Repository Folder Structure

```
ai-college-copilot/
├── backend/
│   ├── src/
│   │   ├── config/ (env.js, db.js)
│   │   ├── controllers/ (auth, student, attendance, planner, chat, rag, quiz, career, admin)
│   │   ├── database/ (schema.sql, seed.sql)
│   │   ├── middleware/ (authMiddleware, roleMiddleware, errorHandler)
│   │   ├── routes/ (all REST API endpoints)
│   │   ├── services/ (aiService, contextService, pdfService, embeddingService, ragService)
│   │   ├── storage/ (db.js)
│   │   └── utils/ (jwt.js, passwords.js)
│   ├── server.js
│   ├── package.json
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── components/ (Navbar, Sidebar, ProtectedRoute)
│   │   ├── context/ (AuthContext.jsx)
│   │   ├── layouts/ (MainLayout.jsx)
│   │   ├── pages/ (Home, Login, Register, StudentDashboard, AdminDashboard, Copilot, Planner, Schedule, Notes, Budget, Attendance, Assignments, Exams, Quiz, Documents, Career, Notifications, Profile)
│   │   ├── services/ (api.js)
│   │   ├── App.jsx
│   │   ├── index.css
│   │   └── main.jsx
│   ├── package.json
│   └── Dockerfile
├── docs/ (architecture.md, database.md, api.md, ai.md, rag.md, deployment.md, testing.md)
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## 🚀 How to Run the Project Locally

### 1. Prerequisites
- Node.js v18+ & npm
- (Optional) MySQL server or Aiven Cloud MySQL credentials

### 2. Backend Setup
```bash
cd backend
npm install
node server.js
```
The backend server starts at `http://localhost:5000`.

### 3. Frontend Setup
```bash
cd frontend
npm install
npm run dev
```
The React frontend starts at `http://localhost:5173`.

---

## ⚡ Instant One-Click Demo Credentials

| Role | Email | Password |
| :--- | :--- | :--- |
| **Student** | `alex@student.edu` | `Student@123` |
| **Admin** | `admin@college.edu` | `Admin@123` |

---

## 🐳 Docker Setup
Run both frontend and backend using Docker Compose:
```bash
docker compose build
docker compose up -d
```
- Frontend: `http://localhost:80`
- Backend: `http://localhost:5000`

---

## 📄 Documentation Sitemap
- [System Architecture](docs/architecture.md)
- [Database Schema & ER Diagram](docs/database.md)
- [API Documentation](docs/api.md)
- [AI Context Architecture](docs/ai.md)
- [RAG Document Pipeline](docs/rag.md)
- [Aiven Cloud Deployment Guide](docs/deployment.md)
- [Comprehensive Test Matrix & Report](docs/testing.md)
