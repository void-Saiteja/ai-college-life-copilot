export const getHealthStatus = (req, res) => {
  res.status(200).json({
    success: true,
    status: 'OK',
    message: 'AI College Life Copilot Backend API is running smoothly.',
    timestamp: new Date().toISOString(),
    environment: process.env.NODE_ENV || 'development',
    version: '1.0.0'
  });
};
