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

async function runTests() {
  console.log('=== STARTING BACKEND API AUDIT SUITE ===');

  // 1. Health API
  const health = await request({ hostname: 'localhost', port: 5000, path: '/api/health', method: 'GET' });
  console.log('1. GET /api/health ->', health.status, health.data?.message);

  // 2. Student Login
  const studentLogin = await request({ hostname: 'localhost', port: 5000, path: '/api/auth/login', method: 'POST', headers: { 'Content-Type': 'application/json' } }, { email: 'alex@student.edu', password: 'Student@123' });
  console.log('2. POST /api/auth/login (Student) ->', studentLogin.status, 'Token acquired:', !!studentLogin.data?.token);
  const studentToken = studentLogin.data?.token;

  // 3. Admin Login
  const adminLogin = await request({ hostname: 'localhost', port: 5000, path: '/api/auth/login', method: 'POST', headers: { 'Content-Type': 'application/json' } }, { email: 'admin@college.edu', password: 'Admin@123' });
  console.log('3. POST /api/auth/login (Admin) ->', adminLogin.status, 'Role:', adminLogin.data?.user?.role);
  const adminToken = adminLogin.data?.token;

  const authHeaders = { Authorization: `Bearer ${studentToken}` };
  const adminHeaders = { Authorization: `Bearer ${adminToken}` };

  // 4. GET /api/auth/me
  const me = await request({ hostname: 'localhost', port: 5000, path: '/api/auth/me', method: 'GET', headers: authHeaders });
  console.log('4. GET /api/auth/me ->', me.status, 'User:', me.data?.user?.name);

  // 5. GET /api/student/dashboard
  const dash = await request({ hostname: 'localhost', port: 5000, path: '/api/student/dashboard', method: 'GET', headers: authHeaders });
  console.log('5. GET /api/student/dashboard ->', dash.status, 'Subjects:', dash.data?.data?.subjects?.length);

  // 6. GET /api/attendance
  const att = await request({ hostname: 'localhost', port: 5000, path: '/api/attendance', method: 'GET', headers: authHeaders });
  console.log('6. GET /api/attendance ->', att.status, 'Records:', att.data?.data?.length);

  // 7. POST /api/attendance/projection
  const proj = await request({ hostname: 'localhost', port: 5000, path: '/api/attendance/projection', method: 'POST', headers: { 'Content-Type': 'application/json' } }, { conducted: 28, attended: 25, missClasses: 2, attendClasses: 3 });
  console.log('7. POST /api/attendance/projection ->', proj.status, 'Projected Pct:', proj.data?.data?.ifMissNext?.projectedPercentage + '%');

  // 8. GET /api/assignments
  const asg = await request({ hostname: 'localhost', port: 5000, path: '/api/assignments', method: 'GET', headers: authHeaders });
  console.log('8. GET /api/assignments ->', asg.status, 'Assignments:', asg.data?.data?.length);

  // 9. GET /api/exams
  const exm = await request({ hostname: 'localhost', port: 5000, path: '/api/exams', method: 'GET', headers: authHeaders });
  console.log('9. GET /api/exams ->', exm.status, 'Exams:', exm.data?.data?.length);

  // 10. GET /api/planner
  const plan = await request({ hostname: 'localhost', port: 5000, path: '/api/planner', method: 'GET', headers: authHeaders });
  console.log('10. GET /api/planner ->', plan.status, 'Plan:', plan.data?.data?.plan?.title);

  // 11. POST /api/chat/chat
  const chat = await request({ hostname: 'localhost', port: 5000, path: '/api/chat/chat', method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeaders } }, { message: 'How is my attendance?' });
  console.log('11. POST /api/chat/chat ->', chat.status, 'Reply Snippet:', chat.data?.data?.assistantMessage?.content?.substring(0, 45) + '...');

  // 12. POST /api/documents/query
  const rag = await request({ hostname: 'localhost', port: 5000, path: '/api/documents/query', method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeaders } }, { query: 'attendance policy requirement' });
  console.log('12. POST /api/documents/query ->', rag.status, 'Found:', rag.data?.data?.found, 'Sources:', rag.data?.data?.sources?.length);

  // 13. POST /api/quiz/generate
  const quiz = await request({ hostname: 'localhost', port: 5000, path: '/api/quiz/generate', method: 'POST', headers: { 'Content-Type': 'application/json', ...authHeaders } }, { subject: 'CS 201', topic: 'Data Structures' });
  console.log('13. POST /api/quiz/generate ->', quiz.status, 'Total Questions:', quiz.data?.data?.totalQuestions);

  // 14. GET /api/admin/stats
  const adminStats = await request({ hostname: 'localhost', port: 5000, path: '/api/admin/stats', method: 'GET', headers: adminHeaders });
  console.log('14. GET /api/admin/stats (Admin) ->', adminStats.status, 'Students:', adminStats.data?.data?.totalStudents);

  // 15. Role authorization test (Student requesting admin endpoint)
  const forbiddenTest = await request({ hostname: 'localhost', port: 5000, path: '/api/admin/stats', method: 'GET', headers: authHeaders });
  console.log('15. GET /api/admin/stats (Student role) ->', forbiddenTest.status, 'Expected 403 Forbidden:', forbiddenTest.data?.error);

  console.log('=== AUDIT TEST SUITE COMPLETE ===');
}

runTests().catch(console.error);
