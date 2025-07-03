import { Router } from 'express';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();
const router = Router();

router.get('/', async (req, res) => {
  const invoices = await prisma.invoice.findMany();
  res.json(invoices);
});

router.get('/stats', async (req, res) => {
  const invoices = await prisma.invoice.findMany();

  const total = invoices.reduce((sum, inv) => sum + inv.amount, 0);
  const avg = invoices.length > 0 ? total / invoices.length : 0;
  const count = invoices.length;

  res.json({
    total,
    average: avg,
    count,
  });
});

export default router;

