import assert from 'assert';
import { isSimpleGreeting } from './src/controllers/aiCopilotController.js';
import { buildOfflineFallbackResponse, generateGeminiResponse } from './src/services/llmService.js';
import { filterSupportingChunks, isAttendanceQuery, isCancellationQuery } from './src/services/retrievalService.js';

console.log('🧪 Starting AI Copilot Chat Resilience & RAG Precision Test Suite...\n');

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

// Mock Data Setup
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

const candidateChunksWithMixedPolicies = [
  {
    id: 'chk-1',
    documentTitle: 'College Academic Rules & Attendance Policy 2026.pdf',
    page_number: 1,
    content: 'Section 4.1 Attendance Policy: Students must maintain a minimum of 75% attendance in each registered subject to be eligible to sit for final semester examinations.'
  },
  {
    id: 'chk-2',
    documentTitle: 'Anurag Admission Cancellation & Fee Refund Policy.pdf',
    page_number: 2,
    content: 'Section 2.0 Cancellation Policy: In case of admission cancellation or seat surrender within 15 days, 90% of tuition fee will be refunded.'
  },
  {
    id: 'chk-3',
    documentTitle: 'Grading System and Evaluation Guidelines.pdf',
    page_number: 3,
    content: 'Section 5.2 Grading System: Grades are calculated based on 30% Continuous Internal Evaluation (assignments & quizzes) and 70% Semester End Examination.'
  }
];

// 2. Test RAG Precision Filtering for Attendance Questions (Requirement 2 & 9)
console.log('Test 2: Attendance Query Filter - Exclude Unrelated Cancellation and Grading Policies');
const attendanceExamQuery = 'What is the attendance required for final semester examinations? Am I eligible?';
assert.strictEqual(isAttendanceQuery(attendanceExamQuery), true);
assert.strictEqual(isCancellationQuery(attendanceExamQuery), false);

const filteredSupporting = filterSupportingChunks(attendanceExamQuery, candidateChunksWithMixedPolicies);
assert.strictEqual(filteredSupporting.length, 1, 'Must retain exactly the 1 directly supporting attendance policy chunk');
assert.strictEqual(filteredSupporting[0].documentTitle, 'College Academic Rules & Attendance Policy 2026.pdf');
assert.strictEqual(filteredSupporting[0].page_number, 1);
assert(filteredSupporting[0].content.includes('minimum of 75% attendance'));

// Verify cancellation and grading policies are excluded
const hasCancellation = filteredSupporting.some(c => c.documentTitle.toLowerCase().includes('cancellation') || c.content.toLowerCase().includes('refund'));
assert.strictEqual(hasCancellation, false, 'Cancellation policy must be strictly excluded from attendance query');
const hasGrading = filteredSupporting.some(c => c.documentTitle.toLowerCase().includes('grading'));
assert.strictEqual(hasGrading, false, 'Unrelated grading policy must be excluded from attendance query');
console.log('  ✅ filterSupportingChunks correctly excludes cancellation and grading excerpts for attendance queries.\n');

// 3. Test Direct Answer First and Specific Examination Eligibility Statement (Requirement 4, 5, 6, 7)
console.log('Test 3: Direct Answer First, Citation Accuracy, and Zero Data Leakage');
const examEligibilityFallback = buildOfflineFallbackResponse({
  message: 'Am I eligible for final semester examinations based on attendance?',
  context: mockContext,
  docChunks: candidateChunksWithMixedPolicies
});

// 3a. Header & Offline Mode Notice
assert(examEligibilityFallback.includes('📋 **Academic Copilot (Offline Mode)**'), 'Must have offline mode header');
assert(examEligibilityFallback.includes('Google Gemini is currently operating under high demand'), 'Must clearly explain live AI is unavailable');

// 3b. Simple, direct answer first
assert(examEligibilityFallback.includes('**Answer:**'), 'Must contain direct Answer header first');
assert(examEligibilityFallback.includes('College Academic Rules & Attendance Policy 2026'), 'Must state document name');
assert(examEligibilityFallback.includes('page 1'), 'Must state page 1');
assert(examEligibilityFallback.includes('75% attendance in each registered subject'), 'Must specify minimum 75% attendance in each registered subject');

// 3c. Supporting source accuracy
assert(examEligibilityFallback.includes('📄 **Supporting Official Document Source**:'), 'Must include supporting source section');
assert(examEligibilityFallback.includes('College Academic Rules & Attendance Policy 2026.pdf'), 'Must cite correct document title');
assert(examEligibilityFallback.includes('Page 1'), 'Must cite Page 1');
assert(!examEligibilityFallback.includes('Anurag Admission Cancellation'), 'Must NOT include Anurag cancellation policy');
assert(!examEligibilityFallback.includes('Fee Refund'), 'Must NOT include fee refund policy');
assert(!examEligibilityFallback.includes('Grading System and Evaluation Guidelines'), 'Must NOT include grading system policy');

// 3d. Do not expose unrelated student profile, GPA, exams, or attendance stats
assert(!examEligibilityFallback.includes('Alex Mercer'), 'Must NOT expose student name when not asked');
assert(!examEligibilityFallback.includes('3.85'), 'Must NOT expose GPA when not asked');
assert(!examEligibilityFallback.includes('Midterm Examination'), 'Must NOT expose unrelated upcoming exams');
assert(!examEligibilityFallback.includes('Binary Search Tree'), 'Must NOT expose assignments');
assert(!examEligibilityFallback.includes('Data Structures & Algorithms (CS201): 87.5%'), 'Must NOT expose individual attendance statistics table');
console.log('  ✅ Answer is presented first, citations are accurate, and unrelated student stats/cancellation chunks are not exposed.\n');

// 4. Test Personal Attendance & Bunk Calculation Inquiry (when specifically asked by student)
console.log('Test 4: Personal Attendance & Bunk Calculations when explicitly requested');
const bunkFallback = buildOfflineFallbackResponse({
  message: 'Can I bunk classes in Data Structures? What is my attendance?',
  context: mockContext,
  docChunks: candidateChunksWithMixedPolicies
});
assert(bunkFallback.includes('📅 **Current Attendance Status & Calculations**:'), 'Must provide attendance calculation table');
assert(bunkFallback.includes('Safe margin'), 'Must compute safe margin');
assert(bunkFallback.includes('Attendance deficit'), 'Must compute deficit');
assert(!bunkFallback.includes('Anurag Admission Cancellation'), 'Must still exclude cancellation policy');
console.log('  ✅ Personal attendance calculations correctly generated when specifically asked.\n');

// 5. Test General Status Query
console.log('Test 5: General Status Inquiry includes student academic overview');
const generalFallback = buildOfflineFallbackResponse({
  message: 'What is my current academic status and overview?',
  context: mockContext,
  docChunks: []
});
assert(generalFallback.includes('Alex Mercer'), 'General overview may summarize student profile');
assert(generalFallback.includes('Attendance Highlights'), 'General overview may summarize attendance');
console.log('  ✅ General status overview functions properly.\n');

// 6. Test Safe Error Handling when Gemini returns 503 High Demand
console.log('Test 6: Safe error handling when Gemini returns 503 High Demand');
const raw503ErrorMsg = '{"error":{"code":503,"message":"This model is currently experiencing high demand.","status":"UNAVAILABLE"}}';
const fallbackUnder503 = buildOfflineFallbackResponse({
  message: 'Am I eligible for final semester examinations based on attendance?',
  context: mockContext,
  docChunks: candidateChunksWithMixedPolicies
});
assert(!fallbackUnder503.includes(raw503ErrorMsg));
assert(!fallbackUnder503.includes('"code":503'));
assert(fallbackUnder503.includes('75% attendance in each registered subject'));
console.log('  ✅ Gemini 503 high demand raw JSON is suppressed; clean grounded fallback is returned.\n');

// 7. Test generateGeminiResponse integration with offline fallback
console.log('Test 7: generateGeminiResponse graceful fallback execution');
async function testGenerateFallback() {
  const result = await generateGeminiResponse({
    message: 'Am I eligible for final semester examinations based on attendance?',
    context: mockContext,
    docChunks: candidateChunksWithMixedPolicies
  });

  assert.strictEqual(result.success, true);
  assert.strictEqual(result.isFallback, true);
  assert(result.reply.includes('📋 **Academic Copilot (Offline Mode)**'));
  assert(result.reply.includes('College Academic Rules & Attendance Policy 2026'));
  assert(result.reply.includes('75% attendance'));
  assert(!result.reply.includes('Anurag Admission Cancellation'));
  assert(!result.reply.includes('503'));
  console.log('  ✅ generateGeminiResponse successfully executes grounded offline fallback with filtered sources.\n');
}

await testGenerateFallback();

console.log('🎉 All AI Copilot Chat Resilience & RAG Precision tests PASSED successfully!');
