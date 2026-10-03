# Production Deployment & Dockerization Guide

This document describes how to deploy and run **AI College Life Copilot** using Docker and Docker Compose with **Aiven Cloud MySQL** and **Google Gemini AI**.

---

## 1. System Architecture Topology

```text
React Frontend (Nginx SPA Container :80)
               │
               ▼ HTTP /api/ reverse proxy
Node.js Express Backend (Alpine Container :5000)
               │
               ├────────────────────────────────────────┐
               ▼ TLS 1.3 encrypted                      ▼ HTTPS REST
    Aiven Cloud MySQL Database                Google Gemini AI Services
  (defaultdb @ *.aivencloud.com)              (text-embedding-004 & LLM)
```

> [!IMPORTANT]
> **No Local MySQL Container**: The architecture intentionally connects directly to the high-availability managed Aiven Cloud MySQL cluster. No local or containerized MySQL database is created.

---

## 2. Security & Secret Protection

- **No Baked Secrets**: Application credentials (`DB_PASSWORD`, `GEMINI_API_KEY`, `JWT_SECRET`) are never copied or baked into Docker images.
- **`.dockerignore` Enforcement**: All `.dockerignore` files explicitly exclude `.env`, `ca.pem`, `data/*.json`, `node_modules`, and `.git`.
- **Runtime Environment Injection**: Docker Compose injects environment variables at container startup via `env_file: ./backend/.env`.
- **Secure CA Mount**: The Aiven CA certificate (`backend/ca.pem`) is mounted read-only (`:ro`) at runtime into `/app/ca.pem`.

---

## 3. Configuration & Prerequisites

### Required Files:
1. `backend/.env` containing verified Aiven credentials:
   ```env
   PORT=5000
   NODE_ENV=production
   CLIENT_ORIGIN=*

   DB_HOST=mysql-16c8b5ec-anurag-6ecc.l.aivencloud.com
   DB_PORT=21653
   DB_USER=avnadmin
   DB_PASSWORD=YOUR_AIVEN_PASSWORD
   DB_NAME=defaultdb
   DB_SSL=true
   DB_SSL_CA_PATH=/app/ca.pem
   DB_SSL_REJECT_UNAUTHORIZED=true

   JWT_SECRET=your_production_jwt_secret_key
   GEMINI_API_KEY=your_gemini_api_key
   ```
2. `backend/ca.pem` containing the verified Aiven Project CA certificate.

---

## 4. Running with Docker Compose

### Step 1: Build the Docker Images
```bash
docker compose build
```

### Step 2: Start the Containers
```bash
docker compose up -d
```

### Step 3: Verify Container Health
```bash
docker compose ps
```
Both containers will display status `Up` (with backend displaying `(healthy)`).

### Step 4: Access the Application
- **Frontend Web UI**: [http://localhost](http://localhost) (Port 80)
- **Backend Health Check**: [http://localhost:5000/api/health](http://localhost:5000/api/health)

### Step 5: View Container Logs
```bash
docker compose logs -f backend
docker compose logs -f frontend
```
Confirm the backend logs output:
```text
✅ MySQL Database Active & Synchronized
🗄️ Host: mysql-16c8b5ec-anurag-6ecc.l.aivencloud.com:21653 | DB: defaultdb
🚀 AI College Life Copilot Backend Active
```

### Step 6: Stopping Containers
```bash
docker compose down
```

---

## 5. Non-Docker Local Operation (Alternative)

To run the application locally without Docker containers:
```bash
# Terminal 1: Backend
cd backend
npm start

# Terminal 2: Frontend
cd frontend
npm run dev
```
Local non-Docker development reads `backend/.env` and `backend/ca.pem` directly from the local filesystem.
