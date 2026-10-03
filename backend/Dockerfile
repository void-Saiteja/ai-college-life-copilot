# Node.js Express Backend Production Dockerfile
FROM node:20-alpine

# Set non-root working environment
WORKDIR /app

# Install production dependencies only
COPY package*.json ./
RUN npm ci --omit=dev

# Copy application source code (secrets and dev files excluded via .dockerignore)
COPY . .

# Set default production environment variables
ENV NODE_ENV=production
ENV PORT=5000

# Expose backend REST API port
EXPOSE 5000

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:5000/api/health || exit 1

# Start production server
CMD ["node", "server.js"]
