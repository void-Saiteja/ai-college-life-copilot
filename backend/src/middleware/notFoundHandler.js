export const notFoundHandler = (req, res, next) => {
  res.status(404).json({
    success: false,
    error: 'Resource Not Found',
    message: `Cannot ${req.method} ${req.originalUrl}`
  });
};
