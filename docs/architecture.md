# System Architecture - AI College Life Copilot

## Overview
AI College Life Copilot is built using a modern decoupled 3-tier client-server architecture designed for high scalability, real-time context retrieval, and seamless AI workflow integration.

```
+-----------------------------------------------------------------------+
|                         User Browser Client                           |
|  React 19 + Vite + React Router + Axios + Glassmorphism UI Components |
+-----------------------------------------------------------------------+
                                   |
                         HTTP / REST API (JWT Bearer)
                                   v
+-----------------------------------------------------------------------+
|                         Node.js / Express Backend                     |
|  - Auth Middleware & Role Guard (STUDENT / ADMIN)                     |
|  - Academic Context Aggregator (Profile, Attendance, Exams)           |
|  - RAG Retrieval Engine (PDF Extractor -> Chunks -> Cosine Similarity)|
|  - AI Services & LLM Prompt Orchestrator                              |
+-----------------------------------------------------------------------+
                                   |
         +-------------------------+-------------------------+
         |                                                   |
         v                                                   v
+-----------------------------------+             +---------------------+
|        Database Layer             |             |     AI Provider     |
| - Aiven Cloud MySQL (SSL Pool)    |             |  Google Gemini API  |
| - JSON Persistence Fallback       |             |  / LLM Service      |
+-----------------------------------+             +---------------------+
```

## Component Architecture

### 1. Frontend Layer
- **Framework**: React 19 + Vite for instant hot-module replacement and optimized production builds.
- **Routing**: React Router DOM with `ProtectedRoute` guards verifying JWT tokens and role authorizations (`STUDENT` vs `ADMIN`).
- **State Management**: Global `AuthContext` for user session persistence and token lifecycle.
- **UI System**: Vanilla CSS with glassmorphism design tokens, CSS variables, and Lucide React icons.

### 2. Backend API Layer
- **Runtime**: Node.js ES Modules with Express server.
- **Security**: Password hashing with `bcryptjs` (salt factor 10) and stateless authentication via `jsonwebtoken`.
- **Controllers**: Decoupled domain controllers handling Auth, Student Dashboard, Attendance Projections, AI Copilot, RAG Documents, Quiz Engine, Study Planner, and Career Analysis.

### 3. AI & RAG Engine
- **Context Builder**: `contextService.js` dynamically extracts real-time student records (enrolled subjects, attendance %, upcoming assignments, exam schedule) to inject rich context into LLM prompts.
- **RAG Pipeline**:
  1. PDF Document Upload & Text Extraction
  2. Text Chunking (~250-character sliding windows)
  3. Term Vector Embedding & Cosine Similarity Ranking
  4. Context Synthesis & Source Attribution Citation (Page & Document Name)
