# Caverne — Résumé des capacités (FR)

Caverne est un backend Spring Boot (Java 21) qui expose une API REST e-commerce sous le préfixe
`/api/v1`. Le contrat est décrit dans `docs/api.yaml` (OpenAPI 3), les clients générés sont dans
`clients/`.

## 1. Authentification et utilisateurs

- Inscription locale (`POST /auth/register`), connexion via `POST /auth/login` qui retourne un
  JWT d'accès HS256 court (15 min) dans le corps **et** pose un cookie `refresh_token`
  long-lived (30 jours, HttpOnly/Secure/SameSite=None) + un cookie `csrf_token` pour
  l'anti-CSRF double-submit.
- `POST /auth/refresh` fait tourner le cookie refresh et émet un nouveau JWT d'accès ;
  l'appelant doit renvoyer la valeur du cookie `csrf_token` dans l'en-tête `X-CSRF-Token`.
  Un refresh déjà révoqué qui se représente déclenche la révocation de toute la famille
  (détection de vol).
- `POST /auth/logout` révoque le refresh courant côté serveur et efface les deux cookies.
  Le JWT d'accès lui reste stateless et expire naturellement en 15 min maximum.
- Provisionnement miroir dans Supabase à l'inscription quand
  `auth.providers.supabase.enabled=true` ; ingestion des Auth Hooks Supabase via
  `POST /auth/webhooks/supabase` (protégé par un secret partagé).
- Deux rôles : `ADMIN` et `SIMPLE_USER`. Les authentifications fournies par un provider externe
  arrivent toujours en `SIMPLE_USER` ; la promotion admin doit venir d'un rattachement local
  préexistant.
- Points d'accès profil (`GET/PUT /users/me`) et carnet d'adresses
  (`GET/POST /users/me/addresses`, `PUT/DELETE /users/me/addresses/{id}`, mise en défaut).
- Gestion admin : `GET/POST /users`, `GET/DELETE /users/{id}`.
- Bootstrap admin initial piloté par `bootstrap.admin.*`, exécuté une seule fois.

## 2. Catalogue — catégories, produits, prix, images

- Arbre public `GET /categories` (liste plate via `?flat=true`) et détail
  `GET /categories/{id}`.
- Upsert admin : `POST /categories` / `PUT /categories/{id}`, suppression via `DELETE`.
- **Le `label` d'une catégorie est unique globalement (insensible à la casse).** Un doublon
  renvoie `409 Conflict`.
- Catalogue paginé public `GET /products` avec filtres `category_id`, `is_active`, `search`,
  `currency`, `price_date`, `price_from`, `price_to`. Détail public `GET /products/{id}` avec les
  mêmes sélecteurs de date/plage de prix.
- Upsert admin : `POST /products` / `PUT /products/{id}`, suppression via `DELETE`.
- **Les champs `label` et `reference` d'un produit sont uniques globalement (insensibles à la
  casse).** Un doublon renvoie `409 Conflict`.
- Les prix sont historisés — chaque produit porte une liste
  `(currency, unit, valid_from, value)`. Le catalogue public renvoie par défaut le prix le plus
  récent applicable par `(currency, unit)`, ou bien l'intervalle complet si `price_from` /
  `price_to` sont fournis.
- Images produit : CRUD admin `GET/POST /products/{id}/images`,
  `DELETE /products/{id}/images/{imageId}` et `PUT .../images/{imageId}/main` garantissant au
  plus une image `is_main` par produit.

## 3. Stock et journal des mouvements

- Le stock est porté par `products.stock_quantity`.
- Endpoints admin : `GET /products/{id}/stock` en lecture, `PUT /products/{id}/stock` pour fixer
  la nouvelle quantité (non négative), `GET /products/{id}/stock/movements` pour lire le journal.
- Chaque variation est tracée dans `stock_movements` avec un motif :
  - `ORDER_PLACED` — décrément consécutif à une commande confirmée.
  - `ORDER_CANCELLED` — rétablissement consécutif à une annulation.
  - `MANUAL_ADJUSTMENT` — ajustement admin via `PUT /products/{id}/stock`.
  - `RESTOCK` — réservé aux futurs réapprovisionnements.
- **Verrouillage ligne par ligne :** `POST /orders`, `POST /orders/{id}/cancel` et
  l'ajustement admin lisent le produit en `SELECT … FOR UPDATE`. Deux clients qui commandent le
  même produit sont sérialisés par le verrou ; le deuxième relit le stock à jour et reçoit
  `422` si la quantité devient insuffisante.

## 4. Commandes et checkout

- Un invité peut passer commande (`POST /orders`) ; un utilisateur connecté peut aussi lister
  ses propres commandes (`GET /orders`) et consulter `GET /orders/{id}` (les invités lisent leurs
  propres commandes sans bearer).
- Vue globale admin via `GET /orders/all` ; transition de statut via `PUT /orders/{id}/status`
  (admin) et annulation via `POST /orders/{id}/cancel` (propriétaire ou admin). L'annulation
  restaure le stock.
- Chaque ligne fige un snapshot produit (label + prix au moment de la commande) dans
  `order_items.product_snapshot` : les modifications ultérieures du produit ne réécrivent pas les
  commandes passées.
- Le coût de livraison peut être rattaché via `delivery_cost_id`.

## 5. Paiements

- Découverte des providers via `GET /payment-methods` (`manual`, `stripe`…).
- Paiement d'une commande : `POST /orders/{id}/payments` ; historique complet sur
  `GET /orders/{id}/payments`.
- Webhook Stripe entrant : `POST /payments/webhooks/stripe` avec l'en-tête `Stripe-Signature`,
  idempotent par `event.id`.
- Remboursement admin : `POST /payments/{orderId}/refund` (branché sur Stripe). Une commande
  entièrement remboursée passe à `CANCELLED`.

## 6. Coûts de livraison

- Lecture publique (`GET /delivery-costs`) et création publique (`POST /delivery-costs`) afin
  que le checkout capture le coût calculé côté front.
- Mise à jour / suppression admin : `PUT /delivery-costs/{id}`, `DELETE /delivery-costs/{id}`.

## 7. Exploitation

- `GET /actuator/health` est public (readiness / liveness).
- CORS entièrement configurable via `app.cors.*` / `APP_CORS_*`.
- Importeur de seed `NichesCatalogImporter` optionnel — piloté par `catalog.niches-import.*`.
  Idempotent face aux labels répétés dans le fichier source : la seconde occurrence réutilise le
  premier produit au lieu de violer l'index unique global sur `products.label`.

## 8. Ce que l'application ne fait PAS (hors périmètre)

- Pas de système de notation / avis.
- Pas de tableau de bord analytique.
- Pas de cloisonnement multi-tenant (catégories, produits, commandes sont globaux).
- Pas d'intégrations webhook autres que Stripe + Supabase Auth Hooks.
- Pas d'abonnement / facturation récurrente.

## 9. Où chercher

| Sujet                         | Référence                                          |
|-------------------------------|----------------------------------------------------|
| Contrat REST                  | `docs/api.yaml`                                    |
| Client généré                 | `clients/`                                         |
| Documentation détaillée       | `DOCUMENTATION.md`                                 |
| Guide de déploiement          | `STEP_BY_STEP.md`                                  |
| Historique des livraisons     | `REALISATION.md`                                   |
| Simulation des parcours       | `simulation.md`                                    |
| Source du catalogue de seed   | `niches.md`                                        |
| Migrations Flyway             | `src/main/resources/db/migration/`                 |

Dernière relecture : 2026-04-20.
