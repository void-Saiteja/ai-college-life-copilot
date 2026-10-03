import { getDB, saveDB } from '../storage/db.js';
import {
  calculateAttendanceMetrics,
  calculateProjectionMetrics,
  generateAttendanceExplanation
} from '../services/attendanceService.js';

export const getAttendance = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { target = 75 } = req.query;
    const db = getDB();

    const records = (db.attendance || [])
      .filter(a => a.student_id === studentId)
      .map(a => {
        const subject = (db.subjects || []).find(s => s.id === a.subject_id);
        const metrics = calculateAttendanceMetrics({
          conducted: a.classes_conducted,
          attended: a.classes_attended,
          targetPercentage: Number(target)
        });

        return {
          id: a.id,
          subjectId: a.subject_id,
          subjectCode: subject?.code || 'CS',
          subjectName: subject?.name || 'Subject',
          instructor: subject?.instructor || 'Faculty',
          ...metrics
        };
      });

    res.json({ success: true, data: records });
  } catch (error) {
    next(error);
  }
};

export const updateAttendance = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const { classes_conducted, classes_attended } = req.body;
    const db = getDB();

    const idx = (db.attendance || []).findIndex(a => a.id === id);
    if (idx === -1) {
      return res.status(404).json({ success: false, error: 'Attendance record not found' });
    }

    const record = db.attendance[idx];
    // Authorization Check: Student can only update their own attendance record
    if (record.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this attendance record' });
    }

    db.attendance[idx].classes_conducted = Number(classes_conducted || 0);
    db.attendance[idx].classes_attended = Number(classes_attended || 0);
    saveDB(db);

    const subject = (db.subjects || []).find(s => s.id === record.subject_id);
    const metrics = calculateAttendanceMetrics({
      conducted: db.attendance[idx].classes_conducted,
      attended: db.attendance[idx].classes_attended
    });

    res.json({
      success: true,
      data: {
        id: record.id,
        subjectId: record.subject_id,
        subjectCode: subject?.code || 'CS',
        subjectName: subject?.name || 'Subject',
        ...metrics
      }
    });
  } catch (error) {
    next(error);
  }
};

export const calculateProjection = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { conducted, attended, missClasses = 0, attendClasses = 0, targetPercentage = 75 } = req.body;

    const result = calculateProjectionMetrics({
      conducted: Number(conducted || 0),
      attended: Number(attended || 0),
      futureMissed: Number(missClasses || 0),
      futureAttended: Number(attendClasses || 0),
      targetPercentage: Number(targetPercentage || 75)
    });

    res.json({
      success: true,
      data: {
        current: result.current,
        ifMissNext: {
          additionalMissed: result.futureInput.futureMissed,
          projectedConducted: result.projected.conducted,
          projectedAttended: result.current.attended,
          projectedPercentage: Number(((result.current.attended / (result.current.conducted + result.futureInput.futureMissed || 1)) * 100).toFixed(2)),
          status: result.projected.status,
          riskClassification: result.projected.riskClassification,
          maxCanMiss: result.projected.maxCanMiss,
          minRequiredToAttend: result.projected.minRequiredToAttend
        },
        ifAttendNext: {
          additionalAttended: result.futureInput.futureAttended,
          projectedConducted: result.current.conducted + result.futureInput.futureAttended,
          projectedAttended: result.current.attended + result.futureInput.futureAttended,
          projectedPercentage: Number((((result.current.attended + result.futureInput.futureAttended) / (result.current.conducted + result.futureInput.futureAttended || 1)) * 100).toFixed(2)),
          status: result.projected.status,
          riskClassification: result.projected.riskClassification,
          maxCanMiss: result.projected.maxCanMiss,
          minRequiredToAttend: result.projected.minRequiredToAttend
        },
        fullProjection: result,
        disclaimer: result.disclaimer
      }
    });
  } catch (error) {
    next(error);
  }
};

export const explainAttendance = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { subjectId, conducted, attended, targetPercentage = 75 } = req.body;
    const db = getDB();

    let subjectName = 'General Subject';
    let subjectCode = 'CS';

    if (subjectId) {
      const subject = (db.subjects || []).find(s => s.id === subjectId);
      const record = (db.attendance || []).find(a => a.subject_id === subjectId && a.student_id === studentId);

      if (!subject || !record) {
        return res.status(404).json({ success: false, error: 'Subject or attendance record not found for student' });
      }

      subjectName = subject.name;
      subjectCode = subject.code;
    }

    const metrics = calculateAttendanceMetrics({
      conducted: Number(conducted || 0),
      attended: Number(attended || 0),
      targetPercentage: Number(targetPercentage || 75)
    });

    const aiExplanation = await generateAttendanceExplanation({
      subjectCode,
      subjectName,
      ...metrics
    });

    res.json({
      success: true,
      data: {
        metrics,
        explanation: aiExplanation.explanation,
        isFallback: aiExplanation.isFallback
      }
    });

  } catch (error) {
    next(error);
  }
};
