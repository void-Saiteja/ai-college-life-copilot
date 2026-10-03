-- Seed script for AI College Life Copilot
-- Password hashes created using bcryptjs for 'Admin@123' and 'Student@123'

INSERT IGNORE INTO users (id, name, email, password_hash, role) VALUES 
('u-admin-1', 'Dr. Sarah Connor', 'admin@college.edu', '$2b$10$KBH7Vh0A0qhwIr38KfsiQOxK/z3qKtc6A5LHVIlVqfG.fpvbNRGwm', 'ADMIN'),
('u-student-1', 'Alex Mercer', 'alex@student.edu', '$2b$10$KBH7Vh0A0qhwIr38KfsiQOxK/z3qKtc6A5LHVIlVqfG.fpvbNRGwm', 'STUDENT');

INSERT IGNORE INTO admins (id, user_id, designation) VALUES
('adm-1', 'u-admin-1', 'Head of Academic Affairs');

INSERT IGNORE INTO students (id, user_id, student_code, semester, department, gpa) VALUES
('std-1', 'u-student-1', 'CS-2026-88', 4, 'Computer Science & Engineering', 3.85);

INSERT IGNORE INTO subjects (id, code, name, instructor, credits, department) VALUES
('sub-1', 'CS 201', 'Data Structures & Algorithms', 'Dr. Alan Turing', 4, 'Computer Science'),
('sub-2', 'CS 340', 'Database Management Systems', 'Dr. Grace Hopper', 3, 'Computer Science'),
('sub-3', 'MATH 220', 'Calculus III', 'Prof. Katherine Johnson', 3, 'Mathematics'),
('sub-4', 'CS 410', 'Artificial Intelligence', 'Dr. Marvin Minsky', 4, 'Computer Science');

INSERT IGNORE INTO attendance (id, student_id, subject_id, classes_conducted, classes_attended) VALUES
('att-1', 'std-1', 'sub-1', 28, 25),
('att-2', 'std-1', 'sub-2', 20, 18),
('att-3', 'std-1', 'sub-3', 24, 22),
('att-4', 'std-1', 'sub-4', 16, 14);

INSERT IGNORE INTO assignments (id, student_id, subject_id, title, description, due_date, priority, status, estimated_hours) VALUES
('asg-1', 'std-1', 'sub-1', 'B-Trees & Red-Black Tree Implementation', 'Implement a self-balancing Red-Black tree in C++/Java with full rotation logs.', CURDATE() + INTERVAL 2 DAY, 'High', 'In Progress', 5.0),
('asg-2', 'std-1', 'sub-2', 'Relational Schema Normalization Project', 'Normalize 3NF schema to BCNF for campus database.', CURDATE() + INTERVAL 5 DAY, 'Medium', 'Pending', 3.5),
('asg-3', 'std-1', 'sub-3', 'Partial Differentiation Problem Set', 'Solve problems 1-15 in Chapter 4.', CURDATE() + INTERVAL 1 DAY, 'High', 'Pending', 2.0);

INSERT IGNORE INTO exams (id, subject_id, title, exam_date, start_time, location, syllabus) VALUES
('exm-1', 'sub-1', 'DSA Midterm Examination', CURDATE() + INTERVAL 7 DAY, '09:00:00', 'Hall A2', 'Arrays, Linked Lists, Trees, Graphs, Sorting Algorithms'),
('exm-2', 'sub-2', 'DBMS Quiz Assessment', CURDATE() + INTERVAL 12 DAY, '11:00:00', 'Lab 302', 'SQL Queries, Joins, Indexing, Transactions');

INSERT IGNORE INTO faqs (id, question, answer, category) VALUES
('faq-1', 'What is the minimum attendance required for exam eligibility?', 'Students must maintain a minimum of 75% attendance in each subject to be eligible for final semester examinations.', 'Academic Rules'),
('faq-2', 'How do I apply for a course drop or semester leave?', 'Submit Form B to the Dean of Academic Affairs within the first 3 weeks of the semester.', 'Administration');

INSERT IGNORE INTO announcements (id, title, content, posted_by) VALUES
('anc-1', 'Spring Semester Midterm Schedule Released', 'Midterm examination timetables have been published on the student portal. Please check hall allocations.', 'u-admin-1');

INSERT IGNORE INTO notifications (id, user_id, title, message, type, is_read) VALUES
('notif-1', 'u-student-1', 'Assignment Due Soon', 'B-Trees & Red-Black Tree Implementation is due in 2 days.', 'assignment', 0),
('notif-2', 'u-student-1', 'Exam Schedule Update', 'DSA Midterm Examination set for next week in Hall A2.', 'exam', 0);
