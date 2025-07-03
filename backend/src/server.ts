import express from 'express';
import invoiceRoutes from './routes/invoice.js';

const app = express();
app.use(express.json());

app.use('/api/invoices', invoiceRoutes);

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Server running at http://localhost:${PORT}`);
});
