import dotenv from 'dotenv';
dotenv.config();

const nodeEnv = process.env.NODE_ENV || 'development';
const rawJwtSecret = process.env.JWT_SECRET;
const defaultDevSecret = 'ai_college_copilot_secret_key_jwt_2026';

if (nodeEnv === 'production' && (!rawJwtSecret || rawJwtSecret === defaultDevSecret)) {
  console.error('[FATAL SECURITY ERROR]: Production environment detected, but JWT_SECRET is unset or using default development fallback secret!');
  throw new Error('FATAL: A strong, unique JWT_SECRET must be configured via environment variables in production.');
}

export const config = {
  port: parseInt(process.env.PORT || '5000', 10),
  nodeEnv,
  clientOrigin: process.env.CLIENT_ORIGIN || '*',
  jwtSecret: rawJwtSecret || defaultDevSecret,
  adminSecret: process.env.ADMIN_SECRET || 'dev_admin_secret_key_2026',
  geminiApiKey: process.env.GEMINI_API_KEY || process.env.AI_API_KEY || '',
  aiApiKey: process.env.AI_API_KEY || '',
  rateLimit: {
    generalMax: parseInt(process.env.RATE_LIMIT_MAX || (nodeEnv === 'production' ? '300' : '10000'), 10),
    authMax: parseInt(process.env.AUTH_RATE_LIMIT_MAX || (nodeEnv === 'production' ? '60' : '5000'), 10),
    windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '60000', 10)
  },
  db: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '3306', 10),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'ai_college_copilot',
    ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false
  }
};
