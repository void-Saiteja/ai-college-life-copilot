/**
 * Lightweight, in-memory, production-safe rate limiter middleware
 */

export function createRateLimiter({
  windowMs = 60 * 1000,
  maxRequests = 120,
  message = 'Too many requests. Please try again later.'
} = {}) {
  const requestCounts = new Map();

  // Periodic cleanup every 2 minutes
  const cleanupInterval = setInterval(() => {
    const now = Date.now();
    for (const [key, record] of requestCounts.entries()) {
      if (now - record.startTime > windowMs) {
        requestCounts.delete(key);
      }
    }
  }, 2 * 60 * 1000);

  if (cleanupInterval.unref) {
    cleanupInterval.unref();
  }

  return (req, res, next) => {
    // Determine client identifier (IP or user ID if authenticated)
    const clientKey = req.user?.id || req.ip || req.headers['x-forwarded-for'] || req.socket.remoteAddress || 'unknown';
    const now = Date.now();

    const record = requestCounts.get(clientKey);

    if (!record) {
      requestCounts.set(clientKey, { count: 1, startTime: now });
      return next();
    }

    if (now - record.startTime < windowMs) {
      record.count += 1;
      if (record.count > maxRequests) {
        res.setHeader('Retry-After', Math.ceil((windowMs - (now - record.startTime)) / 1000));
        return res.status(429).json({
          success: false,
          error: message
        });
      }
      return next();
    }

    // Window elapsed, reset counter
    record.count = 1;
    record.startTime = now;
    next();
  };
}
