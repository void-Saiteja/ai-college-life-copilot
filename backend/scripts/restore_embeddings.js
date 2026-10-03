import fs from 'fs';
import path from 'path';
import mysql from 'mysql2/promise';
import { fileURLToPath } from 'url';
import { config } from '../src/config/env.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const BACKUPS_DIR = path.join(__dirname, '../backups');

/**
 * Restores document_chunks from a previously exported JSON backup file.
 */
async function restoreEmbeddings(backupFilePath) {
  let targetPath = backupFilePath;

  if (!targetPath) {
    // Find most recent backup in backups directory
    if (!fs.existsSync(BACKUPS_DIR)) {
      throw new Error(`Backups directory not found: ${BACKUPS_DIR}`);
    }
    const files = fs.readdirSync(BACKUPS_DIR)
      .filter(f => f.startsWith('document_chunks_backup_') && f.endsWith('.json'))
      .sort()
      .reverse();

    if (files.length === 0) {
      throw new Error('No backup files found in backups directory.');
    }
    targetPath = path.join(BACKUPS_DIR, files[0]);
  }

  console.log('==================================================');
  console.log('🔄 RAG Embeddings Restore Tool');
  console.log('==================================================');
  console.log(`Reading backup file: ${targetPath}`);

  const raw = fs.readFileSync(targetPath, 'utf-8');
  const payload = JSON.parse(raw);

  if (!payload || !Array.isArray(payload.records)) {
    throw new Error('Invalid backup file format: missing records array');
  }

  console.log(`Loaded ${payload.records.length} records from backup dated ${payload.timestamp}.`);
  console.log(`Target database: ${config.db.host}:${config.db.port}/${config.db.database}`);

  const conn = await mysql.createConnection({
    host: config.db.host,
    port: config.db.port,
    user: config.db.user,
    password: config.db.password,
    database: config.db.database,
    ssl: config.db.ssl,
    connectTimeout: 10000
  });

  try {
    let restoredCount = 0;
    for (const r of payload.records) {
      const vecVal = typeof r.vector_embedding === 'object' && r.vector_embedding !== null
        ? JSON.stringify(r.vector_embedding)
        : r.vector_embedding;

      await conn.query(
        `UPDATE document_chunks 
         SET vector_embedding = ?,
             embedding_model = ?,
             embedding_dimension = ?,
             embedding_version = ?
         WHERE id = ?;`,
        [
          vecVal,
          r.embedding_model || null,
          r.embedding_dimension || null,
          r.embedding_version || null,
          r.id
        ]
      );
      restoredCount++;
    }

    console.log(`\n✅ Restore complete! Restored ${restoredCount} chunk records.`);
    console.log('==================================================');
  } finally {
    await conn.end();
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const customPath = process.argv[2];
  restoreEmbeddings(customPath).catch(err => {
    console.error('❌ Restore failed:', err.message || err);
    process.exit(1);
  });
}

export { restoreEmbeddings };
