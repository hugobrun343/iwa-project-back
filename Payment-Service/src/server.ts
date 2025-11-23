import express, { Request, Response } from 'express';
import cors from 'cors';
import dotenv from 'dotenv';

// Use Prisma adapter only (option A). If it fails to load, exit so devs notice
let paymentsDB: any;
let transfersDB: any;
let generateSimulatedTransferId: any;
let TEST_GUARDIAN_EMAIL: any;

// Load env early so adapters see DATABASE_URL, STRIPE keys, etc.
dotenv.config();

try {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const adapter = require('./db/prismaAdapter');
  paymentsDB = adapter.paymentsDB;
  transfersDB = adapter.transfersDB;
  generateSimulatedTransferId = adapter.generateSimulatedTransferId;
  TEST_GUARDIAN_EMAIL = adapter.TEST_GUARDIAN_EMAIL;
  // eslint-disable-next-line no-console
  console.log('Using Prisma adapter for DB operations');
} catch (err) {
  // eslint-disable-next-line no-console
  console.error('Failed to load Prisma adapter. Ensure @prisma/client is installed and `prisma generate`/`prisma db push` have been run.', err);
  process.exit(1);
}

const app = express();
// Ensure PORT is a number for TypeScript and for app.listen overloads
const PORT = Number(process.env.PORT) || 8088;

// Log DB URL presence for Docker verification
const configuredDbUrl = process.env.DATABASE_URL;
if (configuredDbUrl) {
  // eslint-disable-next-line no-console
  console.log('DATABASE_URL found (will be used by the real DB adapter):', configuredDbUrl.startsWith('postgres') ? 'postgres://***' : configuredDbUrl);
} else {
  // eslint-disable-next-line no-console
  console.warn('No DATABASE_URL found — backend will use in-memory DB unless you wire a real DB adapter.');
}

// Middleware
app.use(cors());
app.use(express.json());

// Mount controllers
import * as stripeController from './controllers/stripeController';

// Base route prefix for all payment endpoints
const BASE_ROUTE = '/api/payments';

// All routes are prefixed with /api/payments
app.get(`${BASE_ROUTE}/config`, stripeController.config);
app.post(`${BASE_ROUTE}/create-customer`, stripeController.createCustomer);
app.get(`${BASE_ROUTE}/customer/:customerId`, stripeController.getCustomer);
app.get(`${BASE_ROUTE}/prices`, stripeController.prices);
app.post(`${BASE_ROUTE}/create-subscription`, stripeController.createSubscription);
app.post(`${BASE_ROUTE}/cancel-subscription`, stripeController.cancelSubscription);
app.post(`${BASE_ROUTE}/create-payment-intent`, stripeController.createPaymentIntent);
app.post(`${BASE_ROUTE}/release-payment`, stripeController.releasePayment);
app.post(`${BASE_ROUTE}/refund-payment`, stripeController.refundPayment);
app.post(`${BASE_ROUTE}/release`, stripeController.releasePayment);
app.post(`${BASE_ROUTE}/create-hold`, stripeController.createJobPayment);
app.post(`${BASE_ROUTE}/refund`, stripeController.refundPayment);
app.get(`${BASE_ROUTE}/payouts`, stripeController.listPayouts);
app.get(`${BASE_ROUTE}/user/payments`, stripeController.getUserPayments);
app.get(`${BASE_ROUTE}/connect/transfers/:guardianId`, stripeController.listTransfersForGuardian);
// This route with parameter must be last to avoid conflicts with specific routes
app.get(`${BASE_ROUTE}/:paymentId`, stripeController.getPayment);

// ============================================
// ENDPOINT 1: Configuration Stripe (clé publique)
// ============================================
// NOTE: Routes have been moved to controllers under `src/controllers/stripeController.ts`.
// The controllers are mounted above. Keep this file minimal.

// Route de test
app.get('/', (_req: Request, res: Response) => {
  res.json({
    message: 'Backend Stripe opérationnel - Transferts simulés pour tests',
    note: 'Pour utiliser Stripe Connect en prod, créez des comptes connectés via Dashboard',
  });
});

// Bind to 0.0.0.0 to ensure the server is reachable from host / containers / devices
app.listen(PORT, '0.0.0.0', () => {
  // eslint-disable-next-line no-console
  console.log(`✅ Serveur Stripe démarré sur http://0.0.0.0:${PORT} (accessible via localhost:${PORT} depuis la machine hôte)`);
  console.log('📚 Documentation: http://localhost:' + PORT);
});
