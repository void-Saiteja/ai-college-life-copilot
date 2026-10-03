-- AI College Life Copilot Relational Database Schema
-- Fully Compatible with MySQL 8.0+ and Aiven Cloud MySQL

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    avatar_url VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
);

CREATE TABLE IF NOT EXISTS students (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) UNIQUE NOT NULL,
    student_code VARCHAR(50) UNIQUE NOT NULL,
    semester INT DEFAULT 1,
    department VARCHAR(100) DEFAULT 'Computer Science',
    gpa DECIMAL(3,2) DEFAULT 3.80,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_students_user (user_id)
);

CREATE TABLE IF NOT EXISTS admins (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) UNIQUE NOT NULL,
    designation VARCHAR(100) DEFAULT 'Academic Administrator',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_admins_user (user_id)
);

CREATE TABLE IF NOT EXISTS subjects (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    instructor VARCHAR(100) NOT NULL,
    credits INT DEFAULT 3,
    department VARCHAR(100) DEFAULT 'Computer Science',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subjects_code (code)
);

CREATE TABLE IF NOT EXISTS attendance (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    subject_id VARCHAR(36) NOT NULL,
    classes_conducted INT DEFAULT 0,
    classes_attended INT DEFAULT 0,
    percentage DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE WHEN classes_conducted > 0 THEN (classes_attended / classes_conducted) * 100 ELSE 0 END
    ) STORED,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    INDEX idx_attendance_student (student_id),
    INDEX idx_attendance_subject (subject_id)
);

CREATE TABLE IF NOT EXISTS assignments (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    subject_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    due_date DATE NOT NULL,
    priority ENUM('High', 'Medium', 'Low') DEFAULT 'Medium',
    status ENUM('Pending', 'In Progress', 'Completed', 'Overdue') DEFAULT 'Pending',
    estimated_hours DECIMAL(4,1) DEFAULT 2.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    INDEX idx_assignments_student (student_id),
    INDEX idx_assignments_status (status)
);

CREATE TABLE IF NOT EXISTS exams (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) DEFAULT NULL,
    subject_id VARCHAR(36) NOT NULL,
    title VARCHAR(150) NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    location VARCHAR(100) DEFAULT 'Main Exam Hall',
    syllabus TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    INDEX idx_exams_student (student_id),
    INDEX idx_exams_date (exam_date)
);

CREATE TABLE IF NOT EXISTS study_plans (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    title VARCHAR(150) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_hours DECIMAL(5,1) DEFAULT 10.0,
    status ENUM('Active', 'Completed', 'Replanned') DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_study_plans_student (student_id)
);

CREATE TABLE IF NOT EXISTS study_sessions (
    id VARCHAR(36) PRIMARY KEY,
    plan_id VARCHAR(36) NOT NULL,
    subject_id VARCHAR(36) NOT NULL,
    topic VARCHAR(200) NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    priority ENUM('High', 'Medium', 'Low') DEFAULT 'Medium',
    status ENUM('Pending', 'Completed', 'Missed') DEFAULT 'Pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES study_plans(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    INDEX idx_study_sessions_plan (plan_id)
);

CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size INT NOT NULL,
    uploaded_by VARCHAR(36) NOT NULL,
    chunk_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_documents_uploader (uploaded_by)
);

CREATE TABLE IF NOT EXISTS document_chunks (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    page_number INT DEFAULT 1,
    vector_embedding JSON DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    INDEX idx_chunks_document (document_id)
);

CREATE TABLE IF NOT EXISTS faqs (
    id VARCHAR(36) PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(50) DEFAULT 'General',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS announcements (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    posted_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (posted_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_announcements_posted (posted_by)
);

CREATE TABLE IF NOT EXISTS quiz_questions (
    id VARCHAR(36) PRIMARY KEY,
    subject_id VARCHAR(36) NOT NULL,
    topic VARCHAR(150) NOT NULL,
    question TEXT NOT NULL,
    options JSON NOT NULL,
    correct_answer INT NOT NULL,
    explanation TEXT,
    difficulty ENUM('Easy', 'Medium', 'Hard') DEFAULT 'Medium',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    INDEX idx_quiz_subject (subject_id)
);

CREATE TABLE IF NOT EXISTS quiz_attempts (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    subject_id VARCHAR(36) NOT NULL,
    topic VARCHAR(150) NOT NULL,
    score INT NOT NULL,
    total_questions INT NOT NULL,
    percentage DECIMAL(5,2) NOT NULL,
    recommended_topics JSON DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    INDEX idx_quiz_attempts_student (student_id)
);

CREATE TABLE IF NOT EXISTS chat_sessions (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    title VARCHAR(150) NOT NULL DEFAULT 'New AI Conversation',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_chat_student (student_id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    sender ENUM('user', 'assistant') NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    INDEX idx_messages_session (session_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('assignment', 'exam', 'planner', 'announcement', 'general') DEFAULT 'general',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_user (user_id)
);

CREATE TABLE IF NOT EXISTS resumes (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) DEFAULT NULL,
    target_role VARCHAR(100) DEFAULT 'Full Stack Developer',
    file_name VARCHAR(255) DEFAULT 'Resume.pdf',
    extracted_text LONGTEXT NOT NULL,
    analysis JSON DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_resumes_student (student_id)
);

CREATE TABLE IF NOT EXISTS career_analysis (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    target_role VARCHAR(100) NOT NULL,
    skills_detected JSON DEFAULT NULL,
    missing_skills JSON DEFAULT NULL,
    suggestions JSON DEFAULT NULL,
    interview_questions JSON DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_career_student (student_id)
);

CREATE TABLE IF NOT EXISTS career_profiles (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) UNIQUE NOT NULL,
    career_goal VARCHAR(255) DEFAULT 'Become a Software Engineer',
    target_role VARCHAR(100) DEFAULT 'Full Stack Developer',
    domain VARCHAR(100) DEFAULT 'Software Development',
    skills JSON DEFAULT NULL,
    interests JSON DEFAULT NULL,
    experience_projects JSON DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_cp_student (student_id)
);

CREATE TABLE IF NOT EXISTS interview_questions (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    question TEXT NOT NULL,
    category VARCHAR(50) DEFAULT 'TECHNICAL',
    difficulty VARCHAR(50) DEFAULT 'MEDIUM',
    target_skill VARCHAR(100) DEFAULT NULL,
    target_role VARCHAR(100) DEFAULT 'Full Stack Developer',
    preparation_tips TEXT DEFAULT NULL,
    is_ai_generated BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_iq_student (student_id)
);

CREATE TABLE IF NOT EXISTS interview_practices (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    question_id VARCHAR(36) DEFAULT NULL,
    question TEXT NOT NULL,
    category VARCHAR(50) DEFAULT 'TECHNICAL',
    difficulty VARCHAR(50) DEFAULT 'MEDIUM',
    target_skill VARCHAR(100) DEFAULT NULL,
    target_role VARCHAR(100) DEFAULT 'Full Stack Developer',
    answer TEXT NOT NULL,
    scores JSON DEFAULT NULL,
    feedback TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_ip_student (student_id)
);

CREATE TABLE IF NOT EXISTS schedules (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    course VARCHAR(100) NOT NULL,
    room VARCHAR(50) DEFAULT 'TBD',
    days JSON NOT NULL,
    time VARCHAR(50) NOT NULL,
    instructor VARCHAR(100) DEFAULT 'Staff',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_schedules_student (student_id)
);

CREATE TABLE IF NOT EXISTS notes (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    course VARCHAR(60) DEFAULT 'General',
    content MEDIUMTEXT NOT NULL,
    tags JSON DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_notes_student (student_id)
);

CREATE TABLE IF NOT EXISTS budgets (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) UNIQUE NOT NULL,
    monthly_target DECIMAL(10,2) NOT NULL DEFAULT 1200.00,
    currency VARCHAR(10) DEFAULT '$',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_budgets_student (student_id)
);

CREATE TABLE IF NOT EXISTS expenses (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    title VARCHAR(150) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    category VARCHAR(50) DEFAULT 'General',
    expense_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_expenses_student (student_id)
);
