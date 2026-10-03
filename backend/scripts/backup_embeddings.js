import fs from 'fs';
import path from 'path';
import crypto from 'crypto';
import mysql from 'mysql2/promise';
import { fileURLToPath } from 'url';
import { config } from '../src/config/env.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const BACKUPS_DIR = path.join(__dirname, '../backups');

/**
 * Safely backs up existing document_chunks from Aiven MySQL to a local timestamped JSON file.
 * Read-only operation: NEVER modifies production data.
 */
async function backupEmbeddings() {
  console.log('==================================================');
  console.log('📦 RAG Embeddings Pre-Migration Backup Tool');
  console.log('==================================================');
  console.log(`Connecting to database: ${config.db.host}:${config.db.port}/${config.db.database}...`);

  if (!fs.existsSync(BACKUPS_DIR)) {
    fs.mkdirSync(BACKUPS_DIR, { recursive: true });
  }

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
    // Check available columns
    const [cols] = await conn.query('DESCRIBE document_chunks;');
    const colNames = cols.map(c => c.Field);
    
    const selectFields = ['id', 'document_id', 'chunk_index', 'content', 'page_number', 'vector_embedding'];
    if (colNames.includes('embedding_model')) selectFields.push('embedding_model');
    if (colNames.includes('embedding_dimension')) selectFields.push('embedding_dimension');
    if (colNames.includes('embedding_version')) selectFields.push('embedding_version');
    if (colNames.includes('created_at')) selectFields.push('created_at');

    console.log(`Selected columns for backup: ${selectFields.join(', ')}`);

    const [rows] = await conn.query(
      `SELECT ${selectFields.join(', ')} FROM document_chunks ORDER BY document_id, chunk_index ASC;`
    );

    console.log(`Retrieved ${rows.length} chunk record(s) from document_chunks.`);

    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const backupFileName = `document_chunks_backup_${timestamp}.json`;
    const backupFilePath = path.join(BACKUPS_DIR, backupFileName);

    const backupPayload = {
      timestamp: new Date().toISOString(),
      database: config.db.database,
      host: config.db.host,
      table: 'document_chunks',
      totalRecords: rows.length,
      columns: selectFields,
      records: rows
    };

    const jsonContent = JSON.stringify(backupPayload, null, 2);
    fs.writeFileSync(backupFilePath, jsonContent, 'utf-8');

    const fileHash = crypto.createHash('sha256').update(jsonContent).digest('hex');

    console.log('\n✅ Backup successfully created!');
    console.log(`📁 File: ${backupFilePath}`);
    console.log(`📊 Total records backed up: ${rows.length}`);
    console.log(`🔒 SHA-256 Checksum: ${fileHash}`);
    console.log('==================================================');
  } finally {
    await conn.end();
  }
}

// Only execute directly when run as CLI script
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  backupEmbeddings().catch(err => {
    console.error('❌ Backup failed:', err.message || err);
    process.exit(1);
  });
}

export { backupEmbeddings };
