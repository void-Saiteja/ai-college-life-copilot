import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DATA_DIR = path.join(__dirname, '../../data');
const DB_FILE = path.join(DATA_DIR, 'db.json');

const initialSeedData = {
  users: [
    {
      id: 'u-admin-1',
      name: 'Dr. Sarah Connor',
      email: 'admin@college.edu',
      password_hash: '$2b$10$yekGYidDnDU16GHsDS2wkeVTZl6LNVdUgCZs3kMPP8RtCdp3pnaO.',
      role: 'ADMIN',
      avatar_url: null,
      created_at: new Date().toISOString()
    },
    {
      id: 'u-student-1',
      name: 'Alex Mercer',
      email: 'alex@student.edu',
      password_hash: '$2b$10$KBH7Vh0A0qhwIr38KfsiQOxK/z3qKtc6A5LHVIlVqfG.fpvbNRGwm',
      role: 'STUDENT',
      avatar_url: null,
      created_at: new Date().toISOString()
    }
  ],
  admins: [
    { id: 'adm-1', user_id: 'u-admin-1', designation: 'Head of Academic Affairs' }
  ],
  students: [
    { id: 'std-1', user_id: 'u-student-1', student_code: 'CS-2026-88', semester: 4, department: 'Computer Science & Engineering', gpa: 3.85 }
  ],
  subjects: [
    { id: 'sub-1', code: 'CS 201', name: 'Data Structures & Algorithms', instructor: 'Dr. Alan Turing', credits: 4, department: 'Computer Science' },
    { id: 'sub-2', code: 'CS 340', name: 'Database System Design', instructor: 'Dr. Grace Hopper', credits: 3, department: 'Computer Science' },
    { id: 'sub-3', code: 'MATH 220', name: 'Calculus III', instructor: 'Prof. Katherine Johnson', credits: 3, department: 'Mathematics' },
    { id: 'sub-4', code: 'CS 410', name: 'Artificial Intelligence', instructor: 'Dr. Marvin Minsky', credits: 4, department: 'Computer Science' }
  ],
  attendance: [
    { id: 'att-1', student_id: 'std-1', subject_id: 'sub-1', classes_conducted: 28, classes_attended: 25 },
    { id: 'att-2', student_id: 'std-1', subject_id: 'sub-2', classes_conducted: 20, classes_attended: 18 },
    { id: 'att-3', student_id: 'std-1', subject_id: 'sub-3', classes_conducted: 24, classes_attended: 22 },
    { id: 'att-4', student_id: 'std-1', subject_id: 'sub-4', classes_conducted: 16, classes_attended: 14 }
  ],
  assignments: [
    {
      id: 'asg-1',
      student_id: 'std-1',
      subject_id: 'sub-1',
      title: 'B-Trees & Red-Black Tree Implementation',
      description: 'Implement a self-balancing Red-Black tree in C++/Java with full rotation logs.',
      dueDate: new Date(Date.now() + 86400000 * 2).toISOString().split('T')[0],
      priority: 'High',
      status: 'In Progress',
      estimatedHours: 5.0
    },
    {
      id: 'asg-2',
      student_id: 'std-1',
      subject_id: 'sub-2',
      title: 'Relational Schema Normalization Project',
      description: 'Normalize 3NF schema to BCNF for campus database.',
      dueDate: new Date(Date.now() + 86400000 * 5).toISOString().split('T')[0],
      priority: 'Medium',
      status: 'Pending',
      estimatedHours: 3.5
    },
    {
      id: 'asg-3',
      student_id: 'std-1',
      subject_id: 'sub-3',
      title: 'Partial Differentiation Problem Set',
      description: 'Solve problems 1-15 in Chapter 4.',
      dueDate: new Date(Date.now() + 86400000 * 1).toISOString().split('T')[0],
      priority: 'High',
      status: 'Pending',
      estimatedHours: 2.0
    }
  ],
  exams: [
    {
      id: 'exm-1',
      student_id: 'std-1',
      subject_id: 'sub-1',
      title: 'DSA Midterm Examination',
      exam_date: new Date(Date.now() + 86400000 * 7).toISOString().split('T')[0],
      start_time: '09:00 AM',
      location: 'Hall A2',
      syllabus: 'Arrays, Linked Lists, Trees, Graphs, Sorting Algorithms'
    },
    {
      id: 'exm-2',
      student_id: 'std-1',
      subject_id: 'sub-2',
      title: 'DBMS Quiz Assessment',
      exam_date: new Date(Date.now() + 86400000 * 12).toISOString().split('T')[0],
      start_time: '11:00 AM',
      location: 'Lab 302',
      syllabus: 'SQL Queries, Joins, Indexing, Transactions'
    }
  ],
  study_plans: [
    {
      id: 'sp-1',
      student_id: 'std-1',
      title: 'Midterm Sprint Plan 2026',
      start_date: new Date().toISOString().split('T')[0],
      end_date: new Date(Date.now() + 86400000 * 14).toISOString().split('T')[0],
      status: 'Active'
    }
  ],
  study_sessions: [
    {
      id: 'ss-1',
      plan_id: 'sp-1',
      subject_id: 'sub-1',
      topic: 'Binary Search Trees & AVL Rotations',
      session_date: new Date().toISOString().split('T')[0],
      start_time: '16:00',
      end_time: '18:00',
      priority: 'High',
      status: 'Completed'
    },
    {
      id: 'ss-2',
      plan_id: 'sp-1',
      subject_id: 'sub-2',
      topic: 'SQL Joins & Group By Aggregations',
      session_date: new Date(Date.now() + 86400000).toISOString().split('T')[0],
      start_time: '19:00',
      end_time: '21:00',
      priority: 'Medium',
      status: 'Pending'
    }
  ],
  documents: [
    {
      id: 'doc-1',
      title: 'College Academic Rules & Attendance Policy 2026.pdf',
      file_name: 'Academic_Policy_2026.pdf',
      file_size: 142850,
      uploaded_by: 'u-admin-1',
      chunk_count: 3,
      created_at: new Date().toISOString()
    }
  ],
  document_chunks: [
    {
      id: 'chk-1',
      document_id: 'doc-1',
      chunk_index: 0,
      content: 'Section 4.1 Attendance Policy: Students must maintain a minimum of 75% attendance in each registered subject to be eligible to sit for final semester examinations.',
      page_number: 1
    },
    {
      id: 'chk-2',
      document_id: 'doc-1',
      chunk_index: 1,
      content: 'Section 5.2 Grading System: Grades are calculated based on 30% Continuous Internal Evaluation (assignments & quizzes) and 70% Semester End Examination.',
      page_number: 2
    },
    {
      id: 'chk-3',
      document_id: 'doc-1',
      chunk_index: 2,
      content: 'Section 8.0 Library & Lab Rules: Computer labs are accessible 24/7 for enrolled engineering students with valid student ID badges.',
      page_number: 3
    }
  ],
  faqs: [
    { id: 'faq-1', question: 'What is the minimum attendance required for exam eligibility?', answer: '75% attendance in each registered subject.', category: 'Academic' }
  ],
  announcements: [
    { id: 'anc-1', title: 'Spring Midterm Schedule Published', content: 'Midterm exams begin next week. Check schedule tab.', posted_by: 'u-admin-1', created_at: new Date().toISOString() }
  ],
  quiz_questions: [],
  quiz_attempts: [
    { id: 'qa-1', student_id: 'std-1', subject_id: 'sub-1', topic: 'Data Structures', score: 4, total_questions: 5, percentage: 80.0, created_at: new Date().toISOString() }
  ],
  chat_sessions: [
    { id: 'cs-1', student_id: 'std-1', title: 'Midterm Study Guidance', created_at: new Date().toISOString() }
  ],
  chat_messages: [
    { id: 'cm-1', session_id: 'cs-1', sender: 'assistant', content: 'Hello Alex! I am your AI College Copilot. I have synced your profile, attendance, and upcoming exams. How can I help you today?', created_at: new Date().toISOString() }
  ],
  notifications: [
    { id: 'n-1', user_id: 'u-student-1', title: 'Upcoming Exam Alert', message: 'DSA Midterm Exam scheduled in 7 days.', type: 'exam', is_read: false }
  ],
  resumes: [],
  career_analysis: [],
  schedule: [
    { id: '1', course: 'CS 201: Data Structures', room: 'Hall B2', days: ['Mon', 'Wed', 'Fri'], time: '09:00 AM - 10:30 AM', instructor: 'Dr. Alan Turing' },
    { id: '2', course: 'MATH 220: Calculus III', room: 'Sci 104', days: ['Tue', 'Thu'], time: '11:00 AM - 12:30 PM', instructor: 'Prof. Katherine Johnson' },
    { id: '3', course: 'CS 340: Database Systems', room: 'Lab 4', days: ['Mon', 'Wed'], time: '02:00 PM - 03:30 PM', instructor: 'Dr. Grace Hopper' }
  ],
  budget: {
    monthlyTarget: 1200,
    currency: '$',
    expenses: [
      { id: '1', title: 'Textbooks & Lab Access', amount: 145.50, category: 'Academic', date: new Date().toISOString().split('T')[0] },
      { id: '2', title: 'Campus Meal Pass', amount: 280.00, category: 'Food', date: new Date().toISOString().split('T')[0] }
    ]
  },
  notes: [],
  career_profiles: [],
  interview_questions: [],
  interview_practices: []
};

function ensureDataDir() {
  if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  }
  if (!fs.existsSync(DB_FILE)) {
    fs.writeFileSync(DB_FILE, JSON.stringify(initialSeedData, null, 2), 'utf-8');
  }
}

export function getDB() {
  ensureDataDir();
  try {
    const raw = fs.readFileSync(DB_FILE, 'utf-8');
    const parsed = JSON.parse(raw);
    const db = { ...initialSeedData, ...parsed };
    if (!Array.isArray(db.resumes)) db.resumes = [];
    if (!Array.isArray(db.career_profiles)) db.career_profiles = [];
    if (!Array.isArray(db.interview_questions)) db.interview_questions = [];
    if (!Array.isArray(db.interview_practices)) db.interview_practices = [];
    if (!Array.isArray(db.notifications)) db.notifications = [];
    return db;
  } catch (err) {
    return initialSeedData;
  }
}

export function saveDB(data) {
  ensureDataDir();
  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2), 'utf-8');
  } catch (err) {
    console.error('Error writing db.json', err);
  }
}
