# API Reference - AI College Life Copilot

Base URL: `http://localhost:5000/api`

## Authentication APIs
- `POST /api/auth/register`: Register new student or admin account.
- `POST /api/auth/login`: Authenticate credentials, return JWT token.
- `GET /api/auth/me`: Get active authenticated user profile.

## Student & Dashboard APIs
- `GET /api/student/dashboard`: Retrieve aggregated dashboard stats (subjects, attendance, deadlines, exams).

## Attendance APIs
- `GET /api/attendance`: Retrieve attendance records.
- `PUT /api/attendance/:id`: Update conducted/attended classes count.
- `POST /api/attendance/projection`: Calculate mathematical projection ("If I miss/attend X classes...").

## Assignments & Exams APIs
- `GET /api/assignments`: Get assignments list.
- `POST /api/assignments`: Create new assignment.
- `PUT /api/assignments/:id`: Update status or priority.
- `DELETE /api/assignments/:id`: Remove assignment.
- `GET /api/exams`: Get exam schedule.
- `POST /api/exams`: Schedule exam.

## AI Copilot & Chat APIs
- `GET /api/chat/sessions`: List chat conversations.
- `GET /api/chat/sessions/:id`: Get session message history.
- `POST /api/chat/chat`: Send prompt, receive context-aware response.
- `DELETE /api/chat/sessions/:id`: Delete chat conversation.

## RAG Document APIs
- `GET /api/documents`: List uploaded college documents.
- `POST /api/documents/upload`: Upload PDF/text document and index RAG chunks (Admin).
- `POST /api/documents/query`: Query document knowledge base with source citation page numbers.
- `DELETE /api/documents/:id`: Remove document (Admin).

## AI Study Planner APIs
- `GET /api/planner`: Get active study plan & sessions.
- `POST /api/planner/generate`: Generate personalized AI study plan.
- `PUT /api/planner/session/:id`: Mark session completed/pending.
- `POST /api/planner/replan`: Replan remaining sessions without deleting completed ones.

## AI Quiz & Career APIs
- `POST /api/quiz/generate`: Generate MCQs for topic.
- `POST /api/quiz/submit`: Submit quiz answers, calculate score & revision areas.
- `POST /api/career/analyze-resume`: Extract skills from resume against target role.
- `POST /api/career/interview-practice`: Evaluate mock interview answer.

## Class Timetable & Schedule APIs
- `GET /api/schedule`: List all weekly class lecture and lab periods.
- `POST /api/schedule`: Add new course schedule item with room, time, instructor, and active days.
- `DELETE /api/schedule/:id`: Remove class schedule item.

## Study Notes & AI Study Lab APIs
- `GET /api/notes`: Get student's saved lecture notes with tags and courses.
- `POST /api/notes`: Create new study note with tags and content.
- `PUT /api/notes/:id`: Update note content or tags.
- `DELETE /api/notes/:id`: Delete study note.
- `POST /api/ai/summarize`: AI extract executive summary, flashcards, and practice quiz from notes.
- `POST /api/ai/chat`: Interactive AI study copilot conversation.

## Student Budget & Meal Planner APIs
- `GET /api/budget`: Get budget overview, monthly target, total spent, and category breakdown.
- `POST /api/budget/expense`: Log expense with title, amount, and category.
- `PUT /api/budget/target`: Update monthly target allowance.
- `DELETE /api/budget/expense/:id`: Remove expense entry.
- `POST /api/ai/meal-planner`: AI cheap student meal plans and grocery budgeting recommendations.
