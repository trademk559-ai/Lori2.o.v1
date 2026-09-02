import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import { config } from './config';
import authRoutes from './routes/authRoutes';
import chatRoutes from './routes/chatRoutes';
import deviceRoutes from './routes/deviceRoutes';

const app = express();

// Security Middleware
app.use(helmet());
app.use(cors({
  origin: config.corsOrigin,
  credentials: true
}));
app.use(express.json({ limit: '5mb' }));

// Health Check
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    ecosystem: 'Lori Private AI Assistant',
    version: '1.0.0',
    timestamp: new Date().toISOString()
  });
});

// API Routes
app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/chat', chatRoutes);
app.use('/api/v1/devices', deviceRoutes);

// Global Error Handler
app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Unhandled server error:', err);
  res.status(500).json({
    success: false,
    error: 'Internal server error occurred in Lori ecosystem'
  });
});

const PORT = config.port;
app.listen(PORT, () => {
  console.log(`=========================================`);
  console.log(`  LORI PRIVATE AI BACKEND RUNNING`);
  console.log(`  Port: ${PORT}`);
  console.log(`  Environment: ${config.nodeEnv}`);
  console.log(`=========================================`);
});

export default app;
