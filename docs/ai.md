# AI Architecture & Context Builder Documentation

## Overview
AI College Life Copilot uses a decoupled AI service architecture (`aiService.js` and `contextService.js`).

## Context Builder Pipeline
When a student interacts with the AI Copilot:
1. `contextService.js` extracts:
   - Student Profile (Name, Semester, Department, GPA)
   - Subject Enrollment
   - Attendance Percentages
   - Upcoming Assignments & Due Dates
   - Upcoming Exam Schedule
   - Active Study Plan Progress
2. The context is injected into the prompt payload.
3. If the user asks about attendance, assignment due dates, or exam schedules, the AI uses exact factual student data.

## Rate Limiting & Error Fallback
- If the AI API key is missing or quota is exceeded, the backend gracefully falls back to the smart local rules engine, guaranteeing uninterrupted service during viva/demonstrations.
