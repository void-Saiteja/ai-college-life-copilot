import fs from 'fs';
import dotenv from 'dotenv';
dotenv.config();

const nodeEnv = process.env.NODE_ENV || 'development';
const rawJwtSecret = process.env.JWT_SECRET;
const defaultDevSecret = 'ai_college_copilot_secret_key_jwt_2026';

if (nodeEnv === 'production' && (!rawJwtSecret || rawJwtSecret === defaultDevSecret)) {
  console.error('[FATAL SECURITY ERROR]: Production environment detected, but JWT_SECRET is unset or using default development fallback secret!');
  throw new Error('FATAL: A strong, unique JWT_SECRET must be configured via environment variables in production.');
}

function normalizeCert(cert) {
  if (!cert || typeof cert !== 'string') return cert;
  let clean = cert.trim();
  if ((clean.startsWith('"') && clean.endsWith('"')) || (clean.startsWith("'") && clean.endsWith("'"))) {
    clean = clean.slice(1, -1);
  }
  return clean.replace(/\\r\\n/g, '\n').replace(/\\n/g, '\n').trim();
}

function resolveSSLConfig() {
  if (process.env.DB_SSL !== 'true') {
    return false;
  }

  let caCert = undefined;
  if (process.env.DB_SSL_CA_CERT && process.env.DB_SSL_CA_CERT.trim()) {
    caCert = normalizeCert(process.env.DB_SSL_CA_CERT);
  } else if (process.env.DB_SSL_CA_PATH && process.env.DB_SSL_CA_PATH.trim()) {
    try {
      const caPath = process.env.DB_SSL_CA_PATH.trim();
      if (fs.existsSync(caPath)) {
        caCert = normalizeCert(fs.readFileSync(caPath, 'utf-8'));
      } else {
        console.warn(`[SSL Configuration Warning]: CA certificate file not found at path: ${caPath}`);
      }
    } catch (err) {
      console.warn(`[SSL Configuration Warning]: Failed reading CA certificate: ${err.message}`);
    }
  }

  const sslOptions = {
    rejectUnauthorized: process.env.DB_SSL_REJECT_UNAUTHORIZED === 'false' ? false : true
  };

  if (caCert) {
    sslOptions.ca = caCert;
  }

  return sslOptions;
}

export const config = {
  port: parseInt(process.env.PORT || '5000', 10),
  nodeEnv,
  clientOrigin: process.env.CLIENT_ORIGIN || '*',
  jwtSecret: rawJwtSecret || defaultDevSecret,
  adminSecret: process.env.ADMIN_SECRET || 'dev_admin_secret_key_2026',
  geminiApiKey: process.env.GEMINI_API_KEY || process.env.AI_API_KEY || '',
  aiApiKey: process.env.AI_API_KEY || '',
  gemini: {
    apiKey: process.env.GEMINI_API_KEY || process.env.AI_API_KEY || '',
    generationModels: ['gemini-3.5-flash', 'gemini-3.5-flash-lite'],
    embeddingModel: 'gemini-embedding-2',
    embeddingDimension: 768,
    embeddingVersion: 'v1'
  },
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
    ssl: resolveSSLConfig()
  }
};
