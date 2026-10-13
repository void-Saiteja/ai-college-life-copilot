import { getDB, saveDB } from '../storage/db.js';
import { buildStudentContext } from '../services/contextService.js';
import { retrieveRelevantChunks } from '../services/retrievalService.js';
import { generateGeminiResponse } from '../services/llmService.js';

export const getChatSessions = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }
    const db = getDB();

    const sessions = (db.chat_sessions || []).filter(s => s.student_id === studentId);
    res.json({ success: true, data: sessions });
  } catch (error) {
    next(error);
  }
};

export const getSessionMessages = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }
    const { id } = req.params;
    const db = getDB();

    const session = (db.chat_sessions || []).find(s => s.id === id);
    if (!session) {
      return res.status(404).json({ success: false, error: 'Chat session not found' });
    }

    if (session.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this chat session' });
    }

    const messages = (db.chat_messages || []).filter(m => m.session_id === id);
    res.json({ success: true, data: messages });
  } catch (error) {
    next(error);
  }
};

export function isSimpleGreeting(text) {
  if (!text || typeof text !== 'string') return false;
  const cleaned = text.trim().toLowerCase().replace(/[!.,?]+$/, '').trim();
  const greetingPatterns = [
    'hi',
    'hello',
    'hey',
    'hey there',
    'hi there',
    'hello there',
    'good morning',
    'good afternoon',
    'good evening',
    'greetings',
    'sup',
    'yo',
    "what's up",
    'whats up',
    'hi copilot',
    'hello copilot',
    'hey copilot'
  ];
  return greetingPatterns.includes(cleaned);
}

export const postChatMessage = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }
    const { sessionId, message } = req.body;

    if (!message || typeof message !== 'string' || message.trim() === '') {
      return res.status(400).json({ success: false, error: 'Message content is required' });
    }

    if (message.length > 4000) {
      return res.status(400).json({ success: false, error: 'Message exceeds maximum limit of 4000 characters' });
    }

    const db = getDB();
    let currentSessionId = sessionId;

    if (currentSessionId) {
      const existingSession = (db.chat_sessions || []).find(s => s.id === currentSessionId);
      if (!existingSession) {
        return res.status(404).json({ success: false, error: 'Chat session not found' });
      }
      if (existingSession.student_id !== studentId) {
        return res.status(403).json({ success: false, error: 'Access denied: You do not own this chat session' });
      }
    } else {
      currentSessionId = 'cs-' + Date.now();
      const newSession = {
        id: currentSessionId,
        student_id: studentId,
        title: message.trim().length > 30 ? message.trim().substring(0, 30) + '...' : message.trim(),
        created_at: new Date().toISOString()
      };
      db.chat_sessions.unshift(newSession);
    }

    // 1. Save user message to database
    const userMsg = {
      id: 'cm-' + Date.now() + '-u',
      session_id: currentSessionId,
      sender: 'user',
      content: message.trim(),
      created_at: new Date().toISOString()
    };
    db.chat_messages.push(userMsg);

    // 2. Fetch student academic context
    const studentCtx = buildStudentContext(studentId);

    // 3. Short-circuit simple greetings without semantic vector search or Gemini API call
    if (isSimpleGreeting(message.trim())) {
      const studentName = studentCtx?.student?.name ? studentCtx.student.name.split(' ')[0] : 'there';
      const greetingReply = `👋 Hello ${studentName}! I'm your AI College Life Copilot.

I can help you with:
• **Academic Status**: Check your attendance percentages, bunk allowances, and GPA standing
• **Deadlines & Exams**: Review upcoming assignments, project submissions, and exam schedules
• **College Policies**: Answer questions about grading, leaves, attendance, and campus guidelines

How can I help you today?`;

      const assistantMsg = {
        id: 'cm-' + Date.now() + '-a',
        session_id: currentSessionId,
        sender: 'assistant',
        content: greetingReply,
        sources: [],
        created_at: new Date().toISOString()
      };
      db.chat_messages.push(assistantMsg);
      saveDB(db);

      return res.json({
        success: true,
        data: {
          sessionId: currentSessionId,
          userMessage: userMsg,
          assistantMessage: assistantMsg
        }
      });
    }

    // 4. Retrieve relevant document chunks via semantic RAG vector search (safely wrapped)
    let docChunks = [];
    try {
      docChunks = await retrieveRelevantChunks(message.trim(), 3);
    } catch (ragErr) {
      console.warn('[AI Copilot Semantic RAG]: Vector search error, proceeding with student context only:', ragErr.message);
      docChunks = [];
    }

    // 5. Retrieve recent conversation history for context continuity
    const previousMessages = (db.chat_messages || []).filter(m => m.session_id === currentSessionId && m.id !== userMsg.id);

    // 6. Generate LLM response from Google Gemini with student + document context
    const llmResult = await generateGeminiResponse({
      message: message.trim(),
      context: studentCtx,
      docChunks,
      history: previousMessages
    });

    const aiReply = llmResult.reply;

    // Build structured source metadata
    const sources = docChunks && docChunks.length > 0 ? docChunks.map(c => ({
      documentName: c.documentTitle,
      pageNumber: c.page_number,
      snippet: c.content
    })) : [];

    // 7. Save assistant response to database
    const assistantMsg = {
      id: 'cm-' + Date.now() + '-a',
      session_id: currentSessionId,
      sender: 'assistant',
      content: aiReply,
      sources,
      created_at: new Date().toISOString()
    };
    db.chat_messages.push(assistantMsg);

    saveDB(db);

    res.json({
      success: true,
      data: {
        sessionId: currentSessionId,
        userMessage: userMsg,
        assistantMessage: assistantMsg
      }
    });

  } catch (error) {
    next(error);
  }
};

export const deleteChatSession = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }
    const { id } = req.params;
    const db = getDB();

    const session = (db.chat_sessions || []).find(s => s.id === id);
    if (!session) {
      return res.status(404).json({ success: false, error: 'Chat session not found' });
    }

    if (session.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this chat session' });
    }

    db.chat_sessions = (db.chat_sessions || []).filter(s => s.id !== id);
    db.chat_messages = (db.chat_messages || []).filter(m => m.session_id !== id);
    saveDB(db);

    res.json({ success: true, message: 'Chat conversation deleted' });
  } catch (error) {
    next(error);
  }
};
