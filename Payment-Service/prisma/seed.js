const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  console.log('Seeding minimal test data...');

  // Create two users and annonces, and map stripe customers
  const u1 = await prisma.utilisateur.upsert({
    where: { username: 'user_demo_1' },
    update: {},
    create: {
      username: 'user_demo_1',
      email: 'demo1@example.com',
      nom: 'Demo',
      prenom: 'Un',
    }
  });

  const u2 = await prisma.utilisateur.upsert({
    where: { username: 'user_demo_2' },
    update: {},
    create: {
      username: 'user_demo_2',
      email: 'demo2@example.com',
      nom: 'Demo',
      prenom: 'Deux',
    }
  });

  // Create annonces for each user
  await prisma.annonce.upsert({
    where: { id: 1 },
    update: {},
    create: {
      id: 1,
      proprietaireId: u1.id,
      titre: 'Garde de chat - 1',
      localisation: 'Paris',
      description: 'Prendre soin d\'un chat pendant le weekend',
      remuneration: 30.0,
    }
  });

  await prisma.annonce.upsert({
    where: { id: 2 },
    update: {},
    create: {
      id: 2,
      proprietaireId: u2.id,
      titre: 'Promenade chien - 2',
      localisation: 'Lyon',
      description: 'Promener un chien 30 minutes par jour',
      remuneration: 20.0,
    }
  });

  // Map stripe customers
  await prisma.utilisateurStripe.upsert({
    where: { utilisateurId: u1.id },
    update: { stripeCustomerId: 'cus_TKKneS1WyE9Wdr' },
    create: { utilisateurId: u1.id, stripeCustomerId: 'cus_TKKneS1WyE9Wdr' }
  });

  await prisma.utilisateurStripe.upsert({
    where: { utilisateurId: u2.id },
    update: { stripeCustomerId: 'cus_TP4nxK2F0PXVne' },
    create: { utilisateurId: u2.id, stripeCustomerId: 'cus_TP4nxK2F0PXVne' }
  });

  console.log('Seed complete.');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
