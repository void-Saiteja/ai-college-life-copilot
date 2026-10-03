import http from 'http';

function request(options, body = null) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(data) });
        } catch (e) {
          resolve({ status: res.statusCode, raw: data });
        }
      });
    });
    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

async function runAuthAudit() {
  console.log('=== STARTING AUTHENTICATION & AUTHORIZATION AUDIT SUITE ===\n');

  const timestamp = Date.now();
  const testStudentEmail = `teststudent_${timestamp}@college.edu`;

  // 1. Student Registration
  console.log('Test 1: Student Registration (POST /api/auth/register)');
  const regRes = await request({
    hostname: 'localhost', port: 5000, path: '/api/auth/register', method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, {
    name: 'Audit Test Student',
    email: testStudentEmail,
    password: 'Password@123',
    role: 'STUDENT',
    department: 'Computer Science',
    semester: 4
  });

  const regHasPassword = regRes.data?.user ? ('password' in regRes.data.user || 'password_hash' in regRes.data.user) : false;
  console.log(`  -> Status: ${regRes.status}, Message: ${regRes.data?.message}, Token Received: ${!!regRes.data?.token}, Password Leak: ${regHasPassword}`);

  // 2. Student Login
  console.log('\nTest 2: Student Login (POST /api/auth/login)');
  const studentLoginRes = await request({
    hostname: 'localhost', port: 5000, path: '/api/auth/login', method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, {
    email: testStudentEmail,
    password: 'Password@123'
  });

  const studentToken = studentLoginRes.data?.token;
  const loginHasPassword = studentLoginRes.data?.user ? ('password' in studentLoginRes.data.user || 'password_hash' in studentLoginRes.data.user) : false;
  console.log(`  -> Status: ${studentLoginRes.status}, Role: ${studentLoginRes.data?.user?.role}, Password Leak: ${loginHasPassword}`);

  // 3. Admin Login
  console.log('\nTest 3: Admin Login (POST /api/auth/login)');
  const adminLoginRes = await request({
    hostname: 'localhost', port: 5000, path: '/api/auth/login', method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, {
    email: 'admin@college.edu',
    password: 'Admin@123'
  });

  const adminToken = adminLoginRes.data?.token;
  console.log(`  -> Status: ${adminLoginRes.status}, Role: ${adminLoginRes.data?.user?.role}, Token Received: ${!!adminToken}`);

  // 4 & 5. JWT Creation & Verification
  console.log('\nTest 4 & 5: JWT Creation & Verification');
  console.log(`  -> Student Token format valid (3 parts): ${studentToken?.split('.').length === 3}`);
  console.log(`  -> Admin Token format valid (3 parts): ${adminToken?.split('.').length === 3}`);

  // 6. GET /api/auth/me
  console.log('\nTest 6: GET /api/auth/me (Authenticated user profile)');
  const meRes = await request({
    hostname: 'localhost', port: 5000, path: '/api/auth/me', method: 'GET',
    headers: { Authorization: `Bearer ${studentToken}` }
  });

  const meHasPassword = meRes.data?.user ? ('password' in meRes.data.user || 'password_hash' in meRes.data.user) : false;
  console.log(`  -> Status: ${meRes.status}, User: ${meRes.data?.user?.name}, Password Leak: ${meHasPassword}`);

  // 7. Invalid Password Test
  console.log('\nTest 7: Invalid Password Test');
  const invalidPassRes = await request({
    hostname: 'localhost', port: 5000, path: '/api/auth/login', method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, {
    email: testStudentEmail,
    password: 'WrongPassword999!'
  });
  console.log(`  -> Status: ${invalidPassRes.status}, Error Message: ${invalidPassRes.data?.error}`);

  // 8. Invalid Token Test
  console.log('\nTest 8: Invalid Token Test');
  const invalidTokenRes = await request({
    hostname: 'localhost', port: 5000, path: '/api/auth/me', method: 'GET',
    headers: { Authorization: 'Bearer invalid_garbage_token_12345' }
  });
  console.log(`  -> Status: ${invalidTokenRes.status}, Error Message: ${invalidTokenRes.data?.error}`);

  // 9. Student Accessing Admin APIs (Role Guard Check)
  console.log('\nTest 9: Student Accessing Admin API (GET /api/admin/stats)');
  const studentAdminAccessRes = await request({
    hostname: 'localhost', port: 5000, path: '/api/admin/stats', method: 'GET',
    headers: { Authorization: `Bearer ${studentToken}` }
  });
  console.log(`  -> Status: ${studentAdminAccessRes.status}, Expected 403 Forbidden: ${studentAdminAccessRes.data?.error}`);

  // 10. Admin Accessing Protected Admin APIs
  console.log('\nTest 10: Admin Accessing Admin API (GET /api/admin/stats)');
  const adminAccessRes = await request({
    hostname: 'localhost', port: 5000, path: '/api/admin/stats', method: 'GET',
    headers: { Authorization: `Bearer ${adminToken}` }
  });
  console.log(`  -> Status: ${adminAccessRes.status}, Total Students Reported: ${adminAccessRes.data?.data?.totalStudents}`);

  console.log('\n=== AUTHENTICATION AUDIT COMPLETED ===');
}

runAuthAudit().catch(console.error);
