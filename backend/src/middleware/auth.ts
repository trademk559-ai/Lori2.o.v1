import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config';

export interface AuthenticatedRequest extends Request {
  user?: {
    userId: string;
    phoneNumber: string;
    role: string;
  };
}

export const authenticateToken = (
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.split(' ')[1] : null;

  if (!token) {
    return res.status(401).json({
      success: false,
      error: 'Access token required. Please log in.'
    });
  }

  try {
    const payload = jwt.verify(token, config.jwtSecret) as any;
    req.user = {
      userId: payload.userId,
      phoneNumber: payload.phoneNumber,
      role: payload.role || 'OWNER'
    };
    next();
  } catch (err) {
    return res.status(403).json({
      success: false,
      error: 'Invalid or expired session token.'
    });
  }
};
