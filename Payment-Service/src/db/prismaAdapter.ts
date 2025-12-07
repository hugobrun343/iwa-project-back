import { PrismaClient } from '@prisma/client';

/**
 * prismaAdapter.ts
 * Minimal adapter using Prisma to provide the same small API surface as the
 * in-memory DB / pgAdapter used by the backend. Intended for local testing
 * with Postgres created by docker-compose.
 */

const prisma = new PrismaClient();

export const TEST_GUARDIAN_EMAIL = 'test@gmail.com';

export const paymentsDB = {
  // Payments are NOT persisted in the database and are authoritative on Stripe.
  // To preserve the existing adapter API shape we expose the same async methods
  // but they are no-ops (do not store anything) — callers should rely on Stripe
  // for payment/transfer history. This keeps the server behavior stable while
  // ensuring no Payment/Transfer tables exist in Prisma/Postgres.

  async create(_payment: any) {
    // Intentionally do not persist. Return null to indicate no local record.
    return null;
  },

  async findByStripeId(_stripePaymentId?: string) {
    return undefined;
  },

  async findByGuardianId(_guardianId?: string) {
    return [] as any[];
  },

  async update(_id: number, _updates: any) {
    return null;
  },

  async updateByStripeId(_stripePaymentId?: string, _updates?: any) {
    return null;
  }
};

export const transfersDB = {
  // No persistent or in-memory transfer storage. Transfers are authoritative on Stripe
  // (and in production would be retrieved via `stripe.transfers`). We expose the
  // same API surface but do not keep any local state.
  async create(_transfer: any) {
    return null;
  },

  async findByGuardianId(_guardianId?: string) {
    return [] as any[];
  }
};

/* Repository functions for the four persisted Prisma models
 * - Utilisateur
 * - Annonce
 * - UtilisateurStripe
 * - TransactionAnnonce
 *
 * These provide the minimal operations that the service layer needs to
 * persist domain information linked to Stripe ids.
 */
export const UtilisateurRepo = {
  async create(data: { username: string; email?: string; nom?: string; prenom?: string }) {
    return prisma.utilisateur.create({ data });
  },
  async findById(id: number) {
    return prisma.utilisateur.findUnique({ where: { id } });
  },
  async findByEmail(email: string) {
    return prisma.utilisateur.findUnique({ where: { email } });
  },
  async update(id: number, updates: any) {
    return prisma.utilisateur.update({ where: { id }, data: updates });
  }
};

export const UtilisateurStripeRepo = {
  async upsert(utilisateurId: number, stripeCustomerId: string) {
    return prisma.utilisateurStripe.upsert({
      where: { utilisateurId },
      update: { stripeCustomerId },
      create: { utilisateurId, stripeCustomerId },
    });
  },
  async findByStripeCustomerId(stripeCustomerId: string) {
    return prisma.utilisateurStripe.findUnique({ where: { stripeCustomerId } });
  },
  async findByUserId(utilisateurId: number) {
    return prisma.utilisateurStripe.findUnique({ where: { utilisateurId } });
  }
};

export const AnnonceRepo = {
  async create(data: any) {
    return prisma.annonce.create({ data });
  },
  async findById(id: number) {
    return prisma.annonce.findUnique({ where: { id } });
  },
  async update(id: number, updates: any) {
    return prisma.annonce.update({ where: { id }, data: updates });
  }
};

export const TransactionAnnonceRepo = {
  async createTransaction(data: { stripeId: string; annonceId: number; transactionId?: string }) {
    // transactionId is required by the Prisma model; default to stripeId when not provided
    const payload = { stripeId: data.stripeId, annonceId: data.annonceId, transactionId: data.transactionId ?? data.stripeId };
    return prisma.transactionAnnonce.create({ data: payload });
  },
  async findByStripeId(stripeId: string) {
    // `stripeId` used to be unique; schema changed so search either by stripeId
    // (owner/customer id) or by transactionId (paymentIntent id) and return the
    // first matching row. This preserves backwards-compatibility with callers
    // that previously passed a paymentId.
    return prisma.transactionAnnonce.findFirst({ where: { OR: [{ stripeId }, { transactionId: stripeId }] } });
  },
  async findByOwnerStripeId(stripeId: string) {
    return prisma.transactionAnnonce.findMany({ where: { stripeId } });
  },
  async updateByStripeId(stripeId: string, updates: any) {
    // Find the existing mapping either by owner stripeId or by transactionId,
    // then update by primary key (id) because stripeId/transactionId are no
    // longer guaranteed to be unique in the schema.
    const existing = await prisma.transactionAnnonce.findFirst({ where: { OR: [{ stripeId }, { transactionId: stripeId }] } });
    if (!existing) return null;
    return prisma.transactionAnnonce.update({ where: { id: existing.id }, data: updates });
  },
  async findByAnnonceId(annonceId: number) {
    return prisma.transactionAnnonce.findMany({ where: { annonceId } });
  }
};


export function generateSimulatedTransferId(): string {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substr(2, 9);
  return `sim_tr_${timestamp}_${random}`;
}

// Init (ensure client is connected) — useful for logs
async function init() {
  try {
    await prisma.$connect();
    // no schema push here; the developer should run `npx prisma db push` or migration
    // eslint-disable-next-line no-console
    console.log('Prisma adapter connected to database');
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error('Prisma adapter failed to connect', err);
  }
}

init();

export default {
  paymentsDB,
  transfersDB,
  generateSimulatedTransferId,
  TEST_GUARDIAN_EMAIL,
  prisma,
};

// Also export repos for explicit use by services
export const repos = {
  UtilisateurRepo,
  UtilisateurStripeRepo,
  AnnonceRepo,
  TransactionAnnonceRepo,
};
