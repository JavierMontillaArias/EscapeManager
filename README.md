# EscapeManager

A full-stack Android application for the **operational management of escape room venues**, developed as a Final Degree Project (TFG). EscapeManager digitizes and automates every task that occurs before, during, and after an escape room session: from booking creation and QR-code email confirmations to real-time game management by the Game Master.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Features](#features)
- [Data Model](#data-model)
- [API Reference](#api-reference)
- [Key Flows](#key-flows)
- [Setup](#setup)

---

## Overview

EscapeManager targets **escape room businesses** and supports two distinct user roles:

| Role | Responsibilities |
|---|---|
| **Manager** | Manage rooms (CRUD), create and track bookings, view business statistics, register Game Master accounts |
| **Game Master** | Scan customer QR codes to start sessions, track hints given, close sessions with results, report room incidents |

The core workflow is fully automated:
1. Manager creates a booking → unique QR token generated → confirmation email sent to the customer.
2. Game Master scans the QR on the customer's device → booking validated → live session starts with a running timer.
3. Session ends → result recorded → statistics updated automatically.

---

## Architecture

The project follows a **RESTful client-server architecture**:

```
Android App (Kotlin/MVVM)  ←→  FastAPI REST API (Python)  ←→  MySQL Database
```

### Android (Frontend)
Internal architecture follows **MVVM with Repository layer**:

```
Fragment (View) → ViewModel → Repository → ApiService (Retrofit) → FastAPI → MySQL
```

### FastAPI (Backend)
Organized by domain with clear separation:

```
Router (controller) → Service (business logic) → SQLAlchemy ORM → MySQL
```

Communication between layers is exclusively **HTTP/JSON** with **JWT authentication** (`Authorization: Bearer <token>`).

---

## Tech Stack

### Backend (`API/`)

| Technology | Version | Purpose |
|---|---|---|
| Python | 3.11+ | Primary backend language |
| FastAPI | Latest | REST framework with auto-generated OpenAPI/Swagger docs |
| SQLAlchemy | 2.x | ORM for MySQL; models, queries, relationships |
| Alembic | Latest | Database migrations |
| MySQL (pymysql) | 8.x | Primary relational database |
| Pydantic v2 | Latest | Request/response schema validation and serialization |
| pydantic-settings | Latest | Typed environment variable loading from `.env` |
| python-jose | Latest | JWT generation and validation (HS256) |
| passlib + bcrypt | Latest | Secure password hashing |
| slowapi | Latest | Per-endpoint rate limiting |
| SendGrid | Latest | Transactional email with QR attachment via REST API |
| qrcode + Pillow | Latest | In-memory QR image generation (PNG, rounded style) |

### Frontend (`App/`)

| Technology | Version | Purpose |
|---|---|---|
| Kotlin | 2.0.21 | Primary Android language |
| Android SDK | API 33+ | Android platform base |
| Retrofit | 2.9.0 | Typed HTTP client for REST API consumption |
| OkHttp | 4.12.0 | Underlying HTTP client; auth interceptors, logging |
| AndroidX Navigation | 2.7.7 | Fragment navigation with Safe Args and `nav_graph.xml` |
| ViewModel + LiveData | 2.7.0 | MVVM pattern; lifecycle-aware UI state observation |
| Coroutines | 1.7.3 | Asynchronous API calls without blocking the main thread |
| CameraX | 1.3.2 | Camera access for QR scanning |
| ML Kit Barcode Scanning | 17.2.0 | Real-time QR code detection and decoding |
| Material Design 3 | 1.13.0 | UI components: buttons, inputs, cards, chips, snackbars |
| MPAndroidChart | v3.1.0 | Charts for Manager statistics screen |
| EncryptedSharedPreferences | — | Secure JWT storage on device (AES256-GCM) |

---

## Project Structure

```
.
├── API/                          # FastAPI backend
│   ├── app/
│   │   ├── main.py               # Entry point: CORS, lifespan events
│   │   ├── database.py           # SQLAlchemy engine, session, Base
│   │   ├── dependencies.py       # DI: get_current_user, require_manager, require_gamemaster
│   │   ├── config.py             # Settings via pydantic-settings
│   │   ├── models/               # ORM models: role, user, room, booking, game, incident
│   │   ├── schemas/              # Pydantic schemas for all request/response types
│   │   ├── routers/              # Route handlers: auth, users, rooms, bookings, qr, games, incidents, stats
│   │   ├── services/             # Business logic layer, one service per domain
│   │   └── utils/
│   │       ├── email_sender.py   # SendGrid integration
│   │       └── qr_generator.py   # In-memory QR PNG generation
│   ├── alembic/                  # Database migrations
│   ├── requirements.txt
│   └── main.py                   # ASGI entry point
│
└── App/                          # Android application
    └── app/src/main/java/com/javiermontillaarias/escapemanager/
        ├── EscapeManagerApp.kt   # Application class
        ├── MainActivity.kt       # Single Activity with NavHostFragment
        ├── data/
        │   ├── local/            # SessionManager (EncryptedSharedPreferences)
        │   ├── model/            # All Kotlin data classes
        │   ├── network/          # ApiService, RetrofitClient, AuthInterceptor, TokenAuthenticator
        │   └── repository/       # Auth, Booking, Game, Room, Incident, Stats repositories
        ├── ui/
        │   ├── auth/             # Login screen
        │   ├── manager/          # Dashboard, Bookings, Rooms, Stats, Incidents (Manager role)
        │   └── gamemaster/       # Dashboard, QR Scanner, Active Game, Incidents (GM role)
        └── util/                 # Resource sealed class, safeApiCall, Roles constants
```

---

## Features

### Role-based Authentication
Full JWT login/logout flow. Tokens are stored securely with `EncryptedSharedPreferences`. Access tokens refresh automatically on 401 responses via `TokenAuthenticator`. If the session expires completely, the app redirects to the login screen automatically.

### Room Management (CRUD)
Managers create, edit, and deactivate escape rooms. Each room has a name, theme, maximum capacity, and difficulty level. A room cannot be deactivated while it has active or in-progress bookings.

### Booking Management (CRUD)
Managers create bookings for customer groups, specifying room, date, time slot, group name, headcount, and customer email. The system enforces no time overlap on the same room and validates capacity. On creation, a unique QR token is generated and emailed to the customer automatically.

### Automatic QR Email
When a booking is created, the API generates a QR image (PNG, in-memory) encoding the booking's UUID token and attaches it to an HTML confirmation email sent via SendGrid. Managers can resend the QR from the booking detail screen if needed.

### QR Scanner — Session Start
The Game Master opens the camera scanner and points it at the customer's QR code. ML Kit detects it in real time. The app sends the token to the API, which validates it in 6 steps (booking status, date, ±15-minute time window, active room, no concurrent session). On success, the session is created and the app navigates to the live game screen.

### Live Session Management
During the session, the Game Master sees a running HH:MM:SS timer. A button records each hint given (debounced with `AtomicBoolean`). When the session ends, the GM enters optional observations and taps "Escaped" or "Did not escape" to close the session and record the result.

### Incident Reporting
Both roles can report incidents on any active room (e.g., a broken prop). Incidents are stored with description, room, and timestamp. Managers can view all incidents and mark them as resolved.

### Manager Statistics Dashboard
Real-time business summary (`GET /stats/summary`) plus detailed charts:
- **Escape rate** per room — what percentage of groups escape.
- **Average hints** per room — which rooms need the most assistance.
- **Occupancy by time slot** — morning / afternoon / evening demand distribution.
- **Room ranking** — weighted score: 60% escape rate + 40% hint quality.

### Game History
Game Masters see their own session history with results, hints used, and duration. Managers see the full history across all Game Masters.

### User Management
Managers can register new Game Master accounts, edit their details, and deactivate them. The last active Manager account cannot be deactivated.

---

## Data Model

```
roles ──< users ──< games
rooms ──< bookings ──── games (one-to-one)
rooms ──< incidents
users ──< incidents
```

### Key entities

| Entity | Key fields |
|---|---|
| `roles` | `id`, `nombre` (`"Manager"` \| `"Game Master"`) |
| `users` | `id`, `nombre`, `email`, `password_hash`, `activo`, `rol_id` |
| `rooms` | `id`, `nombre`, `tematica`, `capacidad_max`, `dificultad`, `activa` |
| `bookings` | `id`, `nombre_grupo`, `num_personas`, `email_cliente`, `fecha`, `hora_inicio`, `hora_fin`, `estado`, `qr_token`, `qr_usado`, `qr_enviado`, `sala_id` |
| `games` | `id`, `pistas_usadas`, `hora_inicio_real`, `hora_fin_real` (NULL = active), `escaparon`, `reserva_id`, `gamemaster_id` |
| `incidents` | `id`, `descripcion`, `fecha`, `resuelta`, `sala_id`, `usuario_id` |

**Booking states**: `pendiente` → `confirmada` → `en_curso` → `completada` / `cancelada`

---

## API Reference

**Base URL**: configured via `BuildConfig.BASE_URL`
**Auth**: `Authorization: Bearer <access_token>` (JWT HS256, 30-min expiry)

| Module | Endpoints |
|---|---|
| `/auth` | `POST /login`, `POST /register`, `POST /refresh`, `POST /logout`, `GET /me` |
| `/users` | `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| `/rooms` | `GET`, `GET /active`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` |
| `/bookings` | `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`, `PATCH /{id}/status`, `POST /{id}/resend-qr` |
| `/qr` | `POST /validate` |
| `/games` | `GET`, `GET /{id}`, `PATCH /{id}/hints`, `POST /{id}/close` |
| `/incidents` | `GET`, `POST`, `PATCH /{id}/resolve` |
| `/stats` | `GET /escape-rate`, `GET /hints-avg`, `GET /occupancy`, `GET /ranking`, `GET /summary` |

Rate limits: 5/min on login, 10/min on register/refresh, 20/min on QR validate/logout.

---

## Key Flows

### Login
```
LoginFragment → LoginViewModel → AuthRepository
  → POST /auth/login
  → tokens saved to EncryptedSharedPreferences
  → navigate to ManagerDashboard or GmDashboard based on role
```

### Booking Creation + QR Email
```
CreateBookingFragment → POST /bookings
  → validate room capacity and no schedule overlap
  → generate UUID qr_token
  → generate QR PNG in-memory (qrcode + Pillow)
  → send HTML email with QR attachment via SendGrid
  → return BookingResponse with qr_enviado=true
```

### QR Scan → Session Start
```
QrScannerFragment (CameraX + ML Kit) → detect QR
  → POST /qr/validate
  → 6-step validation: status, date, ±15min window, room active, no concurrent session
  → create Game record, set booking estado="en_curso"
  → navigate to ActiveGameFragment with gameId, groupName, roomName, startTime
```

### Token Refresh (automatic)
```
Any request → API returns 401
  → TokenAuthenticator intercepts
  → POST /auth/refresh (blocking, synchronized)
  → update tokens in EncryptedSharedPreferences (commit())
  → retry original request with new token
  → if refresh fails: clearSession() → emit sessionExpiredFlow → redirect to login
```

---

## Setup

### Backend

```bash
cd API
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

Create a `.env` file in `API/` with:

```env
DATABASE_URL=mysql+pymysql://user:password@localhost:3306/escapemanager
SECRET_KEY=your-secret-key
SENDGRID_API_KEY=your-sendgrid-key
SENDGRID_FROM_EMAIL=your-verified-sender@example.com
ENVIRONMENT=development
```

Apply migrations and start the server:

```bash
alembic upgrade head
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### Android App

1. Open the `App/` directory in Android Studio.
2. Set the API base URL in `App/app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "BASE_URL", "\"http://192.168.x.x:8000/\"")
   ```
3. Build and run on a physical device (API 33+) or emulator.

> The QR scanner requires a physical device with a working camera for full functionality.

---

## Security

- Passwords hashed with bcrypt (constant-time comparison to prevent timing attacks).
- JWT access tokens expire in 30 minutes; refresh tokens rotate on every use.
- Refresh token blacklist maintained in-memory on the server.
- JWT stored on-device using AES256-GCM via `EncryptedSharedPreferences`.
- Per-endpoint rate limiting via `slowapi`.
- Network Security Config restricts HTTP to localhost in debug; HTTPS only in release.
