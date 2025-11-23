# API Endpoints - HomeGuard Backend

Documentation complète de tous les endpoints du backend pour la création des hooks React.

**Base URL Gateway**: `http://localhost:8080` (en développement)

> **Note**: Tous les endpoints nécessitent un token JWT dans le header `Authorization: Bearer <token>`. Le Gateway injecte automatiquement le header `X-Username` extrait du token.

---

## 📋 Table des matières

- [User Service](#user-service)
- [Announcement Service](#announcement-service)
- [Application Service](#application-service)
- [Chat Service](#chat-service)
- [Favorite Service](#favorite-service)
- [Rating Service](#rating-service)
- [WebSocket](#websocket)

---

## 👤 User Service

### Créer un profil utilisateur
**POST** `/api/users`

**Headers:**
- `X-Username`: string (auto par gateway)

**Body:**
```json
{
  "firstName": "string",
  "lastName": "string",
  "phoneNumber": "string (optionnel)",
  "location": "string (optionnel)",
  "description": "string (optionnel)",
  "profilePhoto": "string (optionnel)",
  "identityVerification": "string (optionnel)",
  "preferences": "object (optionnel)"
}
```

**Response:** `201 Created` - PrivateUserDTO
```json
{
  "username": "string",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "phoneNumber": "string",
  "location": "string",
  "description": "string",
  "profilePhoto": "string",
  "identityVerification": "string",
  "preferences": {},
  "registrationDate": "ISO-8601 datetime"
}
```

---

### Vérifier si un utilisateur existe
**GET** `/api/users/exists?username={username}`

**Query Params:**
- `username`: string (requis)

**Response:** `200 OK`
```json
{
  "username": "string",
  "exists": boolean
}
```

---

### Obtenir mon profil
**GET** `/api/users/me`

**Headers:**
- `X-Username`: string (auto par gateway)

**Alternative:** `/api/users/me?username={username}`

**Response:** `200 OK` - PrivateUserDTO (voir structure ci-dessus)

---

### Obtenir un profil utilisateur public
**GET** `/api/users/{username}`

**Path Params:**
- `username`: string

**Response:** `200 OK` - PublicUserDTO
```json
{
  "username": "string",
  "firstName": "string",
  "lastName": "string",
  "location": "string",
  "description": "string",
  "profilePhoto": "string",
  "identityVerification": "string",
  "registrationDate": "ISO-8601 datetime"
}
```

---

### Mettre à jour mon profil
**PATCH** `/api/users/me`

**Headers:**
- `X-Username`: string (auto par gateway)

**Body:** (tous les champs sont optionnels)
```json
{
  "firstName": "string",
  "lastName": "string",
  "phoneNumber": "string",
  "location": "string",
  "description": "string",
  "profilePhoto": "string",
  "identityVerification": "string",
  "preferences": {}
}
```

**Response:** `200 OK` - PrivateUserDTO

---

### Obtenir toutes les langues disponibles
**GET** `/api/languages`

**Response:** `200 OK`
```json
[
  {
    "label": "string"
  }
]
```

---

### Obtenir toutes les spécialisations disponibles
**GET** `/api/specialisations`

**Response:** `200 OK`
```json
[
  {
    "label": "string"
  }
]
```

---

### Obtenir mes langues
**GET** `/api/users/me/languages`

**Headers:**
- `X-Username`: string (auto par gateway)

**Response:** `200 OK`
```json
[
  {
    "label": "string"
  }
]
```

---

### Mettre à jour mes langues
**PATCH** `/api/users/me/languages`

**Headers:**
- `X-Username`: string (auto par gateway)

**Body:**
```json
{
  "languages": ["string", "string"]
}
```

**Response:** `200 OK` - Liste de UserLanguageDTO

---

### Obtenir mes spécialisations
**GET** `/api/users/me/specialisations`

**Headers:**
- `X-Username`: string (auto par gateway)

**Response:** `200 OK`
```json
[
  {
    "label": "string"
  }
]
```

---

### Mettre à jour mes spécialisations
**PATCH** `/api/users/me/specialisations`

**Headers:**
- `X-Username`: string (auto par gateway)

**Body:**
```json
{
  "specialisations": ["string", "string"]
}
```

**Response:** `200 OK` - Liste de UserSpecialisationDTO

---

### Vérifier si le profil est complet
**GET** `/api/users/{username}/profile-complete`

**Path Params:**
- `username`: string

**Response:** `200 OK`
```json
{
  "username": "string",
  "complete": boolean
}
```

---

## 📢 Announcement Service

### Créer une annonce
**POST** `/api/announcements`

**Headers:**
- `X-Username`: string (auto par gateway)

**Body:**
```json
{
  "title": "string",
  "description": "string",
  "requestDate": "ISO-8601 date",
  "hourlyRate": number,
  "location": "string",
  "images": ["string"],
  "specialisations": ["string"],
  "status": "PENDING | ACTIVE | EXPIRED | CANCELLED"
}
```

**Response:** `201 Created` - AnnouncementResponseDto
```json
{
  "id": number,
  "title": "string",
  "description": "string",
  "requestDate": "ISO-8601 date",
  "hourlyRate": number,
  "location": "string",
  "images": ["string"],
  "specialisations": ["string"],
  "status": "string",
  "ownerUsername": "string",
  "createdAt": "ISO-8601 datetime",
  "updatedAt": "ISO-8601 datetime"
}
```

---

### Mettre à jour une annonce
**PUT** `/api/announcements/{id}`

**Path Params:**
- `id`: number

**Body:** (même structure que POST)

**Response:** `200 OK` - AnnouncementResponseDto

---

### Changer le statut d'une annonce
**PATCH** `/api/announcements/{id}/status?status={status}`

**Path Params:**
- `id`: number

**Query Params:**
- `status`: `PENDING | ACTIVE | EXPIRED | CANCELLED`

**Response:** `200 OK` - AnnouncementResponseDto

---

### Supprimer une annonce
**DELETE** `/api/announcements/{id}`

**Path Params:**
- `id`: number

**Response:** `204 No Content`

---

### Obtenir une annonce par ID
**GET** `/api/announcements/{id}`

**Headers:**
- `X-Username`: string (auto par gateway)

**Path Params:**
- `id`: number

**Response:** `200 OK` - AnnouncementResponseDto

---

### Obtenir toutes les annonces (avec filtres optionnels)
**GET** `/api/announcements`

**Query Params:** (tous optionnels)
- `ownerUsername`: string
- `status`: `PENDING | ACTIVE | EXPIRED | CANCELLED`

**Response:** `200 OK` - Liste de AnnouncementResponseDto

---

### Obtenir les annonces par propriétaire
**GET** `/api/announcements/owner/{ownerUsername}`

**Path Params:**
- `ownerUsername`: string

**Response:** `200 OK` - Liste de AnnouncementResponseDto

---

### Obtenir les annonces par statut
**GET** `/api/announcements/status/{status}`

**Path Params:**
- `status`: `PENDING | ACTIVE | EXPIRED | CANCELLED`

**Response:** `200 OK` - Liste de AnnouncementResponseDto

---

## 📝 Application Service

### Créer une candidature
**POST** `/api/applications`

**Body:**
```json
{
  "announcementId": number,
  "guardianUsername": "string",
  "message": "string (optionnel)",
  "status": "PENDING | ACCEPTED | REJECTED | CANCELLED"
}
```

**Response:** `201 Created` - ApplicationResponseDto
```json
{
  "id": number,
  "announcementId": number,
  "guardianUsername": "string",
  "message": "string",
  "status": "string",
  "createdAt": "ISO-8601 datetime",
  "updatedAt": "ISO-8601 datetime"
}
```

---

### Obtenir une candidature par ID
**GET** `/api/applications/{id}`

**Path Params:**
- `id`: number

**Response:** `200 OK` - ApplicationResponseDto

---

### Obtenir toutes les candidatures (avec filtres optionnels)
**GET** `/api/applications`

**Query Params:** (tous optionnels)
- `announcementId`: number
- `guardianUsername`: string
- `status`: `PENDING | ACCEPTED | REJECTED | CANCELLED`

**Response:** `200 OK` - Liste de ApplicationResponseDto

---

### Mettre à jour le statut d'une candidature
**PATCH** `/api/applications/{id}/status`

**Path Params:**
- `id`: number

**Body:**
```json
{
  "status": "PENDING | ACCEPTED | REJECTED | CANCELLED"
}
```

**Response:** `200 OK` - ApplicationResponseDto

---

### Supprimer une candidature
**DELETE** `/api/applications/{id}`

**Path Params:**
- `id`: number

**Response:** `204 No Content`

---

## 💬 Chat Service

### Obtenir mes discussions
**GET** `/api/me/discussions`

**Headers:**
- `X-Username`: string (auto par gateway)

**Query Params:** (optionnels)
- `page`: number (défaut: 0)
- `limit`: number (défaut: 20)

**Response:** `200 OK` - Page de DiscussionDTO
```json
{
  "content": [
    {
      "id": number,
      "announcementId": number,
      "senderId": "string",
      "recipientId": "string",
      "lastMessageAt": "ISO-8601 datetime",
      "createdAt": "ISO-8601 datetime",
      "updatedAt": "ISO-8601 datetime"
    }
  ],
  "totalElements": number,
  "totalPages": number,
  "number": number,
  "size": number
}
```

---

### Obtenir une discussion par annonce et participants
**GET** `/api/discussions?announcementId={id}&recipientId={username}`

**Headers:**
- `X-Username`: string (auto par gateway)

**Query Params:**
- `announcementId`: number
- `recipientId`: string

**Response:** `200 OK` - DiscussionDTO (vide si non trouvée)

---

### Obtenir une discussion par ID
**GET** `/api/discussions/{id}`

**Headers:**
- `X-Username`: string (auto par gateway)

**Path Params:**
- `id`: number

**Response:** `200 OK` - DiscussionDTO

---

### Obtenir les messages d'une discussion
**GET** `/api/discussions/{id}/messages`

**Headers:**
- `X-Username`: string (auto par gateway)

**Path Params:**
- `id`: number

**Query Params:** (optionnels)
- `page`: number (défaut: 0)
- `limit`: number (défaut: 20)

**Response:** `200 OK` - Page de MessageDTO
```json
{
  "content": [
    {
      "id": number,
      "discussionId": number,
      "authorId": "string",
      "content": "string",
      "sentAt": "ISO-8601 datetime"
    }
  ],
  "totalElements": number,
  "totalPages": number,
  "number": number,
  "size": number
}
```

---

### Envoyer un message (créer discussion automatiquement)
**POST** `/api/messages`

**Headers:**
- `X-Username`: string (auto par gateway)

**Body:**
```json
{
  "content": "string",
  "announcementId": number,
  "recipientId": "string"
}
```

**Response:** `201 Created` - MessageDTO

---

### Envoyer un message dans une discussion
**POST** `/api/discussions/{id}/messages`

**Headers:**
- `X-Username`: string (auto par gateway)

**Path Params:**
- `id`: number

**Body:**
```json
{
  "content": "string",
  "announcementId": number (optionnel),
  "recipientId": "string (optionnel)"
}
```

**Response:** `201 Created` - MessageDTO

---

### Supprimer une discussion
**DELETE** `/api/discussions/{id}`

**Headers:**
- `X-Username`: string (auto par gateway)

**Path Params:**
- `id`: number

**Response:** `204 No Content`

---

## ⭐ Favorite Service

### Obtenir mes favoris
**GET** `/api/favorites`

**Headers:**
- `X-Username`: string (auto par gateway)

**Response:** `200 OK`
```json
[
  {
    "id": number,
    "guardianUsername": "string",
    "announcementId": number,
    "createdAt": "ISO-8601 datetime"
  }
]
```

---

### Vérifier si une annonce est favorite
**GET** `/api/favorites/check?announcementId={id}`

**Headers:**
- `X-Username`: string (auto par gateway)

**Query Params:**
- `announcementId`: number

**Response:** `200 OK`
```json
{
  "isFavorite": boolean
}
```

---

### Ajouter un favori
**POST** `/api/favorites`

**Headers:**
- `X-Username`: string (auto par gateway)

**Body:**
```json
{
  "announcementId": number
}
```

**Response:** `201 Created` - FavoriteDTO

---

### Supprimer un favori
**DELETE** `/api/favorites/{announcementId}`

**Headers:**
- `X-Username`: string (auto par gateway)

**Path Params:**
- `announcementId`: number

**Response:** `204 No Content`

---

## ⭐ Rating Service

### Créer une évaluation
**POST** `/api/ratings/{recipientId}`

**Headers:**
- `X-Username`: string (auto par gateway)

**Path Params:**
- `recipientId`: string (username de l'utilisateur évalué)

**Body:**
```json
{
  "score": number (1-5),
  "comment": "string (optionnel)"
}
```

**Response:** `201 Created` - RatingDTO
```json
{
  "id": number,
  "authorId": "string",
  "recipientId": "string",
  "score": number,
  "comment": "string",
  "createdAt": "ISO-8601 datetime",
  "updatedAt": "ISO-8601 datetime"
}
```

---

### Obtenir une évaluation par ID
**GET** `/api/ratings/{id}`

**Path Params:**
- `id`: number

**Response:** `200 OK` - RatingDTO

---

### Obtenir les évaluations reçues par un utilisateur
**GET** `/api/ratings/recipient/{recipientId}`

**Path Params:**
- `recipientId`: string

**Query Params:** (optionnels)
- `page`: number (défaut: 0)
- `limit`: number (défaut: 20)

**Response:** `200 OK` - Page de RatingDTO

---

### Obtenir les évaluations données par un utilisateur
**GET** `/api/ratings/author/{authorId}`

**Path Params:**
- `authorId`: string

**Query Params:** (optionnels)
- `page`: number (défaut: 0)
- `limit`: number (défaut: 20)

**Response:** `200 OK` - Page de RatingDTO

---

### Obtenir la moyenne des évaluations d'un utilisateur
**GET** `/api/ratings/recipient/{recipientId}/average`

**Path Params:**
- `recipientId`: string

**Response:** `200 OK`
```json
4.5
```

---

### Obtenir le nombre d'évaluations d'un utilisateur
**GET** `/api/ratings/recipient/{recipientId}/count`

**Path Params:**
- `recipientId`: string

**Response:** `200 OK`
```json
42
```

---

### Mettre à jour une évaluation
**PUT** `/api/ratings/{id}`

**Headers:**
- `X-Username`: string (auto par gateway, doit être l'auteur)

**Path Params:**
- `id`: number

**Body:**
```json
{
  "score": number (1-5),
  "comment": "string (optionnel)"
}
```

**Response:** `200 OK` - RatingDTO

---

### Supprimer une évaluation
**DELETE** `/api/ratings/{id}`

**Headers:**
- `X-Username`: string (auto par gateway, doit être l'auteur)

**Path Params:**
- `id`: number

**Response:** `204 No Content`

---

## 🔌 WebSocket

### Connection WebSocket
**Endpoint:** `ws://localhost:8080/ws`

**Note:** Après connexion, s'abonner aux topics pour recevoir les messages en temps réel.

---

### Envoyer un message en temps réel
**SEND TO:** `/app/discussions/{id}/messages`

**Payload:**
```json
{
  "authorId": "string",
  "content": "string",
  "announcementId": number (optionnel),
  "recipientId": "string (optionnel)"
}
```

---

### S'abonner aux messages d'une discussion
**SUBSCRIBE TO:** `/topic/discussions/{id}/messages`

**Messages reçus:** MessageDTO
```json
{
  "id": number,
  "discussionId": number,
  "authorId": "string",
  "content": "string",
  "sentAt": "ISO-8601 datetime"
}
```

---

## 📊 Codes de statut HTTP

- `200 OK` - Succès
- `201 Created` - Ressource créée
- `204 No Content` - Succès sans contenu (suppression)
- `400 Bad Request` - Requête invalide
- `401 Unauthorized` - Non authentifié
- `403 Forbidden` - Non autorisé
- `404 Not Found` - Ressource non trouvée
- `409 Conflict` - Conflit (ex: ressource déjà existante)
- `500 Internal Server Error` - Erreur serveur

---

## 🔐 Authentification

Tous les endpoints (sauf documentation) nécessitent un token JWT valide:

```javascript
headers: {
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json'
}
```

Le Gateway extrait automatiquement le username du token et l'injecte dans le header `X-Username`.

---

## 💡 Conseils pour les hooks React

### Structure recommandée

```javascript
// hooks/useUsers.js
export const useCreateUser = () => {
  return useMutation({
    mutationFn: (userData) => api.post('/api/users', userData),
  });
};

export const useUser = (username) => {
  return useQuery({
    queryKey: ['user', username],
    queryFn: () => api.get(`/api/users/${username}`),
  });
};

// hooks/useAnnouncements.js
export const useAnnouncements = (filters) => {
  return useQuery({
    queryKey: ['announcements', filters],
    queryFn: () => api.get('/api/announcements', { params: filters }),
  });
};

// hooks/useChat.js
export const useWebSocket = (discussionId) => {
  const client = useRef(null);
  
  useEffect(() => {
    client.current = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      onConnect: () => {
        client.current.subscribe(
          `/topic/discussions/${discussionId}/messages`,
          (message) => handleMessage(JSON.parse(message.body))
        );
      },
    });
    
    client.current.activate();
    return () => client.current.deactivate();
  }, [discussionId]);
};
```

---

**Date de mise à jour:** 10 novembre 2025  
**Version:** 1.0.0
