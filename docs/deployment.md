# Production Deployment & Aiven Cloud MySQL Guide

## Architecture Topology
```
User Client -> Vercel / Netlify Frontend -> Render / Railway Backend -> Aiven Cloud MySQL
```

## Aiven Cloud MySQL Setup
1. Create a MySQL database instance on Aiven Console.
2. Download the SSL certificate (`ca.pem`) if required or enable `DB_SSL=true` with `rejectUnauthorized: false`.
3. Obtain host, port, user, password, and database name.
4. Set environment variables in hosted backend service (e.g., Render/Railway):
   ```env
   DB_HOST=vn-mysql-cloud.aivencloud.com
   DB_PORT=25000
   DB_USER=avnadmin
   DB_PASSWORD=your_password
   DB_NAME=ai_college_copilot
   DB_SSL=true
   JWT_SECRET=production_jwt_secret_key
   ```
5. Execute `src/database/schema.sql` and `src/database/seed.sql` on the Aiven database.

## Docker Deployment
```bash
# Build and run containers locally
docker compose build
docker compose up -d
```
- Frontend: `http://localhost:80`
- Backend: `http://localhost:5000`
