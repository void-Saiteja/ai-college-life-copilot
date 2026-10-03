import mysql from 'mysql2/promise';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { config } from '../config/env.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export async function setupMySQLDatabase() {
  // 1. In local development or self-hosted MySQL, attempt to ensure DB exists.
  // In cloud-managed MySQL (such as Aiven), databases are pre-provisioned (e.g. defaultdb)
  // and users typically lack global CREATE DATABASE privileges.
  try {
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
  } catch (adminErr) {
    // Gracefully handle permission denials on managed cloud providers like Aiven
    if (adminErr.code !== 'ER_DBACCESS_DENIED_ERROR' && adminErr.code !== 'ER_ACCESS_DENIED_ERROR') {
      // Non-fatal warning if direct connection without DB failed
      // (Managed cloud instances often reject connections that omit the database name)
    }
  }

  // 2. Connect to the designated database using a connection pool
  const pool = mysql.createPool({
    host: config.db.host,
    port: config.db.port,
    user: config.db.user,
    password: config.db.password,
    database: config.db.database,
    ssl: config.db.ssl,
    multipleStatements: true,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
    enableKeepAlive: true,
    keepAliveInitialDelay: 10000
  });

  // Verify pool connectivity with ping
  const conn = await pool.getConnection();
  await conn.ping();
  conn.release();

  // 3. Execute Schema DDL
  const schemaPath = path.join(__dirname, 'schema.sql');
  if (fs.existsSync(schemaPath)) {
    const rawSchema = fs.readFileSync(schemaPath, 'utf-8');
    // Strip comment-only lines and split by semicolon
    const cleanStatements = rawSchema
      .split('\n')
      .filter(line => !line.trim().startsWith('--'))
      .join('\n')
      .split(';')
      .map(s => s.trim())
      .filter(s => s.length > 5);

    for (const stmt of cleanStatements) {
      await pool.query(stmt);
    }
  }

  // 4. Execute Seed DML
  const seedPath = path.join(__dirname, 'seed.sql');
  if (fs.existsSync(seedPath)) {
    const rawSeed = fs.readFileSync(seedPath, 'utf-8');
    const cleanSeedStmts = rawSeed
      .split('\n')
      .filter(line => !line.trim().startsWith('--'))
      .join('\n')
      .split(';')
      .map(s => s.trim())
      .filter(s => s.length > 5);

    for (const stmt of cleanSeedStmts) {
      await pool.query(stmt).catch(() => {}); // Ignore duplicate keys on seed re-run
    }
  }

  return pool;
}
