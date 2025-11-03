# 🧪 Collection Bruno - IWA Microservices

## 📦 Importer la Collection

1. Ouvrir **Bruno**
2. **Open Collection** → Sélectionner `/back/bruno-collection/`
3. Sélectionner l'environnement **"Local"** (dropdown en haut à droite)

---

## 🚀 Démarrage Rapide

### 1️⃣ Login
```
Keycloak/Login
```
→ Récupère et sauvegarde automatiquement le token dans `{{ACCESS_TOKEN}}`

### 2️⃣ Test Gateway (Public)
```
Gateway/Health Check
Gateway/Test Endpoint
Gateway/Get Languages
Gateway/Get Specialisations
```
→ Aucune auth requise

### 3️⃣ Routes Utilisateur (Protégées)
```
User/Get My Profile
User/Update Profile
User/Get My Languages
User/Update Languages
User/Get My Specialisations
User/Update Specialisations
User/Upload Photo Base64
User/Delete Photo
```
→ Requiert le token (automatique)

### 4️⃣ Routes Chat (Protégées)
```
Chat/Get My Discussions
Chat/Create Discussion
Chat/Get Discussion By Id
Chat/Get Messages
Chat/Create Message
```
→ Requiert le token (automatique)
→ Nécessite USER_ID dans les headers (défini dans l'environnement)

---

## 📂 Structure

```
bruno-collection/
├── Keycloak/          # Authentification
├── Gateway/           # Endpoints publics
├── User/              # User Service (protégé)
└── Chat/              # Chat Service (protégé)
```

---

## ⚙️ Configuration

Les variables sont dans `environments/Local.bru` :

```
GATEWAY_URL: http://localhost:8085
KEYCLOAK_URL: http://localhost:8080
KEYCLOAK_REALM: master
CLIENT_ID: admin-cli
USER_ID: usertest  # User ID from token (sub claim)
ACCESS_TOKEN: (auto-saved after login)
DISCUSSION_ID: (auto-saved after creating/getting discussion)
```

---

## 🔍 Voir les Logs

### Kibana (Recommandé)
```
http://localhost:5601
```
1. Créer data view: `microservices-logs*`
2. Discover → Voir tous les logs en temps réel

### Docker Logs
```bash
# Gateway
docker logs gateway-service -f

# User Service
docker logs user-service -f

# Chat Service
docker logs chat-service -f
```

---

## 🎯 Workflow Typique

### User Service
1. **Login** → `Keycloak/Login`
2. **Check Gateway** → `Gateway/Health Check`
3. **Test Public** → `Gateway/Get Languages`
4. **Get Profile** → `User/Get My Profile`
5. **Update** → `User/Update Profile`
6. **Vérifier logs** → Kibana

### Chat Service
1. **Login** → `Keycloak/Login` (sauvegarde automatiquement le token)
2. **Get My Discussions** → `Chat/Get My Discussions`
3. **Create Discussion** → `Chat/Create Discussion` (crée ou récupère existante)
4. **Get Messages** → `Chat/Get Messages` (liste les messages)
5. **Create Message** → `Chat/Create Message` (envoie un nouveau message)
6. **Get Discussion** → `Chat/Get Discussion By Id` (détails de la discussion)

---

## 🐛 Troubleshooting

### 401 Unauthorized
→ Token expiré ou invalide → Re-login

### 403 Forbidden
→ Permissions insuffisantes ou Gateway secret invalide

### Connection refused
→ Service down → `docker-compose up -d`

---

## 📊 Logs Générés

Chaque requête génère des logs détaillés visibles dans Kibana :
- Gateway: Authentication, routing, rate limiting
- User-Service: CRUD operations
- Chat-Service: Discussions et messages
- Tous envoyés automatiquement à Kafka → Elasticsearch → Kibana

## 💬 Notes sur Chat Service

- Les discussions sont liées à une annonce et deux utilisateurs
- `Create Discussion` crée une nouvelle discussion ou retourne l'existante
- `USER_ID` doit correspondre au `sub` du token JWT (utilisateur connecté)
- Les messages nécessitent que l'utilisateur soit participant à la discussion

🚀 **Prêt pour tester !**
