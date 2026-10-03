import { getDB, saveDB } from '../storage/db.js';
import { hashPassword, comparePassword } from '../utils/passwords.js';
import { generateToken } from '../utils/jwt.js';

export const register = async (req, res, next) => {
  try {
    const { name, email, password, role = 'STUDENT', department, semester } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({ success: false, error: 'Name, email, and password are required' });
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({ success: false, error: 'Invalid email format' });
    }

    if (password.length < 6) {
      return res.status(400).json({ success: false, error: 'Password must be at least 6 characters long' });
    }

    const db = getDB();
    const existing = db.users.find(u => u.email.toLowerCase() === email.toLowerCase());
    if (existing) {
      return res.status(409).json({ success: false, error: 'User with this email already exists' });
    }

    // Role Authorization Check: Disallow unauthorized public ADMIN registration
    let userRole = 'STUDENT';
    if (role && role.toUpperCase() === 'ADMIN') {
      const adminSecret = req.body.adminSecret || req.headers['x-admin-key'];
      const expectedSecret = process.env.ADMIN_SECRET || 'dev_admin_secret_key_2026';
      if (adminSecret === expectedSecret || (req.user && req.user.role === 'ADMIN')) {
        userRole = 'ADMIN';
      } else {
        return res.status(403).json({
          success: false,
          error: 'Forbidden: Admin registration requires valid administrative authorization or secret key'
        });
      }
    }

    const password_hash = await hashPassword(password);
    const userId = 'u-' + Date.now();
    const newUser = {
      id: userId,
      name,
      email: email.toLowerCase(),
      password_hash,
      role: userRole,
      avatar_url: null,
      created_at: new Date().toISOString()
    };

    db.users.push(newUser);

    let studentId = null;
    let adminId = null;

    if (newUser.role === 'STUDENT') {
      studentId = 'std-' + Date.now();
      db.students.push({
        id: studentId,
        user_id: userId,
        student_code: 'CS-' + Math.floor(1000 + Math.random() * 9000),
        semester: Number(semester) || 1,
        department: department || 'Computer Science & Engineering',
        gpa: 3.80
      });
    } else {
      adminId = 'adm-' + Date.now();
      db.admins.push({
        id: adminId,
        user_id: userId,
        designation: 'Academic Administrator'
      });
    }

    saveDB(db);

    const token = generateToken({
      id: newUser.id,
      name: newUser.name,
      email: newUser.email,
      role: newUser.role,
      studentId,
      adminId
    });

    return res.status(201).json({
      success: true,
      message: 'Registration successful',
      token,
      user: {
        id: newUser.id,
        name: newUser.name,
        email: newUser.email,
        role: newUser.role,
        studentId,
        adminId
      }
    });

  } catch (error) {
    next(error);
  }
};

export const login = async (req, res, next) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ success: false, error: 'Email and password are required' });
    }

    const db = getDB();
    const user = db.users.find(u => u.email.toLowerCase() === email.toLowerCase());

    if (!user) {
      return res.status(401).json({ success: false, error: 'Invalid email or password' });
    }

    const isMatch = await comparePassword(password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({ success: false, error: 'Invalid email or password' });
    }

    const studentRecord = db.students.find(s => s.user_id === user.id);
    const adminRecord = db.admins.find(a => a.user_id === user.id);

    const token = generateToken({
      id: user.id,
      name: user.name,
      email: user.email,
      role: user.role,
      studentId: studentRecord?.id || null,
      adminId: adminRecord?.id || null
    });

    return res.json({
      success: true,
      message: 'Login successful',
      token,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        role: user.role,
        studentId: studentRecord?.id || null,
        adminId: adminRecord?.id || null,
        semester: studentRecord?.semester || 1,
        department: studentRecord?.department || 'Computer Science'
      }
    });

  } catch (error) {
    next(error);
  }
};

export const getMe = async (req, res, next) => {
  try {
    const db = getDB();
    const user = db.users.find(u => u.id === req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, error: 'User profile not found' });
    }

    const studentRecord = db.students.find(s => s.user_id === user.id);
    const adminRecord = db.admins.find(a => a.user_id === user.id);

    return res.json({
      success: true,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        role: user.role,
        studentId: studentRecord?.id || null,
        adminId: adminRecord?.id || null,
        studentCode: studentRecord?.student_code || null,
        semester: studentRecord?.semester || 1,
        department: studentRecord?.department || 'Computer Science',
        gpa: studentRecord?.gpa || 3.8
      }
    });

  } catch (error) {
    next(error);
  }
};
