import { getDB, saveDB } from '../storage/db.js';

export const getSchedule = (req, res, next) => {
  try {
    const db = getDB();
    res.json({ success: true, data: db.schedule || [] });
  } catch (error) {
    next(error);
  }
};

export const addScheduleItem = (req, res, next) => {
  try {
    const { course, room, days, time, instructor } = req.body;
    if (!course || !time) {
      return res.status(400).json({ success: false, error: 'Course name and time are required' });
    }

    const db = getDB();
    const newItem = {
      id: Date.now().toString(),
      course,
      room: room || 'TBD',
      days: Array.isArray(days) ? days : [days || 'Mon'],
      time,
      instructor: instructor || 'Staff'
    };

    db.schedule.push(newItem);
    saveDB(db);

    res.status(201).json({ success: true, data: newItem });
  } catch (error) {
    next(error);
  }
};

export const deleteScheduleItem = (req, res, next) => {
  try {
    const { id } = req.params;
    const db = getDB();
    db.schedule = db.schedule.filter(s => s.id !== id);
    saveDB(db);
    res.json({ success: true, message: 'Class schedule item removed' });
  } catch (error) {
    next(error);
  }
};
