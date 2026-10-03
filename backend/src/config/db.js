import mysql from 'mysql2/promise';
import { config } from './env.js';
import { setupMySQLDatabase } from '../database/init.js';

let pool = null;
let isMySQLEnabled = false;

export async function initDB() {
  try {
    pool = await setupMySQLDatabase();
    
    // Ping check
    const conn = await pool.getConnection();
    await conn.ping();
    conn.release();

    isMySQLEnabled = true;
    console.log(`==================================================`);
    console.log(`✅ MySQL Database Active & Synchronized`);
    console.log(`🗄️ Host: ${config.db.host}:${config.db.port} | DB: ${config.db.database}`);
    console.log(`==================================================`);
  } catch (err) {
    if (config.nodeEnv === 'production') {
      console.error(`[FATAL DATABASE ERROR]: Unable to connect to MySQL in production mode: ${err.message}`);
      throw new Error(`Production database connection failed: ${err.message}. Silent fallback to local JSON storage is disabled in production.`);
    }
    console.warn(`==================================================`);
    console.warn(`⚠️ MySQL Connection Notice: (${err.message})`);
    console.warn(`🔄 Falling back to local JSON storage engine (Development Mode).`);
    console.warn(`==================================================`);
    isMySQLEnabled = false;
  }
}

export function getPool() {
  return pool;
}

export function isMySQL() {
  return isMySQLEnabled;
}
