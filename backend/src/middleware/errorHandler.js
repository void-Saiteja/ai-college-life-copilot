import { config } from '../config/env.js';

export const errorHandler = (err, req, res, next) => {
  console.error('[Error Handler]:', err.stack || err.message || err);

  const statusCode = err.statusCode || (res.statusCode && res.statusCode !== 200 ? res.statusCode : 500);

  // In production, mask internal server error messages to prevent leakage
  const safeMessage = (config.nodeEnv === 'production' && statusCode >= 500)
    ? 'An unexpected error occurred on the server.'
    : (err.message || 'An unexpected error occurred on the server.');

  res.status(statusCode).json({
    success: false,
    error: err.name || 'InternalServerError',
    message: safeMessage,
    stack: config.nodeEnv === 'development' ? err.stack : undefined
  });
};
