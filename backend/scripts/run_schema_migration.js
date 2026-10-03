import mysql from 'mysql2/promise';
import { config } from '../src/config/env.js';

async function migrateSchema() {
  console.log('==================================================');
  console.log('🛠️ Aiven MySQL Schema Migration: document_chunks');
  console.log('==================================================');
  console.log(`Connecting to: ${config.db.host}:${config.db.port}/${config.db.database}`);

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
    // 1. Pre-check row count
    const [countBefore] = await conn.query('SELECT COUNT(*) as cnt FROM document_chunks;');
    const rowCountBefore = countBefore[0].cnt;
    console.log(`Row count before migration: ${rowCountBefore}`);

    // 2. Execute ALTER TABLE
    console.log('\nExecuting ALTER TABLE statement...');
    try {
      await conn.query(`
        ALTER TABLE document_chunks
        ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(64) NULL,
        ADD COLUMN IF NOT EXISTS embedding_dimension INT NULL,
        ADD COLUMN IF NOT EXISTS embedding_version VARCHAR(32) NULL;
      `);
      console.log('ALTER TABLE (with IF NOT EXISTS) executed successfully.');
    } catch (sqlErr) {
      if (sqlErr.message && sqlErr.message.includes('IF NOT EXISTS')) {
        console.log('MySQL server does not support "IF NOT EXISTS" in ALTER TABLE. Executing standard MySQL syntax...');
        await conn.query(`
          ALTER TABLE document_chunks
          ADD COLUMN embedding_model VARCHAR(64) NULL,
          ADD COLUMN embedding_dimension INT NULL,
          ADD COLUMN embedding_version VARCHAR(32) NULL;
        `);
        console.log('ALTER TABLE executed successfully using standard MySQL syntax.');
      } else {
        throw sqlErr;
      }
    }

    // 3. Schema verification
    console.log('\nVerifying table columns:');
    const [columns] = await conn.query('SHOW COLUMNS FROM document_chunks;');
    const relevantCols = columns.filter(c => 
      ['embedding_model', 'embedding_dimension', 'embedding_version'].includes(c.Field)
    );

    for (const c of relevantCols) {
      console.log(`- Column: ${c.Field} | Type: ${c.Type} | Nullable: ${c.Null} | Default: ${c.Default}`);
    }

    // 4. Verify existing row integrity
    console.log('\nVerifying row data integrity:');
    const [rows] = await conn.query(
      `SELECT id, document_id, chunk_index, content, page_number, 
              vector_embedding, embedding_model, embedding_dimension, embedding_version 
       FROM document_chunks 
       ORDER BY chunk_index ASC;`
    );

    console.log(`Row count after migration: ${rows.length}`);
    for (const r of rows) {
      console.log(`- Chunk [${r.id}]: doc=${r.document_id}, idx=${r.chunk_index}, page=${r.page_number}, vec=${r.vector_embedding}, model=${r.embedding_model}, dim=${r.embedding_dimension}, ver=${r.embedding_version}`);
      console.log(`  content: "${r.content.substring(0, 50)}..."`);
    }

    const allNullMetadata = rows.every(r => r.embedding_model === null && r.embedding_dimension === null && r.embedding_version === null);
    console.log(`\nMetadata remains unpopulated (null): ${allNullMetadata}`);
    console.log('==================================================');
  } finally {
    await conn.end();
  }
}

migrateSchema().catch(err => {
  console.error('Migration failed:', err.message || err);
  process.exit(1);
});
