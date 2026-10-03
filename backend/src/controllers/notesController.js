import { getDB, saveDB } from '../storage/db.js';

export const getNotes = (req, res, next) => {
  try {
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    // Strictly filter notes by student ownership
    const studentNotes = (db.notes || []).filter(
      n => n.student_id === studentKey || (!n.student_id && studentKey === 'std-1')
    );

    res.json({ success: true, data: studentNotes });
  } catch (error) {
    next(error);
  }
};

export const createNote = (req, res, next) => {
  try {
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { title, course, content, tags } = req.body;

    // Strict Validation
    if (!title || typeof title !== 'string' || title.trim().length === 0 || title.length > 200) {
      return res.status(400).json({ success: false, error: 'Title is required (1-200 characters)' });
    }

    if (!content || typeof content !== 'string' || content.trim().length === 0) {
      return res.status(400).json({ success: false, error: 'Content is required' });
    }

    if (content.length > 50000) {
      return res.status(400).json({
        success: false,
        error: 'Note content exceeds maximum allowable limit of 50,000 characters'
      });
    }

    if (course && (typeof course !== 'string' || course.length > 60)) {
      return res.status(400).json({ success: false, error: 'Course code must be a string up to 60 characters' });
    }

    let parsedTags = [];
    if (Array.isArray(tags)) {
      parsedTags = tags.filter(t => typeof t === 'string').map(t => t.trim().slice(0, 30)).slice(0, 20);
    } else if (typeof tags === 'string' && tags.trim()) {
      parsedTags = tags.split(',').map(t => t.trim().slice(0, 30)).filter(Boolean).slice(0, 20);
    }

    const db = getDB();
    if (!Array.isArray(db.notes)) db.notes = [];

    const newNote = {
      id: 'nt-' + Date.now(),
      student_id: studentKey,
      title: title.trim(),
      course: course ? course.trim() : 'General',
      content: content.trim(),
      tags: parsedTags,
      updatedAt: new Date().toISOString()
    };

    db.notes.unshift(newNote);
    saveDB(db);

    res.status(201).json({ success: true, data: newNote });
  } catch (error) {
    next(error);
  }
};

export const updateNote = (req, res, next) => {
  try {
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const idx = (db.notes || []).findIndex(n => n.id === id);

    if (idx === -1) {
      return res.status(404).json({ success: false, error: 'Note not found' });
    }

    const existing = db.notes[idx];

    // Ownership check: Student can only update their own notes
    const isOwner = existing.student_id === studentKey || (!existing.student_id && studentKey === 'std-1');
    if (!isOwner) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this note' });
    }

    const { title, course, content, tags } = req.body;

    if (title && (typeof title !== 'string' || title.length > 200)) {
      return res.status(400).json({ success: false, error: 'Title must be up to 200 characters' });
    }

    if (content && typeof content === 'string' && content.length > 50000) {
      return res.status(400).json({ success: false, error: 'Content exceeds 50,000 characters limit' });
    }

    let parsedTags = existing.tags;
    if (tags !== undefined) {
      if (Array.isArray(tags)) {
        parsedTags = tags.filter(t => typeof t === 'string').map(t => t.trim().slice(0, 30)).slice(0, 20);
      } else if (typeof tags === 'string') {
        parsedTags = tags.split(',').map(t => t.trim().slice(0, 30)).filter(Boolean).slice(0, 20);
      }
    }

    db.notes[idx] = {
      ...existing,
      ...(title ? { title: title.trim() } : {}),
      ...(course ? { course: course.trim() } : {}),
      ...(content ? { content: content.trim() } : {}),
      tags: parsedTags,
      student_id: existing.student_id || studentKey,
      updatedAt: new Date().toISOString()
    };

    saveDB(db);
    res.json({ success: true, data: db.notes[idx] });
  } catch (error) {
    next(error);
  }
};

export const deleteNote = (req, res, next) => {
  try {
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const existing = (db.notes || []).find(n => n.id === id);

    if (!existing) {
      return res.status(404).json({ success: false, error: 'Note not found' });
    }

    // Ownership check: Student can only delete their own notes
    const isOwner = existing.student_id === studentKey || (!existing.student_id && studentKey === 'std-1');
    if (!isOwner) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this note' });
    }

    db.notes = db.notes.filter(n => n.id !== id);
    saveDB(db);
    res.json({ success: true, message: 'Note deleted' });
  } catch (error) {
    next(error);
  }
};
