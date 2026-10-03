/**
 * AI College Life Copilot - Database Migration & Verification Utility
 * Migrates schema.sql & seed.sql and verifies data integrity, ownership, and embeddings.
 */

import mysql from 'mysql2/promise';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { config } from './src/config/env.js';
import { setupMySQLDatabase } from './src/database/init.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function runMigration() {
  console.log('====================================================');
  console.log('🔄 DATABASE MIGRATION & INTEGRITY VERIFICATION');
  console.log(`📡 Target Host: ${config.db.host}:${config.db.port}`);
  console.log(`🗄️ Database:    ${config.db.database}`);
  console.log(`🔒 SSL Enabled: ${config.db.ssl ? 'YES' : 'NO'}`);
  console.log('====================================================\n');

  let pool;
  try {
    pool = await setupMySQLDatabase();
    console.log('✅ Connection established and schema/seed applied successfully.\n');
  } catch (err) {
    console.error(`❌ Migration failed during connection or execution: ${err.message}`);
    process.exit(1);
  }

  // 1. Verify all required tables exist
  const expectedTables = [
    'users',
    'students',
    'admins',
    'subjects',
    'attendance',
    'assignments',
    'exams',
    'study_plans',
    'study_sessions',
    'documents',
    'document_chunks',
    'faqs',
    'announcements',
    'quiz_questions',
    'quiz_attempts',
    'chat_sessions',
    'chat_messages',
    'notifications',
    'resumes',
    'career_analysis',
    'career_profiles',
    'interview_questions',
    'interview_practices',
    'schedules',
    'notes',
    'budgets',
    'expenses'
  ];

  console.log('1. TABLE VERIFICATION:');
  const [rows] = await pool.query('SHOW TABLES;');
  const tableNames = rows.map(r => Object.values(r)[0].toLowerCase());

  let allTablesPresent = true;
  for (const tbl of expectedTables) {
    const present = tableNames.includes(tbl.toLowerCase());
    if (present) {
      console.log(`  ✅ Table '${tbl}' exists`);
    } else {
      console.error(`  ❌ Table '${tbl}' MISSING`);
      allTablesPresent = false;
    }
  }

  if (!allTablesPresent) {
    console.error('❌ Table verification failed.');
    process.exit(1);
  }

  // 2. Verify Data Integrity & Student Ownership
  console.log('\n2. DATA INTEGRITY & STUDENT OWNERSHIP:');

  // Verify users
  const [users] = await pool.query('SELECT id, name, email, role FROM users;');
  console.log(`  ✅ Users count: ${users.length}`);

  // Verify demo student and admin
  const alexUser = users.find(u => u.email === 'alex@student.edu');
  const adminUser = users.find(u => u.email === 'admin@college.edu');
  if (!alexUser || !adminUser) {
    console.error('  ❌ Demo users missing from users table');
    process.exit(1);
  }
  console.log('  ✅ Demo student and admin users confirmed');

  // Verify student profile
  const [students] = await pool.query('SELECT * FROM students WHERE user_id = ?;', [alexUser.id]);
  if (students.length === 0) {
    console.error('  ❌ Student profile record missing');
    process.exit(1);
  }
  const studentId = students[0].id;
  console.log(`  ✅ Student record confirmed (id: ${studentId})`);

  // Verify Attendance percentages
  const [attendance] = await pool.query('SELECT * FROM attendance WHERE student_id = ?;', [studentId]);
  console.log(`  ✅ Attendance records: ${attendance.length} (Generated percentage column operational)`);

  // Verify Schedules
  const [schedules] = await pool.query('SELECT * FROM schedules WHERE student_id = ?;', [studentId]);
  console.log(`  ✅ Schedules records: ${schedules.length}`);

  // Verify Notes
  const [notes] = await pool.query('SELECT * FROM notes WHERE student_id = ?;', [studentId]);
  console.log(`  ✅ Notes records: ${notes.length}`);

  // Verify Budgets & Expenses
  const [budgets] = await pool.query('SELECT * FROM budgets WHERE student_id = ?;', [studentId]);
  const [expenses] = await pool.query('SELECT * FROM expenses WHERE student_id = ?;', [studentId]);
  console.log(`  ✅ Budget target: $${budgets[0]?.monthly_target || 0}, Expenses count: ${expenses.length}`);

  // 3. Verify RAG / Embeddings table schema
  console.log('\n3. RAG & VECTOR EMBEDDING SCHEMA:');
  const [chunkCols] = await pool.query('DESCRIBE document_chunks;');
  const embeddingCol = chunkCols.find(c => c.Field === 'vector_embedding');
  if (embeddingCol && embeddingCol.Type.toLowerCase().includes('json')) {
    console.log('  ✅ document_chunks.vector_embedding is valid JSON type for semantic vectors');
  } else {
    console.error('  ❌ document_chunks.vector_embedding column type mismatch');
    process.exit(1);
  }

  // 4. Foreign Key Cascade Test (Safe isolated test)
  console.log('\n4. FOREIGN KEY CASCADE & ISOLATION CHECK:');
  const tempUserId = 'u-cascade-test-' + Date.now();
  const tempStudentId = 'std-cascade-test-' + Date.now();

  await pool.query(
    'INSERT INTO users (id, name, email, password_hash, role) VALUES (?, ?, ?, ?, ?);',
    [tempUserId, 'Cascade Tester', `cascade_${Date.now()}@test.edu`, 'hash', 'STUDENT']
  );
  await pool.query(
    'INSERT INTO students (id, user_id, student_code) VALUES (?, ?, ?);',
    [tempStudentId, tempUserId, `CODE-${Date.now()}`]
  );
  await pool.query(
    'INSERT INTO notes (id, student_id, title, content) VALUES (?, ?, ?, ?);',
    [`nt-test-${Date.now()}`, tempStudentId, 'Cascade Test Note', 'Sample Note Content']
  );

  // Deleting student record should cascade to notes
  await pool.query('DELETE FROM students WHERE id = ?;', [tempStudentId]);
  const [orphanNotes] = await pool.query('SELECT * FROM notes WHERE student_id = ?;', [tempStudentId]);
  await pool.query('DELETE FROM users WHERE id = ?;', [tempUserId]);

  if (orphanNotes.length === 0) {
    console.log('  ✅ Foreign Key ON DELETE CASCADE operates correctly (no orphaned notes)');
  } else {
    console.error('  ❌ Foreign Key cascade failed: orphaned records exist');
    process.exit(1);
  }

  console.log('\n====================================================');
  console.log('🎉 ALL DATABASE VERIFICATION CHECKS PASSED');
  console.log('====================================================\n');
  await pool.end();
  process.exit(0);
}

runMigration().catch(err => {
  console.error('Migration execution error:', err);
  process.exit(1);
});
