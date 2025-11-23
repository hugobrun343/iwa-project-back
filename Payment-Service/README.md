# stripe-backend (TypeScript)

This backend was migrated to TypeScript and persists domain data using Prisma + Postgres when a DATABASE_URL is provided.
Payments and transfers are NOT persisted locally — Stripe is the source of truth for payment and transfer history.

Quick start (dev):

1. Copy `.env.example` to `.env` and fill your Stripe keys.
2. (Optional) If you want a local persisted database, set `DATABASE_URL` to point to a Postgres instance and run the Prisma steps described below. If you don't set DATABASE_URL the server will still run and Stripe remains the authoritative source for payments/transfers.

Note: Pour un guide complet (Prisma, Docker, seed, dépannage), consultez `../BACKEND_INTEGRATION.md` à la racine du projet. Pour exécuter automatiquement les étapes Prisma/seed dans un conteneur connecté au réseau compose, vous pouvez utiliser le script npm (depuis `stripe-backend`):

```bash
npm run prisma:container
```

Docker (compose) already provides a Postgres service and a default `DATABASE_URL`.

Important notes about persistence
- The project uses Prisma to persist domain models (Utilisateur, Annonce, UtilisateurStripe, TransactionAnnonce) when `DATABASE_URL` is provided.
- Payments and transfers are intentionally NOT stored in the database. All payment and transfer history must be queried from Stripe.
- Do NOT point your local `.env` to a shared production DB. If you do, be careful with your data and credentials.

Note about the minimal local mapping
- The backend does persist a minimal mapping record called `TransactionAnnonce` (Annonce ↔ Stripe id). This is only used to link a job (annonce) with the relevant Stripe identifiers (paymentIntent id, and optionally a transfer reference). It does NOT store full payment or transfer objects — Stripe remains the source of truth for amounts, statuses and history.

Commands:

```bash
# install deps
npm install

# dev (ts-node)
npm run dev

# build
npm run build

# run compiled
npm start
```

Docker:

```bash
docker compose up --build
```
# Backend Stripe pour mon-app-expo

Backend Node.js minimal pour gérer les opérations Stripe de façon sécurisée.

## 🚀 Installation

```bash
cd stripe-backend
npm install
```

## ⚙️ Configuration

1. Copie le fichier `.env.example` en `.env`:
   ```bash
   copy .env.example .env
   ```

2. Récupère tes clés Stripe sur [dashboard.stripe.com](https://dashboard.stripe.com/test/apikeys)

3. Édite `.env` et ajoute tes vraies clés:
   ```
   STRIPE_PUBLISHABLE_KEY=pk_test_...
   STRIPE_SECRET_KEY=sk_test_...
   ```

## 🎯 Démarrage

### Mode développement (avec auto-reload):
```bash
npm run dev
```

### Mode production:
```bash
npm start
```

Le serveur démarrera sur **http://localhost:4242**

## 📚 Endpoints disponibles

### Configuration & Clients
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/config` | Retourne la clé publique Stripe |
| POST | `/create-customer` | Crée un customer Stripe |
| GET | `/customer/:customerId` | Récupère les infos d'un customer (optionnel) |

### Abonnements
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/prices` | Liste tous les prix/plans actifs |
| POST | `/create-subscription` | Crée un abonnement et retourne éventuellement un clientSecret |
| POST | `/cancel-subscription` | Annule un abonnement |

### Paiements génériques
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/create-payment-intent` | Crée un PaymentIntent (paiement unique générique) |

### Paiements de jobs (capture automatique)
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/payments/create-hold` | Crée le paiement du job (capture immédiate: `capture_method: 'automatic'`) |
| GET | `/payments/:paymentId` | Récupère les infos d'un paiement |
| POST | `/payments/release` | Simule l'envoi des fonds au gardien (aucune persistance locale; Stripe est la source) |
| POST | `/payments/refund` | Annule (si non capturé) ou rembourse (si capturé) |

### Transferts & Payouts
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/connect/transfers/:guardianId` | Liste les transferts du gardien (dérivés de Stripe; aucune persistance locale) |
| GET | `/payouts` | Liste les payouts Stripe, ou filtre localement avec `?userId=...` (les données proviennent de Stripe) |

### Endpoints hérités (compat)
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/release-payment` | Capture directement un PaymentIntent (historique) |
| POST | `/refund-payment` | Rembourse un PaymentIntent par `paymentIntentId` (historique) |

## 🧪 Test

Ouvre http://localhost:4242 dans ton navigateur pour voir la liste des endpoints.

## 📱 Connexion avec l'app React Native

L'app mobile va automatiquement se connecter à `http://localhost:4242` pour récupérer la configuration Stripe.

### Sur émulateur Android:
- Utilise `http://10.0.2.2:4242` (IP de l'hôte depuis l'émulateur)

### Sur appareil physique:
- Trouve ton IP locale: `ipconfig` (Windows) ou `ifconfig` (Mac/Linux)
- Configure l'URL dans `src/config.ts` de l'app mobile

## 🔐 Sécurité

⚠️ **IMPORTANT**:
- Le fichier `.env` contient tes clés secrètes
- Ne JAMAIS commit `.env` sur Git
- Utilise uniquement les clés de test en développement
- En production, utilise des variables d'environnement sécurisées

## 📖 Documentation Stripe

- [Documentation Stripe API](https://stripe.com/docs/api)
- [Guide Mobile Payments](https://stripe.com/docs/payments/accept-a-payment?platform=react-native)
