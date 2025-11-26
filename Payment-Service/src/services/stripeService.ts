import Stripe from 'stripe';

/**
 * stripeService.ts
 * Encapsulates calls to Stripe and coordinates with the DB adapter (prismaAdapter)
 * This file provides a thin service layer used by controllers.
 *
 * Design choices:
 * - Methods return simple JS objects (no Express Response) so they are testable.
 * - The service uses the adapter exported by prismaAdapter (paymentsDB, transfersDB)
 */

const stripeSecret = process.env.STRIPE_SECRET_KEY || 'sk_test_YOUR_SECRET_KEY';
const stripe = new Stripe(stripeSecret, { apiVersion: '2023-10-16' });

// Adapter is required dynamically so tests can mock it if needed
// eslint-disable-next-line @typescript-eslint/no-var-requires
const adapter = require('../db/prismaAdapter');

const { repos, generateSimulatedTransferId, TEST_GUARDIAN_EMAIL } = adapter;
const { TransactionAnnonceRepo, UtilisateurStripeRepo, AnnonceRepo, UtilisateurRepo } = repos;

type PayoutSummary = {
  paymentId: string;
  jobId?: number | null;
  amount: number | null;
  currency: string;
  status: string;
  created: string | null;
  source?: 'job_payment' | 'subscription';
  invoiceId?: string | null;
  subscriptionId?: string | null;
};

export async function getConfig() {
  return { publishableKey: process.env.STRIPE_PUBLISHABLE_KEY || 'pk_test_YOUR_PUBLISHABLE_KEY' };
}

export async function createCustomer(email: string) {
  const customer = await stripe.customers.create({ email });
  return { customerId: customer.id, email: customer.email };
}

export async function getCustomerInfo(customerId: string) {
  const customer = await stripe.customers.retrieve(customerId) as Stripe.Customer;
  const subs = await stripe.subscriptions.list({ customer: customerId, status: 'all', limit: 5, expand: ['data.items.data.price'] });
  const activeLike = subs.data.find((s: Stripe.Subscription) => ['active', 'trialing', 'past_due', 'incomplete'].includes(s.status));
  const activeSubscription = activeLike
    ? {
      id: activeLike.id,
      status: activeLike.status,
      priceId: activeLike.items?.data?.[0]?.price?.id,
      cancelAtPeriodEnd: activeLike.cancel_at_period_end,
      currentPeriodEnd: activeLike.current_period_end,
    }
    : null;

  return {
    customer: { id: (customer as any).id, email: (customer as any).email },
    email: (customer as any).email,
    activeSubscription,
    activePriceId: activeSubscription?.priceId || null,
  };
}

export async function listPrices() {
  const prices = await stripe.prices.list({ active: true, expand: ['data.product'] });
  return { prices: prices.data };
}

export async function createSubscription(customerId: string, priceId: string) {
  const subscription = await stripe.subscriptions.create({
    customer: customerId,
    items: [{ price: priceId }],
    payment_behavior: 'default_incomplete',
    payment_settings: {
      payment_method_types: ['card'],
      save_default_payment_method: 'on_subscription',
    },
    expand: ['latest_invoice.payment_intent'],
  });

  let paymentIntent: Stripe.PaymentIntent | undefined;
  const latestInvoice = subscription.latest_invoice as Stripe.Invoice | string | undefined;
  if (latestInvoice && typeof latestInvoice !== 'string') {
    const pi = (latestInvoice as any).payment_intent;
    if (pi && typeof pi !== 'string') {
      paymentIntent = pi as Stripe.PaymentIntent;
    }
  }

  return { subscriptionId: subscription.id, clientSecret: paymentIntent?.client_secret };
}

export async function cancelSubscription(subscriptionId: string, atPeriodEnd = true) {
  if (atPeriodEnd) {
    const result = await stripe.subscriptions.update(subscriptionId, { cancel_at_period_end: true });
    return {
      subscriptionId: result.id,
      status: result.status,
      cancelAtPeriodEnd: result.cancel_at_period_end,
      currentPeriodEnd: result.current_period_end,
      canceledAt: result.canceled_at || null,
    };
  }
  const result = await stripe.subscriptions.cancel(subscriptionId);
  return {
    subscriptionId: result.id,
    status: result.status,
    cancelAtPeriodEnd: result.cancel_at_period_end,
    currentPeriodEnd: result.current_period_end,
    canceledAt: result.canceled_at || null,
  };
}

export async function createPaymentIntent(amount: number, currency = 'eur', customerId?: string) {
  const paymentIntent = await stripe.paymentIntents.create({
    amount: Math.round(amount * 100),
    currency,
    customer: customerId,
    payment_method_types: ['card'],
  });
  return { clientSecret: paymentIntent.client_secret, paymentIntentId: paymentIntent.id };
}

export async function capturePayment(paymentIntentId: string) {
  const paymentIntent = await stripe.paymentIntents.capture(paymentIntentId);
  return { status: paymentIntent.status, captured: paymentIntent.amount_received };
}

export async function refundPayment(paymentIntentId: string) {
  const refund = await stripe.refunds.create({ payment_intent: paymentIntentId });
  return { refundId: refund.id, status: refund.status, amount: refund.amount };
}

export async function createJobPayment(jobId: string, amountCents: number, customerId?: string, guardianId?: string) {
  const paymentIntent = await stripe.paymentIntents.create({
    amount: Math.round(amountCents), // amount already in cents
    currency: 'eur',
    capture_method: 'automatic',
    customer: customerId,
    metadata: { jobId: jobId, guardianId: guardianId || '', type: 'job_payment' },
    payment_method_types: ['card'],
  });

  // Persist a minimal mapping between the job (annonce) and the Stripe payment
  try {
    // Store the payer's Stripe customer id in `stripeId` so it's easy to find which
    // user (owner) initiated the payment. Fall back to paymentIntent.customer
    // (if provided by Stripe) and as a last resort store the paymentIntent id.
    const ownerStripeId = customerId || (typeof paymentIntent.customer === 'string' ? paymentIntent.customer : undefined) || paymentIntent.id;
    console.log('createJobPayment persisting TransactionAnnonce with', { ownerStripeId, annonceId: Number(jobId) || 0, transactionId: paymentIntent.id });
    await TransactionAnnonceRepo.createTransaction({ stripeId: ownerStripeId, annonceId: Number(jobId) || 0, transactionId: paymentIntent.id });
  } catch (err) {
    // If DB persist fails, we still return the payment info — caller can retry persistence later
    // eslint-disable-next-line no-console
    console.error('Failed to persist TransactionAnnonce mapping', err);
  }

  return { paymentId: paymentIntent.id, clientSecret: paymentIntent.client_secret };
}

export async function getPayment(paymentId: string) {
  const paymentIntent = await stripe.paymentIntents.retrieve(paymentId) as Stripe.PaymentIntent;
  const dbPayment = await TransactionAnnonceRepo.findByStripeId(paymentId);
  return {
    paymentId: paymentIntent.id,
    status: paymentIntent.status,
    amount: paymentIntent.amount,
    currency: paymentIntent.currency,
    jobId: paymentIntent.metadata?.jobId,
    guardianId: paymentIntent.metadata?.guardianId,
    captured: (paymentIntent.amount_received ?? 0) > 0,
    dbMapping: dbPayment || null,
  };
}

export async function releasePaymentToGuardian(paymentId: string) {
  const paymentIntent = await stripe.paymentIntents.retrieve(paymentId) as Stripe.PaymentIntent;
  if (paymentIntent.status !== 'succeeded') throw new Error('Le paiement doit être succeeded pour transférer les fonds');
  const guardianId = paymentIntent.metadata?.guardianId;
  if (!guardianId) throw new Error('guardianId manquant dans les metadata du paiement');

  return {
    status: 'transferred',
    transferId: undefined,
    guardianEmail: TEST_GUARDIAN_EMAIL,
    amount: paymentIntent.amount,
    message: `Paiement accepté pour transfert au gardien (simulation test; en production utiliser Stripe Connect)`,
    note: 'En production, utilisez stripe.transfers.create() avec un compte connecté réel',
  };
}

export async function listPayouts(userId?: string, limit = 10) {
  if (!userId) {
    const payouts = await stripe.payouts.list({ limit });
    return { payouts: payouts.data };
  }

  let resolvedStripeCustomerId: string | undefined = userId;

  // If a numeric user id is provided, attempt to resolve the Stripe customer id
  if (userId && !userId.startsWith('cus_')) {
    const parsed = Number(userId);
    if (!Number.isNaN(parsed)) {
      try {
        const userStripe = await UtilisateurStripeRepo.findByUserId(parsed);
        if (userStripe?.stripeCustomerId) {
          resolvedStripeCustomerId = userStripe.stripeCustomerId;
        }
      } catch (err) {
        console.warn('Failed to resolve Stripe customer for userId', userId, err);
      }
    }
  }

  if (!resolvedStripeCustomerId) {
    return { payouts: [], note: 'Unable to resolve Stripe customer for provided userId' };
  }

  let note: string | undefined;
  let jobPayouts: PayoutSummary[] = [];
  try {
    const txs = await TransactionAnnonceRepo.findByOwnerStripeId(resolvedStripeCustomerId);
    jobPayouts = await Promise.all(txs.map(async (t: any) => {
      try {
        const pi = await stripe.paymentIntents.retrieve(t.transactionId);
        let status: string = (pi as any).status || 'unknown';
        if (status === 'succeeded') {
          try {
            const refunds = await stripe.refunds.list({ payment_intent: t.transactionId, limit: 1 });
            if (refunds.data && refunds.data.length > 0) {
              status = 'refunded';
            }
          } catch (refundErr) {
            console.warn('Failed to check refunds for', t.transactionId, refundErr);
          }
        }
        return {
          paymentId: t.transactionId,
          jobId: t.annonceId,
          amount: typeof (pi as any).amount === 'number' ? (pi as any).amount : null,
          currency: (pi as any).currency || 'eur',
          status,
          created: (pi as any).created ? new Date(((pi as any).created) * 1000).toISOString() : null,
          source: 'job_payment' as const,
        };
      } catch (err) {
        return { paymentId: t.transactionId, jobId: t.annonceId, amount: null, currency: 'eur', status: 'unknown', created: null, source: 'job_payment' as const };
      }
    }));
  } catch (err: any) {
    note = 'Error querying local transactions';
    console.error('Transaction lookup failed for payouts', err);
  }

  const { payouts: subscriptionPayouts, note: subscriptionNote } = await collectSubscriptionPayouts(resolvedStripeCustomerId, limit);
  if (subscriptionNote) {
    note = note ? `${note}; ${subscriptionNote}` : subscriptionNote;
  }

  const payouts = mergePayouts(jobPayouts, subscriptionPayouts);

  return note ? { payouts, note } : { payouts };
}

function mergePayouts(jobPayouts: PayoutSummary[], subscriptionPayouts: PayoutSummary[]): PayoutSummary[] {
  const merged: PayoutSummary[] = [];
  const seen = new Set<string>();

  [...jobPayouts, ...subscriptionPayouts].forEach((payout) => {
    if (payout.paymentId && seen.has(payout.paymentId)) return;
    if (payout.paymentId) seen.add(payout.paymentId);
    merged.push(payout);
  });

  return merged;
}

async function collectSubscriptionPayouts(customerId: string, limit: number): Promise<{ payouts: PayoutSummary[]; note?: string }> {
  try {
    const invoices = await stripe.invoices.list({
      customer: customerId,
      limit,
      expand: ['data.payment_intent'],
    });

    const payouts: PayoutSummary[] = invoices.data
      .filter((invoice: Stripe.Invoice) => Boolean(invoice.subscription))
      .map((invoice: Stripe.Invoice) => {
        const paymentIntent = typeof invoice.payment_intent === 'string' ? null : invoice.payment_intent as Stripe.PaymentIntent | null;
        const createdSeconds = paymentIntent?.created ?? invoice.created ?? null;
        const status = paymentIntent?.status || invoice.status || 'unknown';
        const amount = paymentIntent?.amount_received ?? invoice.amount_paid ?? paymentIntent?.amount ?? null;

        return {
          paymentId: paymentIntent?.id ?? invoice.id,
          jobId: null,
          amount: typeof amount === 'number' ? amount : null,
          currency: paymentIntent?.currency ?? invoice.currency ?? 'eur',
          status: typeof status === 'string' ? status : 'unknown',
          created: createdSeconds ? new Date(createdSeconds * 1000).toISOString() : null,
          source: 'subscription',
          invoiceId: invoice.id,
          subscriptionId: typeof invoice.subscription === 'string' ? invoice.subscription : invoice.subscription?.id ?? null,
        };
      });

    return { payouts };
  } catch (err) {
    console.warn('Failed to collect subscription payouts for customer', customerId, err);
    return { payouts: [], note: 'Unable to retrieve subscription payouts from Stripe' };
  }
}

export async function listTransfersForGuardian(guardianId: string, limit = 10) {
  try {
    // Attempt to fetch transfers from Stripe and filter by metadata.guardianId when available
    const stripeTransfers = await stripe.transfers.list({ limit });
    const filtered = stripeTransfers.data.filter((t: any) => (t.metadata as any)?.guardianId === guardianId);
  const formatted = filtered.map((t: any) => ({ transferId: (t as any).id, amount: (t as any).amount, currency: ((t as any).currency ?? 'eur'), created: new Date(((t as any).created ?? 0) * 1000).toISOString(), status: (t as any).status, metadata: (t as any).metadata }));
    return { guardianId, email: TEST_GUARDIAN_EMAIL, transfers: formatted };
  } catch (err) {
    // Fallback: no local persistence available
    return { guardianId, email: TEST_GUARDIAN_EMAIL, transfers: [], note: 'Unable to fetch transfers from Stripe in this environment' };
  }
}

export async function getUserPayments(userId?: string, customerId?: string, limit = 100) {
  try {
    // If customerId is provided, use it directly
    // Otherwise, try to find the Stripe customer ID from the userId
    let stripeCustomerId = customerId;
    
    if (!stripeCustomerId && userId) {
      // Try to find the Stripe customer ID from the database
      try {
        const userStripe = await UtilisateurStripeRepo.findByUserId(Number(userId));
        if (userStripe) {
          stripeCustomerId = userStripe.stripeCustomerId;
        }
      } catch (err) {
        // If we can't find it in DB, we'll query by userId as stripeId in TransactionAnnonce
        console.log('Could not find Stripe customer for userId, will query by TransactionAnnonce');
      }
    }

    let payments: Stripe.PaymentIntent[] = [];
    const paymentIntentMap = new Map<string, Stripe.PaymentIntent>();

    if (stripeCustomerId) {
      // Query Stripe directly by customer ID for PaymentIntents
      const paymentIntents = await stripe.paymentIntents.list({
        customer: stripeCustomerId,
        limit,
      });
      paymentIntents.data.forEach((pi: any) => paymentIntentMap.set(pi.id, pi));

      // Also get payments from subscription invoices
      try {
        const invoices = await stripe.invoices.list({
          customer: stripeCustomerId,
          limit,
          expand: ['data.payment_intent'],
        });

        invoices.data.forEach((invoice: any) => {
          const paymentIntent = invoice.payment_intent;
          if (paymentIntent && typeof paymentIntent !== 'string') {
            const pi = paymentIntent as Stripe.PaymentIntent;
            // Only add if not already in map (avoid duplicates)
            if (!paymentIntentMap.has(pi.id)) {
              paymentIntentMap.set(pi.id, pi);
            }
          }
        });
      } catch (invoiceErr) {
        console.warn('Failed to retrieve invoices for customer', stripeCustomerId, invoiceErr);
      }

      payments = Array.from(paymentIntentMap.values());
    } else if (userId) {
      // Query by TransactionAnnonce to find payments linked to this user
      try {
        const txs = await TransactionAnnonceRepo.findByOwnerStripeId(userId);
        const paymentIds = txs.map((t: any) => t.transactionId);
        
        // Fetch payment details from Stripe
        payments = await Promise.all(
          paymentIds.map(async (paymentId: string) => {
            try {
              return await stripe.paymentIntents.retrieve(paymentId);
            } catch (err) {
              console.warn(`Failed to retrieve payment ${paymentId}`, err);
              return null;
            }
          })
        );
        payments = payments.filter((p): p is Stripe.PaymentIntent => p !== null);
      } catch (err) {
        console.error('Error querying TransactionAnnonce', err);
      }
    } else {
      throw new Error('Either userId or customerId must be provided');
    }

    // Format the payments
    const formatted = await Promise.all(
      payments.map(async (pi) => {
        let status: string = pi.status;
        
        // Check if payment has been refunded
        if (status === 'succeeded') {
          try {
            const refunds = await stripe.refunds.list({ payment_intent: pi.id, limit: 1 });
            if (refunds.data && refunds.data.length > 0) {
              status = 'refunded';
            }
          } catch (refundErr) {
            // Keep original status if refund check fails
            console.warn('Failed to check refunds for', pi.id, refundErr);
          }
        }

        return {
          paymentId: pi.id,
          amount: pi.amount,
          currency: pi.currency,
          status,
          created: new Date(pi.created * 1000).toISOString(),
          jobId: pi.metadata?.jobId,
          guardianId: pi.metadata?.guardianId,
          customerId: typeof pi.customer === 'string' ? pi.customer : pi.customer?.id,
          captured: (pi.amount_received ?? 0) > 0,
          description: pi.description,
          type: pi.metadata?.type || (pi.description?.toLowerCase().includes('subscription') ? 'subscription' : 'payment'),
        };
      })
    );

    return { payments: formatted, count: formatted.length };
  } catch (err: any) {
    console.error('Error in getUserPayments', err);
    throw new Error(`Failed to retrieve user payments: ${err.message}`);
  }
}

export default {
  getConfig,
  createCustomer,
  getCustomerInfo,
  listPrices,
  createSubscription,
  cancelSubscription,
  createPaymentIntent,
  capturePayment,
  refundPayment,
  createJobPayment,
  getPayment,
  releasePaymentToGuardian,
  listPayouts,
  listTransfersForGuardian,
  getUserPayments,
};
