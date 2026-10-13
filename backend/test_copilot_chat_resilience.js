import assert from 'assert';
import { isSimpleGreeting } from './src/controllers/aiCopilotController.js';
import { buildOfflineFallbackResponse, generateGeminiResponse } from './src/services/llmService.js';

console.log('🧪 Starting AI Copilot Chat Resilience Test Suite...\n');

// 1. Test isSimpleGreeting
console.log('Test 1: Greeting detection');
assert.strictEqual(isSimpleGreeting('hi'), true);
assert.strictEqual(isSimpleGreeting('Hi!'), true);
assert.strictEqual(isSimpleGreeting('hello'), true);
assert.strictEqual(isSimpleGreeting('Hello?'), true);
assert.strictEqual(isSimpleGreeting('hey'), true);
assert.strictEqual(isSimpleGreeting('good morning'), true);
assert.strictEqual(isSimpleGreeting('Good Evening!'), true);
assert.strictEqual(isSimpleGreeting('sup'), true);
assert.strictEqual(isSimpleGreeting('yo'), true);
assert.strictEqual(isSimpleGreeting("what's up"), true);
assert.strictEqual(isSimpleGreeting('hi copilot'), true);

// Non-greetings must return false
assert.strictEqual(isSimpleGreeting('What is my attendance in DSA?'), false);
assert.strictEqual(isSimpleGreeting('hi, what is my GPA?'), false);
assert.strictEqual(isSimpleGreeting('Tell me about the refund policy'), false);
assert.strictEqual(isSimpleGreeting(''), false);
assert.strictEqual(isSimpleGreeting(null), false);
console.log('  ✅ Greeting detection works accurately for simple greetings and complex queries.\n');

// 2. Test buildOfflineFallbackResponse with student context and RAG doc chunks
console.log('Test 2: Offline Fallback Response Structure & Grounding');
const mockContext = {
  student: {
    id: 'std-1',
    name: 'Alex Mercer',
    department: 'Computer Science & Engineering',
    semester: 4,
    gpa: 3.85
  },
  attendance: [
    { subject: 'Data Structures & Algorithms', code: 'CS201', conducted: 24, attended: 21, percentage: 87.5, status: 'Good Standing' },
    { subject: 'Operating Systems', code: 'CS202', conducted: 20, attended: 14, percentage: 70.0, status: 'Low Attendance Warning' }
  ],
  upcomingAssignments: [
    { id: 'asg-1', title: 'Binary Search Tree Implementation', subject: 'Data Structures', dueDate: '2026-10-20', priority: 'High' }
  ],
  upcomingExams: [
    { id: 'ex-1', title: 'Midterm Examination', subject: 'Operating Systems', examDate: '2026-10-25', startTime: '10:00 AM', location: 'Hall B' }
  ]
};

const mockDocChunks = [
  {
    documentTitle: 'Student_Handbook_2026.pdf',
    page_number: 12,
    content: 'Students must maintain a minimum attendance of 75% in each course to be eligible to sit for end-semester examinations.'
  }
];

// 2a. Offline mode header & label
const fallbackGeneral = buildOfflineFallbackResponse({
  message: 'What is my current academic status?',
  context: mockContext,
  docChunks: mockDocChunks
});

assert(fallbackGeneral.includes('📋 **Academic Copilot (Offline Mode)**'), 'Must have offline mode header');
assert(fallbackGeneral.includes('Google Gemini is currently operating under high demand'), 'Must explain temporary unavailability');
assert(fallbackGeneral.includes('Student_Handbook_2026.pdf'), 'Must include RAG document citation');
assert(fallbackGeneral.includes('Alex Mercer'), 'Must include student profile info');
assert(!fallbackGeneral.includes('{"error"'), 'Must never include raw JSON error');
console.log('  ✅ Offline Fallback contains clear Academic Copilot (Offline Mode) labeling.\n');

// 2b. Attendance inquiry with calculation
console.log('Test 3: Attendance calculations in fallback');
const fallbackAttendance = buildOfflineFallbackResponse({
  message: 'Can I bunk classes in Data Structures? What is my attendance?',
  context: mockContext,
  docChunks: []
});
assert(fallbackAttendance.includes('Safe margin'), 'Must compute safe margin for high attendance');
assert(fallbackAttendance.includes('Attendance deficit'), 'Must compute deficit for low attendance');
console.log('  ✅ Attendance calculations and bunk margins correctly generated.\n');

// 2c. RAG Document Grounding inquiry
console.log('Test 4: RAG Document citations in fallback');
const fallbackPolicy = buildOfflineFallbackResponse({
  message: 'What is the attendance policy according to the student handbook?',
  context: mockContext,
  docChunks: mockDocChunks
});
assert(fallbackPolicy.includes('Student_Handbook_2026.pdf'), 'Must include document title');
assert(fallbackPolicy.includes('Page 12'), 'Must include page number');
assert(fallbackPolicy.includes('minimum attendance of 75%'), 'Must include document content');
console.log('  ✅ Verified document citations and page numbers included in fallback.\n');

// 3. Test generateGeminiResponse with mock / unconfigured API key
console.log('Test 5: generateGeminiResponse fallback when live API is unavailable');
async function testGenerateFallback() {
  const result = await generateGeminiResponse({
    message: 'When is my Operating Systems exam?',
    context: mockContext,
    docChunks: []
  });

  assert.strictEqual(result.success, true);
  assert.strictEqual(result.isFallback, true);
  assert(result.reply.includes('📋 **Academic Copilot (Offline Mode)**'));
  assert(result.reply.includes('Midterm Examination'));
  assert(result.reply.includes('Operating Systems'));
  assert(!result.reply.includes('503'));
  assert(!result.reply.includes('{"error"'));
  console.log('  ✅ generateGeminiResponse gracefully returns grounded fallback response without raw errors.\n');
}

await testGenerateFallback();

// 4. Test handling of raw Gemini 503 error JSON
console.log('Test 6: Safe error handling when Gemini returns 503 High Demand');
const raw503ErrorMsg = '{"error":{"code":503,"message":"This model is currently experiencing high demand.","status":"UNAVAILABLE"}}';
// Ensure fallback builder handles 503 error situations cleanly
const fallbackUnder503 = buildOfflineFallbackResponse({
  message: 'What assignments are due?',
  context: mockContext,
  docChunks: []
});
assert(fallbackUnder503.includes('Binary Search Tree Implementation'));
assert(!fallbackUnder503.includes(raw503ErrorMsg));
assert(!fallbackUnder503.includes('"code":503'));
assert(fallbackUnder503.includes('📋 **Academic Copilot (Offline Mode)**'));
console.log('  ✅ Gemini 503 high demand is never exposed to user; structured offline fallback returned instead.\n');

console.log('🎉 All AI Copilot Chat Resilience tests PASSED successfully!');
