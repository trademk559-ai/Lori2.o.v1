import { Router, Request, Response } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { z } from 'zod';
import { query } from '../db';
import { config } from '../config';
import { authRateLimiter } from '../middleware/rateLimiter';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth';

const router = Router();

const loginSchema = z.object({
  phoneNumber: z.string().min(5),
  password: z.string().min(6),
  deviceId: z.string().default('web-client')
});

const setupSchema = z.object({
  phoneNumber: z.string().min(5),
  password: z.string().min(6),
  pin: z.string().length(4).optional()
});

// Setup Master Single User (Only allowed if no user exists in the database)
router.post('/setup', authRateLimiter, async (req: Request, res: Response) => {
  try {
    const validated = setupSchema.parse(req.body);

    const userCountResult = await query('SELECT COUNT(*) as count FROM users');
    const count = parseInt(userCountResult.rows[0].count, 10);

    if (count > 0) {
      return res.status(403).json({
        success: false,
        error: 'Master user is already configured. Public registration is strictly disabled.'
      });
    }

    const salt = await bcrypt.genSalt(12);
    const passwordHash = await bcrypt.hash(validated.password, salt);
    const pinHash = validated.pin ? await bcrypt.hash(validated.pin, salt) : null;

    const insertResult = await query(
      `INSERT INTO users (phone_number, password_hash, pin_hash, role)
       VALUES ($1, $2, $3, 'OWNER')
       RETURNING id, phone_number, role, created_at`,
      [validated.phoneNumber, passwordHash, pinHash]
    );

    const newUser = insertResult.rows[0];

    // Create default assistant settings
    await query(
      `INSERT INTO assistant_settings (user_id) VALUES ($1)`,
      [newUser.id]
    );

    return res.status(201).json({
      success: true,
      message: 'Master Lori account successfully initialized.',
      user: {
        id: newUser.id,
        phoneNumber: newUser.phone_number,
        role: newUser.role
      }
    });
  } catch (err: any) {
    return res.status(400).json({
      success: false,
      error: err.message || 'Setup initialization failed'
    });
  }
});

// Single-User Login
router.post('/login', authRateLimiter, async (req: Request, res: Response) => {
  try {
    const validated = loginSchema.parse(req.body);

    const userResult = await query(
      'SELECT id, phone_number, password_hash, role, is_active FROM users WHERE phone_number = $1',
      [validated.phoneNumber]
    );

    if (userResult.rows.length === 0) {
      return res.status(401).json({
        success: false,
        error: 'Invalid credentials or user not authorized.'
      });
    }

    const user = userResult.rows[0];
    if (!user.is_active) {
      return res.status(403).json({
        success: false,
        error: 'Account has been deactivated.'
      });
    }

    const isMatch = await bcrypt.compare(validated.password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({
        success: false,
        error: 'Invalid credentials or user not authorized.'
      });
    }

    // Generate Tokens
    const accessToken = jwt.sign(
      { userId: user.id, phoneNumber: user.phone_number, role: user.role },
      config.jwtSecret,
      { expiresIn: config.accessTokenExpiry }
    );

    const refreshToken = jwt.sign(
      { userId: user.id, deviceId: validated.deviceId },
      config.jwtRefreshSecret,
      { expiresIn: config.refreshTokenExpiry }
    );

    const refreshHash = await bcrypt.hash(refreshToken, 10);
    const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);

    // Store Session
    await query(
      `INSERT INTO sessions (user_id, device_id, refresh_token_hash, ip_address, user_agent, expires_at)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [
        user.id,
        validated.deviceId,
        refreshHash,
        req.ip,
        req.headers['user-agent'] || '',
        expiresAt
      ]
    );

    return res.json({
      success: true,
      accessToken,
      refreshToken,
      user: {
        id: user.id,
        phoneNumber: user.phone_number,
        role: user.role
      }
    });
  } catch (err: any) {
    return res.status(400).json({
      success: false,
      error: err.message || 'Authentication failed'
    });
  }
});

// Refresh Access Token
router.post('/refresh', async (req: Request, res: Response) => {
  const { refreshToken, deviceId } = req.body;
  if (!refreshToken || !deviceId) {
    return res.status(400).json({ success: false, error: 'Refresh token and deviceId required' });
  }

  try {
    const payload = jwt.verify(refreshToken, config.jwtRefreshSecret) as any;
    const userResult = await query('SELECT id, phone_number, role FROM users WHERE id = $1', [payload.userId]);
    if (userResult.rows.length === 0) {
      return res.status(401).json({ success: false, error: 'User no longer exists' });
    }

    const user = userResult.rows[0];

    const newAccessToken = jwt.sign(
      { userId: user.id, phoneNumber: user.phone_number, role: user.role },
      config.jwtSecret,
      { expiresIn: config.accessTokenExpiry }
    );

    return res.json({
      success: true,
      accessToken: newAccessToken
    });
  } catch (err) {
    return res.status(403).json({ success: false, error: 'Invalid or expired refresh token' });
  }
});

// Logout
router.post('/logout', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    if (req.user) {
      await query('DELETE FROM sessions WHERE user_id = $1', [req.user.userId]);
    }
    return res.json({ success: true, message: 'Logged out successfully' });
  } catch (err) {
    return res.status(500).json({ success: false, error: 'Logout failed' });
  }
});

// Verify Current Token / Profile
router.get('/me', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const userResult = await query(
      'SELECT id, phone_number, role, created_at FROM users WHERE id = $1',
      [req.user?.userId]
    );
    if (userResult.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'User not found' });
    }
    return res.json({ success: true, user: userResult.rows[0] });
  } catch (err) {
    return res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

export default router;
