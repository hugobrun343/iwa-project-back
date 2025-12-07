import { Request, Response } from 'express';
import * as stripeService from '../services/stripeService';

export async function config(req: Request, res: Response) {
  const cfg = await stripeService.getConfig();
  res.json(cfg);
}

export async function createCustomer(req: Request, res: Response) {
  try {
    const { email } = req.body;
    const result = await stripeService.createCustomer(email);
    res.json(result);
  } catch (err: any) {
    console.error('Error in createCustomer controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function getCustomer(req: Request, res: Response) {
  try {
    const { customerId } = req.params;
    const result = await stripeService.getCustomerInfo(customerId);
    res.json(result);
  } catch (err: any) {
    console.error('Error in getCustomer controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function prices(req: Request, res: Response) {
  try {
    const result = await stripeService.listPrices();
    res.json(result);
  } catch (err: any) {
    console.error('Error in prices controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function createSubscription(req: Request, res: Response) {
  try {
    const { customerId, priceId } = req.body;
    const result = await stripeService.createSubscription(customerId, priceId);
    res.json(result);
  } catch (err: any) {
    console.error('Error in createSubscription controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function cancelSubscription(req: Request, res: Response) {
  try {
    const { subscriptionId, atPeriodEnd = true } = req.body;
    const result = await stripeService.cancelSubscription(subscriptionId, atPeriodEnd);
    res.json(result);
  } catch (err: any) {
    console.error('Error in cancelSubscription controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function createPaymentIntent(req: Request, res: Response) {
  try {
    const { amount, currency, customerId } = req.body;
    const result = await stripeService.createPaymentIntent(amount, currency, customerId);
    res.json(result);
  } catch (err: any) {
    console.error('Error in createPaymentIntent controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function capturePayment(req: Request, res: Response) {
  try {
    const { paymentIntentId } = req.body;
    const result = await stripeService.capturePayment(paymentIntentId);
    res.json(result);
  } catch (err: any) {
    console.error('Error in capturePayment controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function refundPayment(req: Request, res: Response) {
  try {
    const { paymentIntentId, paymentId } = req.body as { paymentIntentId?: string; paymentId?: string };
    const id = paymentIntentId || paymentId;
    console.log('refundPayment controller called with', { paymentIntentId, paymentId, resolvedId: id });
    if (!id) return res.status(400).json({ error: 'Missing paymentIntentId or paymentId in request body' });
    const result = await stripeService.refundPayment(id);
    res.json(result);
  } catch (err: any) {
    console.error('Error in refundPayment controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function createJobPayment(req: Request, res: Response) {
  try {
    const { jobId, amount, customerId, guardianId } = req.body;
    const amountCents = Math.round(amount); // server expects cents for this helper
    const result = await stripeService.createJobPayment(jobId, amountCents, customerId, guardianId);
    res.json(result);
  } catch (err: any) {
    console.error('Error in createJobPayment controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function getPayment(req: Request, res: Response) {
  try {
    const { paymentId } = req.params;
    const result = await stripeService.getPayment(paymentId);
    res.json(result);
  } catch (err: any) {
    console.error('Error in getPayment controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function releasePayment(req: Request, res: Response) {
  try {
    const { paymentId } = req.body as { paymentId?: string };
    console.log('releasePayment controller called with', { paymentId });
    if (!paymentId) return res.status(400).json({ error: 'Missing paymentId in request body' });
    const result = await stripeService.releasePaymentToGuardian(paymentId);
    res.json(result);
  } catch (err: any) {
    console.error('Error in releasePayment controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function listPayouts(req: Request, res: Response) {
  try {
    const { userId, limit = '10' } = req.query;
    const result = await stripeService.listPayouts(userId as string | undefined, parseInt(String(limit)));
    res.json(result);
  } catch (err: any) {
    console.error('Error in listPayouts controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function listTransfersForGuardian(req: Request, res: Response) {
  try {
    const { guardianId } = req.params;
    const { limit = '10' } = req.query;
    const result = await stripeService.listTransfersForGuardian(guardianId, parseInt(String(limit)));
    res.json(result);
  } catch (err: any) {
    console.error('Error in listTransfersForGuardian controller', err);
    res.status(500).json({ error: err.message });
  }
}

export async function getUserPayments(req: Request, res: Response) {
  try {
    const { userId, customerId } = req.query;
    const { limit = '100' } = req.query;
    
    if (!userId && !customerId) {
      return res.status(400).json({ error: 'Either userId or customerId must be provided' });
    }
    
    const result = await stripeService.getUserPayments(
      userId as string | undefined,
      customerId as string | undefined,
      parseInt(String(limit))
    );
    res.json(result);
  } catch (err: any) {
    console.error('Error in getUserPayments controller', err);
    res.status(500).json({ error: err.message });
  }
}