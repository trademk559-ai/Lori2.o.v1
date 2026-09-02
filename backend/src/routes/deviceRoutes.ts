import { Router, Response } from 'express';
import { z } from 'zod';
import { query } from '../db';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth';

const router = Router();

const deviceRegisterSchema = z.object({
  deviceUid: z.string().min(1),
  deviceName: z.string().min(1),
  deviceType: z.enum(['android', 'web', 'desktop']),
  pushToken: z.string().optional()
});

// Register or update device
router.post('/register', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const validated = deviceRegisterSchema.parse(req.body);
    const userId = req.user!.userId;

    const result = await query(
      `INSERT INTO devices (user_id, device_uid, device_name, device_type, push_token, last_seen)
       VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP)
       ON CONFLICT (device_uid) DO UPDATE
       SET device_name = EXCLUDED.device_name,
           push_token = EXCLUDED.push_token,
           last_seen = CURRENT_TIMESTAMP
       RETURNING id, device_uid, device_name, device_type, is_authorized, last_seen`,
      [userId, validated.deviceUid, validated.deviceName, validated.deviceType, validated.pushToken || null]
    );

    return res.json({
      success: true,
      device: result.rows[0]
    });
  } catch (err: any) {
    return res.status(400).json({ success: false, error: err.message || 'Failed to register device' });
  }
});

// List authorized devices
router.get('/', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const result = await query(
      `SELECT id, device_uid, device_name, device_type, is_authorized, last_seen, created_at
       FROM devices
       WHERE user_id = $1
       ORDER BY last_seen DESC`,
      [req.user!.userId]
    );

    return res.json({
      success: true,
      devices: result.rows
    });
  } catch (err: any) {
    return res.status(500).json({ success: false, error: 'Failed to fetch devices' });
  }
});

// Revoke a device
router.delete('/:deviceId', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { deviceId } = req.params;
    await query(
      'DELETE FROM devices WHERE id = $1 AND user_id = $2',
      [deviceId, req.user!.userId]
    );
    return res.json({ success: true, message: 'Device revoked successfully' });
  } catch (err: any) {
    return res.status(500).json({ success: false, error: 'Failed to revoke device' });
  }
});

export default router;
