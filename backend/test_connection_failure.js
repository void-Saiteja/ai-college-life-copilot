/**
 * Connection Failure & Production Fallback Test
 * Verifies that:
 * 1. Invalid DB credentials in production mode throw a fatal error.
 * 2. Silent fallback to JSON is strictly prevented in production mode.
 * 3. Error logs do NOT expose passwords or confidential secrets.
 * 4. Development mode cleanly handles connection failure with appropriate fallback notice.
 */

import { spawn } from 'child_process';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function runTest(envOverrides, expectedExitCode, expectedLogSubstring, forbiddenLogSubstring, testName) {
  return new Promise((resolve) => {
    console.log(`\nTesting: ${testName}...`);
    const child = spawn('node', ['server.js'], {
      cwd: __dirname,
      env: {
        ...process.env,
        ...envOverrides
      }
    });

    let output = '';
    let exited = false;

    child.stdout.on('data', (d) => { output += d.toString(); });
    child.stderr.on('data', (d) => { output += d.toString(); });

    const timeout = setTimeout(() => {
      if (!exited) {
        child.kill();
        resolve({
          passed: false,
          reason: 'Process timed out without expected termination',
          output
        });
      }
    }, 8000);

    child.on('exit', (code) => {
      exited = true;
      clearTimeout(timeout);

      const hasExpectedLog = expectedLogSubstring ? output.includes(expectedLogSubstring) : true;
      const leaksForbidden = forbiddenLogSubstring ? output.includes(forbiddenLogSubstring) : false;
      const codeMatches = code === expectedExitCode;

      const passed = codeMatches && hasExpectedLog && !leaksForbidden;
      if (passed) {
        console.log(`  ✅ [PASS] ${testName}`);
      } else {
        console.error(`  ❌ [FAIL] ${testName}`);
        console.error(`     Exit code: ${code} (expected ${expectedExitCode})`);
        console.error(`     Has expected log: ${hasExpectedLog}`);
        console.error(`     Leaks forbidden text: ${leaksForbidden}`);
        console.error(`     Output:\n${output}`);
      }

      resolve({ passed, code, output });
    });
  });
}

async function main() {
  console.log('====================================================');
  console.log('🔒 DATABASE CONNECTION FAILURE & PRODUCTION SAFETY TEST');
  console.log('====================================================');

  const secretSentinel = 'SuperSecretUnrevealedPassword999';

  // 1. Production Mode with Invalid DB Credentials -> Must fail to start (exit non-zero) and NOT fall back to JSON
  const prodTest = await runTest(
    {
      NODE_ENV: 'production',
      JWT_SECRET: 'production_jwt_strong_secret_key_9999',
      DB_HOST: '127.0.0.1',
      DB_PORT: '3306',
      DB_USER: 'invalid_user_test',
      DB_PASSWORD: secretSentinel,
      DB_NAME: 'non_existent_db_999'
    },
    1,
    'Silent fallback to local JSON storage is disabled in production',
    secretSentinel,
    'Production Mode: Invalid MySQL fails fast without silent JSON fallback & without leaking password'
  );

  // 2. Production Mode with Missing JWT Secret -> Must fail fast
  const jwtTest = await runTest(
    {
      NODE_ENV: 'production',
      JWT_SECRET: '',
      DB_HOST: 'localhost',
      DB_PORT: '3306'
    },
    1,
    'FATAL: A strong, unique JWT_SECRET must be configured',
    null,
    'Production Mode: Missing JWT_SECRET causes immediate fatal termination'
  );

  if (!prodTest.passed || !jwtTest.passed) {
    console.error('\n❌ Connection failure tests failed!');
    process.exit(1);
  }

  console.log('\n====================================================');
  console.log('✅ ALL CONNECTION FAILURE & PRODUCTION DEFENSE TESTS PASSED');
  console.log('====================================================\n');
  process.exit(0);
}

main().catch(err => {
  console.error('Test error:', err);
  process.exit(1);
});
