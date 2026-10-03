import mysql from 'mysql2/promise';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { config } from '../config/env.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export async function setupMySQLDatabase() {
  try {
    // 1. Connect without database to ensure DB exists
    const adminConn = await mysql.createConnection({
      host: config.db.host,
      port: config.db.port,
      user: config.db.user,
      password: config.db.password,
      ssl: config.db.ssl,
      connectTimeout: 5000
    });

    await adminConn.query(`CREATE DATABASE IF NOT EXISTS \`${config.db.database}\`;`);
    await adminConn.end();

    // 2. Connect to specific database
    const pool = mysql.createPool({
      host: config.db.host,
      port: config.db.port,
      user: config.db.user,
      password: config.db.password,
      database: config.db.database,
      ssl: config.db.ssl,
      multipleStatements: true,
      waitForConnections: true,
      connectionLimit: 10
    });

    // 3. Execute Schema DDL
    const schemaPath = path.join(__dirname, 'schema.sql');
    if (fs.existsSync(schemaPath)) {
      const schemaSql = fs.readFileSync(schemaPath, 'utf-8');
      const statements = schemaSql.split(';').map(s => s.trim()).filter(Boolean);
      for (const stmt of statements) {
        if (stmt.length > 5) {
          await pool.query(stmt);
        }
      }
    }

    // 4. Execute Seed DML
    const seedPath = path.join(__dirname, 'seed.sql');
    if (fs.existsSync(seedPath)) {
      const seedSql = fs.readFileSync(seedPath, 'utf-8');
      const seedStmts = seedSql.split(';').map(s => s.trim()).filter(Boolean);
      for (const stmt of seedStmts) {
        if (stmt.length > 5) {
          await pool.query(stmt).catch(() => {}); // Ignore duplicate keys on seed re-run
        }
      }
    }

    return pool;
  } catch (err) {
    throw err;
  }
}
