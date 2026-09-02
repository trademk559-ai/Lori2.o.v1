import { Router, Response } from 'express';
import { z } from 'zod';
import { query } from '../db';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth';

const router = Router();

const createMemorySchema = z.object({
  category: z.string().default('general'),
  content: z.string().min(1)
});

// Get user memories
router.get('/', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const result = await query(
      'SELECT id, category, content, created_at, updated_at FROM memories WHERE user_id = $1 ORDER BY created_at DESC',
      [req.user!.userId]
    );
    return res.json({ success: true, memories: result.rows });
  } catch (err: any) {
    return res.status(500).json({ success: false, error: 'Failed to fetch memories' });
  }
});

// Save new memory
router.post('/', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const validated = createMemorySchema.parse(req.body);
    const result = await query(
      `INSERT INTO memories (user_id, category, content)
       VALUES ($1, $2, $3)
       RETURNING id, category, content, created_at`,
      [req.user!.userId, validated.category, validated.content]
    );
    return res.status(201).json({ success: true, memory: result.rows[0] });
  } catch (err: any) {
    return res.status(400).json({ success: false, error: err.message || 'Failed to save memory' });
  }
});

// Delete memory by id
router.delete('/:id', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const { id } = req.params;
    await query('DELETE FROM memories WHERE id = $1 AND user_id = $2', [id, req.user!.userId]);
    return res.json({ success: true, message: 'Memory deleted' });
  } catch (err: any) {
    return res.status(500).json({ success: false, error: 'Failed to delete memory' });
  }
});

export default router;
