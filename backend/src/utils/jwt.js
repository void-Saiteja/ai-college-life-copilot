import jwt from 'jsonwebtoken';
import { config } from '../config/env.js';

export function generateToken(payload) {
  return jwt.sign(payload, config.jwtSecret, { algorithm: 'HS256', expiresIn: '7d' });
}

export function verifyToken(token) {
  try {
    return jwt.verify(token, config.jwtSecret, { algorithms: ['HS256'] });
  } catch (err) {
    return null;
  }
}
