import { getDB, saveDB } from '../storage/db.js';

export const getNotes = (req, res, next) => {
  try {
    const db = getDB();
    res.json({ success: true, data: db.notes || [] });
  } catch (error) {
    next(error);
  }
};

export const createNote = (req, res, next) => {
  try {
    const { title, course, content, tags } = req.body;
    if (!title || !content) {
      return res.status(400).json({ success: false, error: 'Title and content are required' });
    }

    const db = getDB();
    const newNote = {
      id: Date.now().toString(),
      title,
      course: course || 'General',
      content,
      tags: Array.isArray(tags) ? tags : (tags ? tags.split(',').map(t => t.trim()) : []),
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
    const { id } = req.params;
    const db = getDB();
    const idx = db.notes.findIndex(n => n.id === id);

    if (idx === -1) {
      return res.status(404).json({ success: false, error: 'Note not found' });
    }

    db.notes[idx] = {
      ...db.notes[idx],
      ...req.body,
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
    const { id } = req.params;
    const db = getDB();
    db.notes = db.notes.filter(n => n.id !== id);
    saveDB(db);
    res.json({ success: true, message: 'Note deleted' });
  } catch (error) {
    next(error);
  }
};
