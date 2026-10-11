# SENSA — Rescue Panel

Web dashboard for **rescue/emergency services** in the SENSA emergency notification
system. Built with **Nuxt 4 (Vue 3) + TypeScript**, PrimeVue UI and Leaflet maps.

Operators use it to:

- review incoming emergency reports (`emergency-situation-request-service`);
- move a report through its verification workflow
  `REPORTED → UNDER_REVIEW → CONFIRMED → BROADCAST` (or `REJECTED`).
  Confirming a report triggers the alert broadcast to users in the area;
- manage the notification **templates** used for broadcasts (`template-service`).

All requests go through the **api-gateway** (`/api/v1/*`).

## Requirements

- Node.js 22 LTS (recommended). Nuxt 4 supports `^22.21 || ^24.11 || >=26`.
- A running backend (api-gateway + services). See the repository root docs.

## Setup

```bash
npm install
cp .env.example .env   # adjust NUXT_PUBLIC_API_BASE if needed
```

`.env`:

```bash
NUXT_PUBLIC_API_BASE=http://localhost:8007
```

## Development

```bash
npm run dev        # http://localhost:3000
```

## Production

```bash
npm run build
npm run preview
```

## Backend endpoints used

| Feature | Method & path |
| --- | --- |
| Login | `POST /api/v1/auth/login` |
| Validate token | `POST /api/v1/auth/validate` |
| List emergencies | `GET /api/v1/emergencies` |
| Get emergency | `GET /api/v1/emergencies/{id}` |
| Update status | `PATCH /api/v1/emergencies/{id}/status` |
| List templates | `GET /api/v1/templates` |
| Get template | `GET /api/v1/templates/{name}` |
| Create template | `POST /api/v1/templates` |
| Update template | `PATCH /api/v1/templates/{name}` |
| Delete template | `DELETE /api/v1/templates/{name}` |

> Note: the Rust `emergency-service` returns **snake_case** JSON, the Kotlin
> `template-service` returns **camelCase**. Normalisation happens in
> `app/utils/emergency.ts`.

## Notes / known simplifications

- Authentication reuses the regular `authentication-service` accounts; there is
  currently **no dedicated rescue-staff role**.
- The emergency model stores only an address (city/street/country), so the map
  geocodes it via OpenStreetMap Nominatim at runtime (may be unavailable).
