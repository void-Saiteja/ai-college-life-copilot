# Database Architecture & Entity Relationship (ER) Diagram

## Overview
The database layer for AI College Life Copilot is designed using a clean, normalized relational schema supporting MySQL 8.0+ and cloud-hosted Aiven MySQL instances.

## Entity Relationship (ER) Diagram

```
+----------------+        1:1       +------------------+
|     users      |------------------|     students     |
| (id, email,    |                  | (id, user_id,    |
|  role, pass)   |                  |  code, semester) |
+----------------+                  +------------------+
        |                                    |
        | 1:1                                | 1:N
        v                                    v
+----------------+                  +------------------+
|     admins     |                  |    attendance    |
| (id, user_id)  |                  | (id, student_id, |
+----------------+                  |  conducted, att) |
                                    +------------------+
                                             |
                                             | N:1
                                             v
+----------------+  1:N             +------------------+
|    subjects    |<-----------------|   assignments    |
| (id, code,     |                  | (id, student_id, |
|  name, instr)  |                  |  title, due_date)|
+----------------+                  +------------------+
        |                                    |
        | 1:N                                |
        v                                    v
+----------------+                  +------------------+
|     exams      |                  |   study_plans    |
| (id, subject,  |                  | (id, student_id) |
|  date, time)   |                  +------------------+
+----------------+                           | 1:N
                                             v
+----------------+                  +------------------+
|   documents    |                  |  study_sessions  |
| (id, title,    |                  | (id, plan_id,    |
|  uploaded_by)  |                  |  topic, status)  |
+----------------+                  +------------------+
        | 1:N
        v
+----------------+                  +------------------+
|document_chunks |                  |  chat_sessions   |
| (id, doc_id,   |                  | (id, student_id) |
|  content, page)|                  +------------------+
+----------------+                           | 1:N
                                             v
+----------------+                  +------------------+
| quiz_attempts  |                  |  chat_messages   |
| (id, student,  |                  | (id, session_id, |
|  score, pct)   |                  |  sender, content)|
+----------------+                  +------------------+
```

## Entity Reference (21 Tables)
1. `users`: Master user credential table (STUDENT / ADMIN).
2. `students`: Student profile parameters (semester, department, GPA).
3. `admins`: Admin role delegation and designations.
4. `subjects`: Academic course catalog.
5. `attendance`: Classes conducted & attended with generated percentage calculation.
6. `assignments`: Homework, lab projects, and exams with priority levels.
7. `exams`: Exam schedules, locations, and syllabus scope.
8. `study_plans`: Student active study sprint plans.
9. `study_sessions`: Daily study sessions associated with study plans.
10. `documents`: Uploaded college policy PDFs and metadata.
11. `document_chunks`: Extracted text blocks with page numbers and vector embeddings for RAG.
12. `faqs`: Common student queries and administrative responses.
13. `announcements`: Global campus noticeboard.
14. `quiz_questions`: Bank of AI-generated MCQs.
15. `quiz_attempts`: Student test scores and topic performance history.
16. `chat_sessions`: Student AI copilot conversation threads.
17. `chat_messages`: Individual prompt and response messages.
18. `notifications`: Student academic deadline and exam alerts.
19. `resumes`: Uploaded student resume text files.
20. `career_analysis`: Target job role skill gap analysis and mock interview records.
21. `schedule`: Weekly class timetable grid.
