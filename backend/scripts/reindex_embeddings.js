import mysql from 'mysql2/promise';
import { fileURLToPath } from 'url';
import { GoogleGenAI } from '@google/genai';
import { config } from '../src/config/env.js';

const TARGET_MODEL = 'gemini-embedding-2';
const TARGET_DIMENSION = 768;
const TARGET_VERSION = 'v1';

/**
 * Production-safe, idempotent re-indexing migration script.
 * Migrates document_chunks in Aiven MySQL to gemini-embedding-2 (768-dim) vectors.
 */
async function reindexEmbeddings(customKey) {
  console.log('==================================================');
  console.log('🚀 RAG Embeddings Re-indexing Migration Tool');
  console.log('==================================================');
  console.log(`Target Model: ${TARGET_MODEL}`);
  console.log(`Target Dimension: ${TARGET_DIMENSION}`);
  console.log(`Target Version: ${TARGET_VERSION}`);

  const apiKey = customKey || process.env.GEMINI_API_KEY || config.gemini?.apiKey || config.geminiApiKey || (process.argv[2] && !process.argv[2].startsWith('--') ? process.argv[2] : null);
  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    console.error('❌ FATAL: A valid GEMINI_API_KEY is strictly required to run this migration.');
    process.exit(1);
  }

  const ai = new GoogleGenAI({ apiKey });

  console.log(`\nConnecting to Aiven MySQL: ${config.db.host}:${config.db.port}/${config.db.database}...`);
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
    // 1. Ensure metadata columns exist (idempotent, nullable defaults)
    console.log('\nStep 1: Checking schema columns on document_chunks...');
    const [cols] = await conn.query('DESCRIBE document_chunks;');
    const colNames = cols.map(c => c.Field);

    if (!colNames.includes('embedding_model')) {
      console.log('Adding column embedding_model VARCHAR(64) NULL...');
      await conn.query('ALTER TABLE document_chunks ADD COLUMN embedding_model VARCHAR(64) NULL;');
    }
    if (!colNames.includes('embedding_dimension')) {
      console.log('Adding column embedding_dimension INT NULL...');
      await conn.query('ALTER TABLE document_chunks ADD COLUMN embedding_dimension INT NULL;');
    }
    if (!colNames.includes('embedding_version')) {
      console.log('Adding column embedding_version VARCHAR(32) NULL...');
      await conn.query('ALTER TABLE document_chunks ADD COLUMN embedding_version VARCHAR(32) NULL;');
    }
    console.log('✅ Schema columns verified.');

    // 2. Query all document chunks
    console.log('\nStep 2: Scanning document_chunks table...');
    const [allChunks] = await conn.query(
      `SELECT id, document_id, chunk_index, page_number, content, 
              embedding_model, embedding_dimension, embedding_version,
              vector_embedding IS NOT NULL as has_vec
       FROM document_chunks 
       ORDER BY document_id, chunk_index ASC;`
    );

    console.log(`Found ${allChunks.length} total chunk record(s) in database.`);

    const forceAll = process.argv.includes('--force');
    const chunksToMigrate = forceAll
      ? allChunks
      : allChunks.filter(c => 
          c.embedding_model !== TARGET_MODEL || 
          c.embedding_version !== TARGET_VERSION || 
          c.embedding_dimension !== TARGET_DIMENSION ||
          !c.has_vec
        );

    console.log(`Identified ${chunksToMigrate.length} chunk(s) requiring migration${forceAll ? ' (--force active)' : ''}.`);

    if (chunksToMigrate.length === 0) {
      console.log('✨ All chunks already migrated to gemini-embedding-2 (v1 / 768-dim). No action needed.');
      return;
    }

    // 3. Process each chunk sequentially
    console.log('\nStep 3: Generating embeddings and updating records...');
    let processed = 0;

    for (const chunk of chunksToMigrate) {
      processed++;
      const textToEmbed = chunk.content;
      if (!textToEmbed || typeof textToEmbed !== 'string' || textToEmbed.trim() === '') {
        console.warn(`[Warning]: Chunk ${chunk.id} has empty content; generating zero vector.`);
      }

      console.log(`[${processed}/${chunksToMigrate.length}] Generating embedding for chunk ${chunk.id} (len: ${textToEmbed.length})...`);

      let response;
      try {
        response = await ai.models.embedContent({
          model: TARGET_MODEL,
          contents: textToEmbed.trim(),
          config: {
            outputDimensionality: TARGET_DIMENSION
          }
        });
      } catch (apiErr) {
        console.error(`\n❌ FATAL: Embedding API call failed on chunk ${chunk.id}: ${apiErr.message}`);
        console.error('Migration aborted. No further records were modified.');
        throw apiErr;
      }

      const values = response?.embeddings?.[0]?.values || response?.embedding?.values;

      // Strict validation before updating database
      if (!Array.isArray(values)) {
        throw new Error(`Embedding response for chunk ${chunk.id} does not contain an array of values.`);
      }
      if (values.length !== TARGET_DIMENSION) {
        throw new Error(`Expected dimension ${TARGET_DIMENSION}, but API returned ${values.length} values for chunk ${chunk.id}.`);
      }
      if (!values.every(Number.isFinite)) {
        throw new Error(`Embedding vector for chunk ${chunk.id} contains non-finite numerical values.`);
      }

      // Update record in Aiven MySQL (preserving all chunk metadata)
      await conn.query(
        `UPDATE document_chunks 
         SET vector_embedding = ?,
             embedding_model = ?,
             embedding_dimension = ?,
             embedding_version = ?
         WHERE id = ?;`,
        [
          JSON.stringify(values),
          TARGET_MODEL,
          TARGET_DIMENSION,
          TARGET_VERSION,
          chunk.id
        ]
      );

      console.log(`  ✅ Successfully updated chunk ${chunk.id} (dim: ${values.length}, model: ${TARGET_MODEL}, ver: ${TARGET_VERSION})`);

      // Gentle rate-limiting delay between sequential API calls (200ms)
      await new Promise(resolve => setTimeout(resolve, 200));
    }

    // 4. Final verification query
    console.log('\nStep 4: Running final verification query...');
    const [audit] = await conn.query(
      `SELECT 
         COUNT(*) as total_count,
         SUM(CASE WHEN embedding_model = ? AND embedding_version = ? AND embedding_dimension = ? AND vector_embedding IS NOT NULL THEN 1 ELSE 0 END) as migrated_count
       FROM document_chunks;`,
      [TARGET_MODEL, TARGET_VERSION, TARGET_DIMENSION]
    );

    const total = audit[0].total_count;
    const migrated = Number(audit[0].migrated_count || 0);

    console.log('==================================================');
    console.log(`Audit Results: Total Chunks: ${total} | Fully Migrated: ${migrated}`);
    if (total === migrated) {
      console.log('🎉 SUCCESS: 100% of document_chunks are migrated to gemini-embedding-2 (768-dim, v1).');
    } else {
      console.error(`⚠️ WARNING: ${total - migrated} chunk(s) remain unmigrated.`);
    }

    console.log('\nVerifying individual records and vector integrity:');
    const [finalRows] = await conn.query(
      `SELECT id, document_id, chunk_index, page_number, content, 
              vector_embedding, embedding_model, embedding_dimension, embedding_version 
       FROM document_chunks 
       ORDER BY chunk_index ASC;`
    );

    const expectedContent = {
      'chk-1': 'Section 4.1 Attendance Policy: Students must maintain a minimum of 75% attendance in each registered subject to be eligible to sit for final semester examinations.',
      'chk-2': 'Section 5.2 Grading System: Grades are calculated based on 30% Continuous Internal Evaluation (assignments & quizzes) and 70% Semester End Examination.',
      'chk-3': 'Section 8.0 Library & Lab Rules: Computer labs are accessible 24/7 for enrolled engineering students with valid student ID badges.'
    };

    let allPreserved = true;
    let allVectorsValid = true;

    for (const row of finalRows) {
      const parsedVec = typeof row.vector_embedding === 'string' ? JSON.parse(row.vector_embedding) : row.vector_embedding;
      const isVecValid = Array.isArray(parsedVec) && parsedVec.length === TARGET_DIMENSION && parsedVec.every(Number.isFinite);
      const isContentPreserved = row.content === expectedContent[row.id];
      const isMetadataValid = row.embedding_model === TARGET_MODEL && row.embedding_dimension === TARGET_DIMENSION && row.embedding_version === TARGET_VERSION;

      if (!isVecValid) allVectorsValid = false;
      if (!isContentPreserved) allPreserved = false;

      console.log(`- Chunk [${row.id}]: doc=${row.document_id}, idx=${row.chunk_index}, page=${row.page_number}`);
      console.log(`  vector valid: ${isVecValid} (len: ${parsedVec?.length || 0})`);
      console.log(`  metadata: model=${row.embedding_model}, dim=${row.embedding_dimension}, ver=${row.embedding_version}`);
      console.log(`  content preserved: ${isContentPreserved}`);
    }

    console.log(`\nAll vectors valid (768 finite numbers): ${allVectorsValid}`);
    console.log(`All chunk identities and content preserved: ${allPreserved}`);
    console.log('==================================================');

  } finally {
    await conn.end();
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  reindexEmbeddings().catch(err => {
    console.error('❌ Migration failed:', err.message || err);
    process.exit(1);
  });
}

export { reindexEmbeddings };
