<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.1.0-6DB33F?style=for-the-badge&logo=spring-boot" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Stripe-Payments-635BFF?style=for-the-badge&logo=stripe&logoColor=white" alt="Stripe"/>
  <img src="https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white" alt="JWT"/>
</p>

# BookMyShow — Full-Stack Movie Ticket Booking Platform

A **production-grade**, full-stack movie ticket booking system inspired by BookMyShow. Built with **Spring Boot**, **React**, **MySQL**, and **Stripe**, featuring real-time seat locking, split payments, food & parking add-ons, wallet ecosystem, role-based dashboards, unified QR codes, and database-driven refund policies.

> **This is not a toy project.** It implements real-world concurrency handling, idempotent payments, time-decay refund policies, optimistic locking, and session-based booking flows — the same patterns used in production booking systems.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [User Roles & Permissions](#user-roles--permissions)
- [Core Features](#core-features)
- [Temporary Wallet (Temp Wallet)](#temporary-wallet-temp-wallet)
- [Spinning Wheel Feature](#spinning-wheel-feature)
- [Application Flows](#application-flows)
   - [User Registration & Authentication](#1-user-registration--authentication-flow)
   - [Movie Discovery](#2-movie-discovery-flow)
   - [Ticket Booking (End-to-End)](#3-ticket-booking-flow-end-to-end)
   - [Seat Locking Mechanism](#4-seat-locking-mechanism)
   - [Payment Processing](#5-payment-processing-flow)
   - [Booking Cancellation & Refund](#6-booking-cancellation--refund-flow)
   - [Wallet Operations](#7-wallet-operations-flow)
   - [Food Ordering](#8-food-ordering-flow)
   - [Parking Booking](#9-parking-booking-flow)
   - [Admin Management](#10-admin-management-flow)
   - [Theater Owner Operations](#11-theater-owner-operations-flow)
- [System Design & UML Diagrams](#system-design--uml-diagrams)
   - [High-Level Architecture](#high-level-system-architecture)
   - [Entity Relationship Diagram](#entity-relationship-diagram)
   - [Booking Sequence Diagram](#booking-sequence-diagram)
   - [Payment State Machine](#payment-state-machine)
   - [Seat Lock State Machine](#seat-lock-state-machine)
   - [Class Diagram (Core)](#class-diagram-core-domain)
- [Design Patterns](#design-patterns-used)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Environment Variables](#environment-variables)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │  React SPA   │  │  Admin Panel │  │  Theater Owner Dashboard │  │
│  │  (Vite)      │  │  (React)     │  │  (React)                 │  │
│  └──────┬───────┘  └──────┬───────┘  └────────────┬─────────────┘  │
│         │                 │                        │                │
│         └─────────────────┼────────────────────────┘                │
│                           │  HTTP/REST + JWT                        │
├───────────────────────────┼─────────────────────────────────────────┤
│                    API GATEWAY LAYER                                 │
│  ┌────────────────────────┼──────────────────────────────────────┐  │
│  │              Spring Security Filter Chain                     │  │
│  │         JWT Authentication Filter → Role Extraction           │  │
│  │              CORS Configuration                               │  │
│  └────────────────────────┼──────────────────────────────────────┘  │
├───────────────────────────┼─────────────────────────────────────────┤
│                    CONTROLLER LAYER (REST)                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │  Auth    │ │  Movie   │ │ Booking  │ │ Payment  │ │  Admin   │ │
│  │Controller│ │Controller│ │Controller│ │Controller│ │Controller│ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │
│       │             │            │             │             │       │
│  ┌────┴─────┐ ┌─────┴────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴─────┐ │
│  │ Seat Lock│ │  Show    │ │  Wallet  │ │  Food    │ │ Parking  │ │
│  │Controller│ │Controller│ │Controller│ │Controller│ │Controller│ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │
├───────┼─────────────┼────────────┼─────────────┼─────────────┼──────┤
│                    SERVICE LAYER (Business Logic)                    │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  AuthService │ MovieService │ BookingService │ PaymentService│   │
│  │  SeatLockService │ WalletService │ RefundPolicyService      │   │
│  │  EmailService │ StripePaymentService │ TicketService        │   │
│  │  FoodService │ ParkingService │ QrCodeService               │   │
│  │  AdminAnalyticsService │ TheatreAdminService                │   │
│  │  PaymentAddOnOrchestrationService                           │   │
│  └──────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                    TRANSFORMER LAYER (DTO ↔ Entity)                 │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ MovieTransformer │ ShowTransformer │ TicketTransformer       │   │
│  │ TheaterTransformer │ UserTransformer                         │   │
│  └──────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                    REPOSITORY LAYER (Data Access)                    │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ Spring Data JPA Repositories (15+ interfaces)                │   │
│  │ Custom queries │ Pagination │ Derived query methods          │   │
│  └──────────────────────────┬───────────────────────────────────┘   │
├─────────────────────────────┼───────────────────────────────────────┤
│                    PERSISTENCE LAYER                                 │
│  ┌──────────────────────────┼───────────────────────────────────┐   │
│  │                     MySQL 8.0                                │   │
│  │  20+ Tables │ JPA Auto-DDL │ Indexed │ Constraints           │   │
│  └──────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                    EXTERNAL SERVICES                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                          │
│  │  Stripe  │  │  Gmail   │  │  ZXing   │                          │
│  │ Payments │  │   SMTP   │  │ QR Codes │                          │
│  └──────────┘  └──────────┘  └──────────┘                          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Backend** | Spring Boot 3.1.0, Java 17 | REST API server |
| **Frontend** | React 18, Vite 5, React Router 6 | Single Page Application |
| **Database** | MySQL 8.0 + Hibernate ORM | Relational data persistence |
| **Auth** | Spring Security + JWT (jjwt 0.11.5) | Stateless authentication |
| **Payments** | Stripe API (stripe-java 24.3.0) | Card payment processing |
| **Email** | Spring Mail + Gmail SMTP | Booking confirmations & notifications |
| **QR Codes** | ZXing 3.5.2 | Unified QR code generation |
| **Charts** | Recharts | Admin analytics visualization |
| **Styling** | SCSS Modules | Component-scoped styling |
| **API Docs** | SpringDoc OpenAPI 2.0 | Auto-generated Swagger UI |
| **Build** | Maven (backend), Vite (frontend) | Build tooling |
| **Password** | BCrypt | Secure password hashing |
| **Config** | dotenv-java 3.0.0 | Environment variable management |

---

## User Roles & Permissions

The system implements **Role-Based Access Control (RBAC)** with three distinct user roles:

```
                    ┌─────────────────────┐
                    │     BookMyShow      │
                    │   Access Control    │
                    └──────────┬──────────┘
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
   ┌───────────────┐  ┌───────────────┐  ┌────────────────┐
   │     USER      │  │     ADMIN     │  │ THEATER_OWNER  │
   │  (Customer)   │  │ (Super Admin) │  │  (Manager)     │
   └───────┬───────┘  └───────┬───────┘  └───────┬────────┘
           │                   │                   │
           ▼                   ▼                   ▼
   • Browse movies      • Full system       • Manage assigned
   • Search & filter       access              theater only
   • Book tickets       • Add/edit movies   • Schedule shows
   • Select seats       • Add/edit theaters • Accept/reject
   • Make payments      • Manage shows        recommendations
   • View bookings      • Manage users      • View theater
   • Cancel tickets     • Add theater         analytics
   • Get refunds          owners            • Manage seats
   • Use wallet         • View analytics      & pricing
   • Order food         • Revenue reports   • View bookings
   • Book parking       • Recommend movies    for their shows
   • View history       • Manage food menu
                        • Manage parking
                        • Configure refund
                          rules
```

### Role Details

| Role | Access Level | Key Capabilities |
|------|-------------|-----------------|
| **USER** | `/api/*`, `/ticket/*` | Browse movies, book tickets, manage wallet, order food, book parking, cancel with refund |
| **ADMIN** | `/admin/*` | Full CRUD on movies/theaters/shows, user management, analytics dashboard, recommendations, food & parking management |
| **THEATER_OWNER** | `/owner/*` | Manage assigned theater, schedule shows for recommended movies, theater-specific analytics, seat configuration |



## Core Features
## Temporary Wallet (Temp Wallet)

The Temporary Wallet (Temp Wallet) feature allows users to receive and use promotional or refund credits that are separate from their main wallet balance. This wallet is used for:

- **Refunds for failed or partial transactions**: If a payment fails or is partially refunded, the amount is credited to the Temp Wallet for immediate use.
- **Promotional credits**: Admins can issue temporary credits for marketing campaigns, which are stored in the Temp Wallet.
- **Automatic deduction**: During checkout, the system automatically deducts from the Temp Wallet first before using the main wallet or other payment methods.
- **Expiry management**: Temp Wallet balances can have an expiry date, after which unused credits are revoked.
- **Audit and transparency**: All Temp Wallet transactions are logged and visible in the user’s transaction history.

**API Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/temp-wallet/balance/{userId}` | Get Temp Wallet balance |
| `POST` | `/api/temp-wallet/credit` | Credit Temp Wallet (refund/promo) |
| `POST` | `/api/temp-wallet/debit` | Debit Temp Wallet (on booking) |
| `GET` | `/api/temp-wallet/transactions/{userId}` | Temp Wallet transaction log |

**Business Rules:**
- Temp Wallet is prioritized for deductions during payments.
- Expired credits are automatically purged.
- Users are notified of expiring credits.

**Use Cases:**
- Refunds processed instantly to Temp Wallet for failed/cancelled bookings.
- Admin issues ₹100 promo credit to all users for a festival offer.

---

## Spinning Wheel Feature

The Spinning Wheel is a gamification feature that rewards users with random prizes or discounts. It is designed to increase user engagement and retention.

- **How it works:**
   - Users can spin the wheel once per day or after completing specific actions (e.g., booking a ticket).
   - Prizes include: wallet credits, discount coupons, free food add-ons, or bonus loyalty points.
   - The outcome is determined randomly, with configurable probabilities for each prize.
   - All spins and rewards are logged for audit and analytics.

**API Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/spin-wheel/spin/{userId}` | Spin the wheel and get a reward |
| `GET` | `/api/spin-wheel/history/{userId}` | Get user’s spin history |

**Business Rules:**
- Each user can spin only once per day (or as configured).
- Rewards are credited instantly (e.g., wallet, coupon, etc.).
- Admin can configure prizes and probabilities.

**Use Cases:**
- User books a ticket and gets a free spin, winning a ₹50 wallet credit.
- Admin runs a weekend campaign with special prizes on the wheel.

---

### Ticket Booking System
- **Real-time seat selection** with interactive seat layout
- **10-minute seat locking** to prevent race conditions
- **Session-based booking flow** with unique `sessionId` tracking
- **Multiple seat types**: Classic, Premium, Gold, Silver, Couple (2-seater recliners)
- **Unified QR code** generation for ticket + food + parking

### Payment Ecosystem
- **6 Payment Methods**: Credit Card, Debit Card, UPI, Net Banking, Wallet, Stripe
- **Split Payment**: Pay partially with wallet + card (e.g., ₹500 wallet + ₹1000 card)
- **Idempotent transactions** with unique `transactionId`
- **Price breakdown**: Base price + convenience fee + tax − discount
- **Stripe integration** with PaymentIntent API (test mode)

### Wallet System
- **Default ₹10,000** balance on signup
- **Top-up via card** payment
- **Transaction audit log** — immutable history with balance snapshots
- **Credit / Debit / Refund** operations with type safety
- **Optimistic locking** via `@Version` to prevent race conditions

### Booking Cancellation & Refund
- **Database-driven refund policy** (no hardcoded percentages)
- **Time-decay refund rules**:
  | Time Since Booking | Refund % |
  |-------------------|----------|
  | Within 1 hour | 100% |
  | Within 6 hours | 75% |
  | Within 12 hours | 50% |
  | After 12 hours | 0% |
- **Refund estimate API** before confirming cancellation
- **Automatic wallet credit** on cancellation

### Food & Beverage Module
- **Theater-specific menus** with categories: Combo, Popcorn, Beverage, Snack, Dessert
- **Deliver-to-seat** functionality
- **Order tracking**: PENDING → CONFIRMED → PREPARING → DELIVERED
- **Integrated with booking** — order during checkout

### Parking Module
- **Vehicle types**: Two-Wheeler (₹30/hr), Four-Wheeler (₹50/hr), EV (₹40/hr)
- **Duration-based pricing** (min 1hr, max 12hrs)
- **15-minute grace period** for overstay
- **1.5x overstay multiplier**
- **Status lifecycle**: BOOKED → ACTIVE → COMPLETED
- **Linked to movie ticket** (optional)

### Admin Analytics Dashboard
- **Revenue reports** by city, movie, theater
- **Occupancy heatmaps** and trends
- **Cancellation trends** and insights
- **Payment distribution** analysis
- **Peak time analysis** for scheduling optimization
- **Genre & language** performance tracking
- **Exportable reports** (CSV format)

---

## 🔄 Application Flows

### 1. User Registration & Authentication Flow

```
┌──────┐          ┌──────────┐          ┌───────────┐          ┌────────┐
│Client│          │AuthController│      │AuthService │          │Database│
└──┬───┘          └─────┬────────┘      └─────┬─────┘          └───┬────┘
   │  POST /auth/signup  │                     │                    │
   │ {email,password,    │                     │                    │
   │  name,mobile,       │                     │                    │
   │  gender,age}        │                     │                    │
   │────────────────────>│  signup(dto)         │                    │
   │                     │────────────────────>│                    │
   │                     │                     │ Check duplicate    │
   │                     │                     │───────────────────>│
   │                     │                     │                    │
   │                     │                     │ BCrypt password    │
   │                     │                     │ Create user        │
   │                     │                     │ (role=USER)        │
   │                     │                     │───────────────────>│
   │                     │                     │                    │
   │                     │                     │ Create wallet      │
   │                     │                     │ (₹10,000 default)  │
   │                     │                     │───────────────────>│
   │                     │                     │                    │
   │                     │                     │ Generate JWT       │
   │                     │  {accessToken,      │ (access+refresh)   │
   │  AuthResponseDto    │   refreshToken,     │                    │
   │  with tokens        │   user details}     │                    │
   │<────────────────────│<────────────────────│                    │
   │                     │                     │                    │
   │  POST /auth/login   │                     │                    │
   │ {email, password}   │                     │                    │
   │────────────────────>│  login(dto)         │                    │
   │                     │────────────────────>│                    │
   │                     │                     │ Verify BCrypt      │
   │                     │                     │ Generate JWT pair  │
   │  {accessToken,      │                     │                    │
   │   refreshToken}     │<────────────────────│                    │
   │<────────────────────│                     │                    │
```

**Key Points:**
- Passwords hashed with **BCrypt** before storage
- JWT tokens issued with **access** (short-lived) + **refresh** (long-lived) strategy
- Wallet auto-created with ₹10,000 balance on signup
- Duplicate email check prevents re-registration

---

### 2. Movie Discovery Flow

```
┌──────┐          ┌────────────────┐          ┌──────────────┐
│Client│          │MovieSearchCtrl │          │MovieSearchSvc│
└──┬───┘          └───────┬────────┘          └──────┬───────┘
   │                      │                          │
   │ GET /api/movies/     │                          │
   │     now-showing      │  getNowShowing()         │
   │─────────────────────>│─────────────────────────>│
   │  [Movie list]        │                          │
   │<─────────────────────│<─────────────────────────│
   │                      │                          │
   │ GET /api/movies/     │                          │
   │     search?keyword=  │  searchMovies(keyword)   │
   │─────────────────────>│─────────────────────────>│
   │  [Filtered movies]   │                          │
   │<─────────────────────│<─────────────────────────│
   │                      │                          │
   │ GET /api/movies/     │                          │
   │  filter/advanced?    │  advancedFilter(genre,   │
   │  genre=ACTION&       │    language, rating,     │
   │  language=HINDI&     │    nowShowing)            │
   │  minRating=7.0       │                          │
   │─────────────────────>│─────────────────────────>│
   │  [Filtered movies]   │                          │
   │<─────────────────────│<─────────────────────────│
   │                      │                          │
   │ GET /api/movies/     │                          │
   │   city/{city}        │  getMoviesByCity(city)   │
   │─────────────────────>│─────────────────────────>│
   │  [City's movies]     │                          │
   │<─────────────────────│<─────────────────────────│
```

**Search & Filter Capabilities:**
- Keyword search (title, director, cast)
- Filter by 10 genres: `ACTION`, `DRAMA`, `THRILLER`, `ROMANTIC`, `COMEDY`, `HISTORICAL`, `ANIMATION`, `SPORTS`, `SOCIAL`, `WAR`
- Filter by 7 languages: `HINDI`, `ENGLISH`, `TELUGU`, `TAMIL`, `MARATHI`, `PUNJAB`, `KANNADA`
- Filter by minimum rating
- City-based movie listings
- Now-showing vs Upcoming toggle

---

### 3. Ticket Booking Flow (End-to-End)

This is the most complex flow in the system. It spans 5 stages:

```
┌─────────────┐    ┌─────────────┐    ┌──────────────┐    ┌──────────────┐    ┌────────────────┐
│   STAGE 1   │    │   STAGE 2   │    │   STAGE 3    │    │   STAGE 4    │    │    STAGE 5     │
│   Movie &   │───>│    Seat     │───>│   Add-ons    │───>│   Payment    │───>│  Confirmation  │
│   Show      │    │  Selection  │    │ (Food+Park)  │    │  Processing  │    │  & QR Code     │
│  Selection  │    │  & Locking  │    │  (Optional)  │    │              │    │                │
└─────────────┘    └─────────────┘    └──────────────┘    └──────────────┘    └────────────────┘
      │                  │                   │                   │                     │
  Browse shows      Lock seats for       Select food          Pay via              Generate
  by movie/         10 minutes           & parking            Card/Wallet/         unified QR
  theater/date      (prevents race       during checkout      Split/Stripe         Send email
                    conditions)                                                     confirmation
```

**Detailed Sequence:**

```
┌──────┐    ┌─────────┐    ┌──────────┐    ┌─────────┐    ┌─────────┐    ┌────────┐
│Client│    │SeatLock │    │ Payment  │    │ Ticket  │    │  QR     │    │ Email  │
│      │    │Controller│   │Controller│    │Service  │    │Service  │    │Service │
└──┬───┘    └────┬────┘    └────┬─────┘    └────┬────┘    └────┬────┘    └───┬────┘
   │             │              │               │              │             │
   │ 1. Lock     │              │               │              │             │
   │   seats     │              │               │              │             │
   │────────────>│              │               │              │             │
   │ {showId,    │              │               │              │             │
   │  seatNos,   │              │               │              │             │
   │  userId}    │              │               │              │             │
   │             │              │               │              │             │
   │ sessionId   │              │               │              │             │
   │ expiryTime  │              │               │              │             │
   │<────────────│              │               │              │             │
   │             │              │               │              │             │
   │ 2. Initiate │              │               │              │             │
   │   payment   │              │               │              │             │
   │─────────────────────────>│               │              │             │
   │ {sessionId,              │               │              │             │
   │  userId,                 │               │              │             │
   │  paymentMethod,          │               │              │             │
   │  transactionId(unique)}  │               │              │             │
   │                          │               │              │             │
   │ 3. Process payment       │               │              │             │
   │   (Stripe/Wallet)        │               │              │             │
   │                          │──────────────>│              │             │
   │                          │               │              │             │
   │                          │               │ 4. Create    │             │
   │                          │               │    ticket    │             │
   │                          │               │──────────────>│             │
   │                          │               │              │             │
   │                          │               │  5. Generate │             │
   │                          │               │     QR code  │             │
   │                          │               │<─────────────│             │
   │                          │               │              │             │
   │                          │               │  6. Send     │             │
   │                          │               │     email    │             │
   │                          │               │──────────────────────────>│
   │                          │               │              │             │
   │  BookingConfirmation     │               │              │             │
   │  {ticket, qrCode,        │               │              │             │
   │   payment details}       │               │              │             │
   │<─────────────────────────│<──────────────│              │             │
```

---

### 4. Seat Locking Mechanism

The seat locking system prevents two users from booking the same seat simultaneously.

```
┌────────────────────────────────────────────────────────────────────┐
│                    SEAT LOCK LIFECYCLE                              │
│                                                                    │
│  User selects       Lock acquired         Payment           Seats  │
│  seats              (10 min TTL)          complete          booked │
│    │                    │                    │                 │    │
│    ▼                    ▼                    ▼                 ▼    │
│  ┌────┐  Success   ┌────────┐  Payment  ┌──────────┐  Book  ┌──┐ │
│  │Lock│───────────>│ LOCKED │──────────>│CONFIRMED │──────>│OK│ │
│  │Req │            └────┬───┘           └──────────┘       └──┘ │
│  └────┘                 │                                        │
│                    Timeout(10m)                                    │
│                    or Manual                                       │
│                         │                                          │
│                         ▼                                          │
│                    ┌──────────┐                                    │
│                    │ RELEASED │  ← Seats available again           │
│                    └──────────┘                                    │
│                                                                    │
│  CONCURRENT ACCESS HANDLING:                                       │
│  ┌─────────┐    ┌─────────┐                                       │
│  │ User A  │    │ User B  │                                       │
│  │locks S1 │    │tries S1 │                                       │
│   │  OK   │        │  FAIL │  ← Same seat, same show              │
│  └─────────┘    └─────────┘    "Seat already locked"              │
│                                                                    │
│  LOCK EXTENSION:                                                   │
│  If user needs more time: POST /extend → adds 5 more minutes      │
│  Maximum extension: once per session                               │
└────────────────────────────────────────────────────────────────────┘
```

**API Endpoints:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/seat-locks/lock` | Lock selected seats (10 min) |
| `POST` | `/api/seat-locks/release/{sessionId}` | Release locked seats |
| `GET` | `/api/seat-locks/session/{sessionId}/remaining-time` | Check remaining lock time |
| `POST` | `/api/seat-locks/session/{sessionId}/extend` | Extend lock (+5 min) |
| `POST` | `/api/seat-locks/check-availability` | Check if seats are available |
| `GET` | `/api/seat-locks/show/{showId}/locked-seats` | Get all locked seats for a show |

---

### 5. Payment Processing Flow

```
┌────────────────────────────────────────────────────────────────────┐
│                    PAYMENT METHODS                                  │
│                                                                    │
│  ┌────────────┐  ┌──────────┐  ┌───────┐  ┌───────────────────┐  │
│  │Credit/Debit│  │   UPI    │  │Wallet │  │ Wallet+Card Split │  │
│  │   Card     │  │          │  │       │  │                   │  │
│  └─────┬──────┘  └────┬─────┘  └───┬───┘  └────────┬──────────┘  │
│        │              │            │                │              │
│        └──────────────┴────────────┴────────────────┘              │
│                           │                                        │
│                    ┌──────┴──────┐                                  │
│                    │  INITIATE   │  POST /api/payment/initiate     │
│                    │  PAYMENT    │  {sessionId, userId,            │
│                    │             │   paymentMethod,                │
│                    │             │   transactionId (idempotent)}   │
│                    └──────┬──────┘                                  │
│                           │                                        │
│              ┌────────────┼────────────┐                           │
│              ▼            ▼            ▼                           │
│         ┌────────┐  ┌─────────┐  ┌──────────┐                    │
│         │PENDING │→ │PROCESS- │→ │ SUCCESS  │                    │
│         │        │  │  ING    │  │          │                    │
│         └────────┘  └─────────┘  └──────────┘                    │
│              │            │                                        │
│              ▼            ▼                                        │
│         ┌────────┐  ┌─────────┐                                   │
│         │CANCEL- │  │ FAILED  │                                   │
│         │  LED   │  │         │                                   │
│         └────────┘  └─────────┘                                   │
│                                                                    │
│  SPLIT PAYMENT FLOW (Wallet + Card):                              │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ Total: ₹1500                                                │  │
│  │ ├── Wallet debit: ₹500  (instant)                           │  │
│  │ └── Stripe charge: ₹1000 (PaymentIntent)                   │  │
│  │                                                              │  │
│  │ If Stripe fails → Wallet ₹500 refunded automatically       │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  PRICE BREAKDOWN:                                                  │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ Base Amount  = Sum of selected seat prices                  │  │
│  │ + Convenience Fee (calculated)                              │  │
│  │ + Tax (calculated)                                          │  │
│  │ − Discount (promo code, if any)                             │  │
│  │ ─────────────────────────────────                           │  │
│  │ = Total Amount                                              │  │
│  └─────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

---

### 6. Booking Cancellation & Refund Flow

```
┌──────┐        ┌──────────────┐      ┌────────────────┐      ┌───────────┐
│Client│        │UserBookingCtrl│     │CancellationSvc │      │WalletSvc  │
└──┬───┘        └──────┬───────┘      └───────┬────────┘      └─────┬─────┘
   │                    │                      │                     │
   │ GET /api/bookings/ │                      │                     │
   │ {ticketId}/        │                      │                     │
   │ refund-estimate    │                      │                     │
   │───────────────────>│  getRefundEstimate() │                     │
   │                    │─────────────────────>│                     │
   │                    │                      │                     │
   │                    │                      │ Fetch refund_rules  │
   │                    │                      │ from database       │
   │                    │                      │                     │
   │                    │                      │ Calculate hours     │
   │                    │                      │ since booking       │
   │                    │                      │                     │
   │                    │                      │ Find applicable     │
   │                    │                      │ rule (by priority)  │
   │                    │                      │                     │
   │ {refundAmount,     │                      │                     │
   │  refundPercentage, │  {estimate}          │                     │
   │  timeRemaining}    │<─────────────────────│                     │
   │<───────────────────│                      │                     │
   │                    │                      │                     │
   │ POST /api/bookings/│                      │                     │
   │ {ticketId}/cancel  │  cancelBooking()     │                     │
   │───────────────────>│─────────────────────>│                     │
   │                    │                      │ Mark ticket         │
   │                    │                      │ CANCELLED           │
   │                    │                      │                     │
   │                    │                      │ Release seats       │
   │                    │                      │                     │
   │                    │                      │ Credit refund       │
   │                    │                      │ to wallet           │
   │                    │                      │────────────────────>│
   │                    │                      │                     │
   │                    │                      │ Create refund       │
   │                    │                      │ transaction log     │
   │                    │                      │────────────────────>│
   │                    │                      │                     │
   │ {refundAmount,     │                      │                     │
   │  walletBalance}    │<─────────────────────│                     │
   │<───────────────────│                      │                     │
```

**Refund Policy (Database-Driven):**
```
refund_rules table:
┌────┬──────────────────┬──────────────────┬──────────┬──────────┐
│ id │ hours_threshold   │ refund_percentage │ priority │ is_active│
├────┼──────────────────┼──────────────────┼──────────┼──────────┤
│  1 │        1          │       100         │    1     │   true   │
│  2 │        6          │        75         │    2     │   true   │
│  3 │       12          │        50         │    3     │   true   │
│  4 │       24          │         0         │    4     │   true   │
└────┴──────────────────┴──────────────────┴──────────┴──────────┘
```

---

### 7. Wallet Operations Flow

```
┌────────────────────────────────────────────────────────────────────┐
│                    WALLET ECOSYSTEM                                 │
│                                                                    │
│  ┌─────────────┐      ┌──────────────┐      ┌──────────────────┐  │
│  │  user_wallets│      │   wallet_    │      │   Operations     │  │
│  │  (balance)  │◄────►│ transactions │      │                  │  │
│  └─────────────┘      │  (audit log) │      │  • CREDIT (add)  │  │
│                       └──────────────┘      │  • DEBIT (spend) │  │
│                                              │  • REFUND (return)│  │
│                                              └──────────────────┘  │
│                                                                    │
│  FLOW:                                                             │
│                                                                    │
│  ┌────────────┐  POST /add-money   ┌──────────────┐               │
│  │ Top-up via │─────────────────>│ Stripe charge │               │
│  │ Card (₹X)  │                   │ Create intent │               │
│  └────────────┘                   └──────┬───────┘               │
│                                          │ On success             │
│                                          ▼                        │
│                                   ┌──────────────┐                │
│                                   │ Credit wallet │                │
│                                   │ Update balance│                │
│                                   │ Log txn       │                │
│                                   │ (CREDIT type) │                │
│                                   └──────────────┘                │
│                                                                    │
│  TRANSACTION LOG (Immutable):                                      │
│  ┌────────┬────────┬────────┬──────────┬────────────────────────┐ │
│  │ userId │  type  │ amount │ balance  │ description            │ │
│  │        │        │        │ (after)  │                        │ │
│  ├────────┼────────┼────────┼──────────┼────────────────────────┤ │
│  │   1    │ CREDIT │ 10000  │  10000   │ Welcome bonus          │ │
│  │   1    │ DEBIT  │  500   │   9500   │ Booking #42            │ │
│  │   1    │ REFUND │  375   │   9875   │ Refund for ticket #42  │ │
│  │   1    │ CREDIT │ 2000   │  11875   │ Wallet top-up          │ │
│  └────────┴────────┴────────┴──────────┴────────────────────────┘ │
│                                                                    │
│  CONCURRENCY: @Version (Optimistic Locking) on wallet balance     │
│  prevents double-debit / race conditions                           │
└────────────────────────────────────────────────────────────────────┘
```

---

### 8. Food Ordering Flow

```
┌──────┐        ┌──────────────┐      ┌────────────┐      ┌─────────┐
│Client│        │FoodController│      │FoodService │      │Database │
└──┬───┘        └──────┬───────┘      └─────┬──────┘      └────┬────┘
   │                    │                    │                   │
   │ GET /api/food/     │                    │                   │
   │  menu/{theaterId}  │  getMenu()         │                   │
   │───────────────────>│───────────────────>│                   │
   │                    │                    │──────────────────>│
   │ [FoodItems by      │                    │                   │
   │  category:         │                    │                   │
   │  COMBO,POPCORN,    │                    │                   │
   │  BEVERAGE,SNACK,   │                    │                   │
   │  DESSERT]          │                    │                   │
   │<───────────────────│<───────────────────│                   │
   │                    │                    │                   │
   │ POST /api/food/    │                    │                   │
   │  order             │  placeOrder()      │                   │
   │ {theaterId,        │───────────────────>│                   │
   │  ticketId,         │                    │ Validate items    │
   │  seatNumber,       │                    │ Calculate total   │
   │  items: [{         │                    │ Create order      │
   │   foodItemId,      │                    │──────────────────>│
   │   quantity}]}      │                    │                   │
   │                    │                    │                   │
   │ FoodOrderResponse  │                    │                   │
   │ {orderId, total,   │<───────────────────│                   │
   │  status: PENDING}  │                    │                   │
   │<───────────────────│                    │                   │
   │                    │                    │                   │
   │              ORDER LIFECYCLE:                                │
   │    PENDING → CONFIRMED → PREPARING → DELIVERED              │
```

---

### 9. Parking Booking Flow

```
┌──────┐       ┌────────────────┐      ┌──────────────┐     ┌──────────────┐
│Client│       │ParkingController│     │ParkingService│     │PricingService│
└──┬───┘       └───────┬────────┘      └──────┬───────┘     └──────┬───────┘
   │                    │                      │                    │
   │ GET /api/parking/  │                      │                    │
   │  availability/     │  checkAvailability() │                    │
   │  {theaterId}       │─────────────────────>│                    │
   │───────────────────>│                      │                    │
   │ {twoWheeler: 20,   │                      │                    │
   │  fourWheeler: 50,  │<─────────────────────│                    │
   │  ev: 10}           │                      │                    │
   │<───────────────────│                      │                    │
   │                    │                      │                    │
   │ GET /api/parking/  │                      │                    │
   │  pricing           │  getPricing()        │                    │
   │───────────────────>│─────────────────────>│───────────────────>│
   │ {TWO_WHEELER:₹30/h │                      │                    │
   │  FOUR_WHEELER:₹50/h│<─────────────────────│<───────────────────│
   │  EV: ₹40/hr}      │                      │                    │
   │<───────────────────│                      │                    │
   │                    │                      │                    │
   │ POST /api/parking/ │                      │                    │
   │  book              │  bookParking()       │                    │
   │ {theaterId,        │─────────────────────>│                    │
   │  vehicleType,      │                      │ Find available     │
   │  durationHours,    │                      │ slot               │
   │  ticketId}         │                      │ Calculate fee      │
   │                    │                      │ Create ticket      │
   │                    │                      │                    │
   │ ParkingTicket      │                      │                    │
   │ {slotNo, fee,      │<─────────────────────│                    │
   │  status: BOOKED}   │                      │                    │
   │<───────────────────│                      │                    │
   │                    │                      │                    │
   │         PARKING LIFECYCLE:                                     │
   │    BOOKED → ACTIVE → COMPLETED                                │
   │                                                                │
   │    Grace period: 15 minutes                                    │
   │    Overstay: 1.5x multiplier                                  │
```

---

### 10. Admin Management Flow

```
┌─────────────────────────────────────────────────────────┐
│                 ADMIN CAPABILITIES                        │
│                                                          │
│  ┌─────────────┐    ┌──────────────┐    ┌────────────┐  │
│  │   Movies    │    │   Theaters   │    │   Shows    │  │
│  │  Management │    │  Management  │    │ Management │  │
│  ├─────────────┤    ├──────────────┤    ├────────────┤  │
│  │ POST /admin │    │ POST /admin  │    │ POST /admin│  │
│  │   /movies   │    │  /theaters   │    │   /shows   │  │
│  │ PUT  /admin │    │ PUT  /admin  │    │ PUT  /admin│  │
│  │  /movies/id │    │ /theaters/id │    │  /shows/id │  │
│  │ DELETE      │    │ DELETE       │    │ DELETE     │  │
│  │  /movies/id │    │ /theaters/id │    │  /shows/id │  │
│  └─────────────┘    └──────────────┘    └────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │              ANALYTICS DASHBOARD                  │   │
│  │                                                   │   │
│  │  GET /admin/analytics/dashboard    → KPIs         │   │
│  │  GET /admin/analytics/cities       → City stats   │   │
│  │  GET /admin/analytics/movies       → Movie perf   │   │
│  │  GET /admin/analytics/revenue/trends → Revenue    │   │
│  │  GET /admin/analytics/occupancy/trends → Seats    │   │
│  │  GET /admin/analytics/cancellation/trends         │   │
│  │  GET /admin/analytics/peak-times   → Scheduling   │   │
│  │  GET /admin/analytics/export/{type} → CSV export  │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │           MOVIE RECOMMENDATIONS                   │   │
│  │                                                   │   │
│  │  Admin ──recommend──> Theater Owner               │   │
│  │                                                   │   │
│  │  POST /admin/recommendations                      │   │
│  │  {movieId, theaterId, message}                    │   │
│  │                                                   │   │
│  │  Status: PENDING → ACCEPTED / REJECTED            │   │
│  │                                                   │   │
│  │  Theater Owner can:                               │   │
│  │  • Accept → Schedule shows for the movie          │   │
│  │  • Reject → Provide reason                        │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │           THEATER OWNER ASSIGNMENT                │   │
│  │                                                   │   │
│  │  PUT /admin/theaters/{id}/assign-admin            │   │
│  │  {userId}  → Promotes user to THEATER_OWNER       │   │
│  │             Assigns them to specific theater       │   │
│  │                                                   │   │
│  │  PUT /admin/theaters/{id}/remove-admin            │   │
│  │  → Revokes theater management access              │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

### 11. Theater Owner Operations Flow

```
┌─────────────────────────────────────────────────────────┐
│              THEATER OWNER DASHBOARD                      │
│                                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │  GET /owner/dashboard                            │    │
│  │  Returns: theater info, show count, booking      │    │
│  │  count, revenue, pending recommendations         │    │
│  └─────────────────────────────────────────────────┘    │
│                                                          │
│  RECOMMENDATION WORKFLOW:                                │
│  ┌──────────┐   ┌──────────┐   ┌────────────────────┐  │
│  │  Admin   │──>│ PENDING  │──>│ Theater Owner      │  │
│  │recommends│   │          │   │ reviews             │  │
│  └──────────┘   └──────────┘   └─────────┬──────────┘  │
│                                    ┌──────┴──────┐      │
│                                    ▼             ▼      │
│                              ┌──────────┐  ┌─────────┐  │
│                              │ ACCEPTED │  │REJECTED │  │
│                              │          │  │         │  │
│                              │ Schedule │  │ Provide │  │
│                              │ shows    │  │ reason  │  │
│                              └──────────┘  └─────────┘  │
│                                                          │
│  ANALYTICS (Theater-Specific):                           │
│  • Seat revenue breakdown                                │
│  • Time slot performance                                 │
│  • Weekly revenue trends                                 │
│  • Genre distribution                                    │
│  • Payment method analysis                               │
│  • Cancellation statistics                               │
└─────────────────────────────────────────────────────────┘
```

---

## 📐 System Design & UML Diagrams

### High-Level System Architecture

```
                           ┌──────────────────┐
                           │   Load Balancer   │
                           │   (Future)        │
                           └────────┬─────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            ┌──────────┐   ┌──────────┐   ┌──────────┐
            │  React   │   │  React   │   │  React   │
            │  (User)  │   │ (Admin)  │   │ (Owner)  │
            └────┬─────┘   └────┬─────┘   └────┬─────┘
                 │              │              │
                 └──────────────┼──────────────┘
                                │
                       ┌────────┴────────┐
                       │  Spring Boot    │
                       │  REST API       │
                       │  (Port 8080)    │
                       │                 │
                       │ ┌─────────────┐ │
                       │ │Spring       │ │
                       │ │Security     │ │
                       │ │+ JWT Filter │ │
                       │ └─────────────┘ │
                       │                 │
                       │ ┌─────────────┐ │
                       │ │ Service     │ │
                       │ │ Layer       │ │
                       │ └─────────────┘ │
                       │                 │
                       │ ┌─────────────┐ │
                       │ │ JPA/        │ │
                       │ │ Hibernate   │ │
                       │ └─────────────┘ │
                       └────────┬────────┘
                                │
                     ┌──────────┼──────────┐
                     ▼          ▼          ▼
              ┌──────────┐ ┌────────┐ ┌────────┐
              │  MySQL   │ │ Stripe │ │ Gmail  │
              │  8.0     │ │  API   │ │  SMTP  │
              └──────────┘ └────────┘ └────────┘
```

---

### Entity Relationship Diagram

```
┌───────────────────┐       ┌───────────────────┐       ┌───────────────────┐
│      users        │       │      movies       │       │     theaters      │
├───────────────────┤       ├───────────────────┤       ├───────────────────┤
│ id (PK)           │       │ id (PK)           │       │ id (PK)           │
│ name              │       │ movieName         │       │ name              │
│ email (UNIQUE)    │       │ genre (ENUM)      │       │ address           │
│ password (BCrypt) │       │ language (ENUM)   │       │ city_id (FK)      │
│ mobile            │       │ rating            │       │ owner_id (FK)     │
│ age               │       │ duration          │       │ totalSeats        │
│ gender (ENUM)     │       │ description       │       │ created_at        │
│ role (ENUM)       │       │ director          │       └─────────┬─────────┘
│ created_at        │       │ cast              │                 │
│ updated_at        │       │ posterUrl         │                 │ 1:N
└─────────┬─────────┘       │ trailerUrl        │                 │
          │                 │ nowShowing        │       ┌─────────┴─────────┐
          │ 1:1             │ created_at        │       │   theater_seats   │
          │                 └─────────┬─────────┘       ├───────────────────┤
┌─────────┴─────────┐               │                  │ id (PK)           │
│   user_wallets    │               │ 1:N              │ seatNo            │
├───────────────────┤               │                  │ seatType (ENUM)   │
│ id (PK)           │     ┌─────────┴─────────┐       │ price             │
│ user_id (FK,UNQ)  │     │      shows        │       │ theater_id (FK)   │
│ balance           │     ├───────────────────┤       └───────────────────┘
│ version (@Version)│     │ id (PK)           │
│ created_at        │     │ date              │       ┌───────────────────┐
│ updated_at        │     │ time              │       │    show_seats     │
└─────────┬─────────┘     │ movie_id (FK)     │       ├───────────────────┤
          │               │ theater_id (FK)   │       │ id (PK)           │
          │ 1:N           │ created_at        │──────>│ seatNo            │
          │               └─────────┬─────────┘  1:N  │ seatType (ENUM)  │
┌─────────┴─────────┐             │               │ price             │
│wallet_transactions│             │ 1:N           │ isAvailable       │
├───────────────────┤             │               │ isFoodContains    │
│ id (PK)           │   ┌─────────┴─────────┐    │ show_id (FK)      │
│ wallet_id (FK)    │   │     tickets       │    └───────────────────┘
│ type (ENUM)       │   ├───────────────────┤
│ amount            │   │ id (PK)           │    ┌───────────────────┐
│ balanceAfter      │   │ bookedSeats       │    │    seat_locks     │
│ description       │   │ totalAmount       │    ├───────────────────┤
│ created_at        │   │ status (ENUM)     │    │ id (PK)           │
└───────────────────┘   │ qrCodeData        │    │ show_id (FK)      │
                        │ show_id (FK)      │    │ seatNumber        │
                        │ user_id (FK)      │    │ user_id (FK)      │
┌───────────────────┐   │ created_at        │    │ lockTime          │
│    payments       │   └─────────────────┘    │ expiryTime        │
├───────────────────┤                           │ status (ENUM)     │
│ id (PK)           │    ┌───────────────────┐  │ sessionId         │
│ transactionId(UNQ)│    │   refund_rules    │  └───────────────────┘
│ sessionId         │    ├───────────────────┤
│ user_id (FK)      │    │ id (PK)           │  ┌───────────────────┐
│ ticket_id (FK)    │    │ hoursThreshold    │  │movie_recommendations│
│ baseAmount        │    │ refundPercentage  │  ├───────────────────┤
│ convenienceFee    │    │ description       │  │ id (PK)           │
│ tax               │    │ isActive          │  │ movie_id (FK)     │
│ totalAmount       │    │ priority          │  │ theater_id (FK)   │
│ discountAmount    │    └───────────────────┘  │ status            │
│ paymentMethod     │                           │ adminMessage      │
│ walletAmount      │    ┌───────────────────┐  │ response          │
│ cardAmount        │    │   food_items      │  │ recommended_by(FK)│
│ status (ENUM)     │    ├───────────────────┤  └───────────────────┘
│ gatewayTxnId      │    │ id (PK)           │
│ refundAmount      │    │ name              │  ┌───────────────────┐
│ refundedAt        │    │ category (ENUM)   │  │  parking_tickets  │
│ created_at        │    │ price             │  ├───────────────────┤
└───────────────────┘    │ theater_id (FK)   │  │ id (PK)           │
                         │ isAvailable       │  │ slot_id (FK)      │
                         └──────────┬────────┘  │ user_id (FK)      │
                                    │ 1:N       │ vehicleType(ENUM) │
                         ┌──────────┴────────┐  │ durationHours     │
                         │  food_order_items │  │ fee               │
                         ├───────────────────┤  │ status (ENUM)     │
                         │ id (PK)           │  │ ticket_id (FK)    │
                         │ order_id (FK)     │  └───────────────────┘
                         │ foodItem_id (FK)  │
                         │ quantity          │
                         │ subtotal          │
                         └───────────────────┘
```

---

### Booking Sequence Diagram

```
┌──────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────┐
│Client│  │SeatLock  │  │ AddOns   │  │ Payment  │  │ Ticket   │  │QRCode   │  │Email │
│      │  │Service   │  │Service   │  │Service   │  │Service   │  │Service   │  │Svc   │
└──┬───┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──┬───┘
   │           │             │             │             │             │           │
   │ 1.Lock    │             │             │             │             │           │
   │  seats    │             │             │             │             │           │
   │──────────>│             │             │             │             │           │
   │           │             │             │             │             │           │
   │ sessionId │             │             │             │             │           │
   │<──────────│             │             │             │             │           │
   │           │             │             │             │             │           │
   │ 2.Select food/parking   │             │             │             │           │
   │  (optional)             │             │             │             │           │
   │────────────────────────>│             │             │             │           │
   │           │             │             │             │             │           │
   │ addon summary           │             │             │             │           │
   │<────────────────────────│             │             │             │           │
   │           │             │             │             │             │           │
   │ 3.Initiate payment      │             │             │             │           │
   │ (ticket + addons)       │             │             │             │           │
   │──────────────────────────────────────>│             │             │           │
   │           │             │             │             │             │           │
   │           │             │             │ 4.Charge    │             │           │
   │           │             │             │  Stripe/    │             │           │
   │           │             │             │  Wallet     │             │           │
   │           │             │             │             │             │           │
   │           │             │             │ 5.Create    │             │           │
   │           │             │             │  ticket     │             │           │
   │           │             │             │────────────>│             │           │
   │           │             │             │             │             │           │
   │           │             │             │             │ 6.Generate  │           │
   │           │             │             │             │  QR (unified│           │
   │           │             │             │             │  ticket+    │           │
   │           │             │             │             │  food+park) │           │
   │           │             │             │             │────────────>│           │
   │           │             │             │             │             │           │
   │           │             │             │             │ 7.Send      │           │
   │           │             │             │             │  email      │           │
   │           │             │             │             │─────────────────────────>│
   │           │             │             │             │             │           │
   │  8.Booking Confirmation (ticket + QR + payment + addons)        │           │
   │<─────────────────────────────────────────────────────────────────│           │
```

---

### Payment State Machine

```
                    ┌──────────────────────────────────────┐
                    │       PAYMENT STATE MACHINE           │
                    └──────────────────────────────────────┘

                              ┌─────────┐
                  Initiate    │         │
              ───────────────>│ PENDING │
                              │         │
                              └────┬────┘
                                   │
                          Process  │
                                   ▼
                              ┌──────────┐
                              │PROCESSING│
                              └────┬─────┘
                                   │
                     ┌─────────────┼─────────────┐
                     │             │             │
                Success      Failure        Timeout
                     │             │             │
                     ▼             ▼             ▼
               ┌─────────┐  ┌──────────┐  ┌──────────┐
               │ SUCCESS │  │  FAILED  │  │CANCELLED │
               └────┬────┘  └──────────┘  └──────────┘
                    │
               Refund request
                    │
                    ▼
               ┌──────────┐
               │ REFUNDED │
               └──────────┘
```

---

### Seat Lock State Machine

```
                              ┌─────────────────────────┐
                              │  SEAT LOCK STATE MACHINE │
                              └─────────────────────────┘

              Lock Request
              (showId, seatNos, userId)
                    │
                    ▼
        ┌───────────────────────┐
        │   Check availability  │
        │   for each seat       │
        └───────────┬───────────┘
                    │
          ┌─────────┴─────────┐
          │                   │
     Available            Locked by
                          another user
          │                   │
          ▼                   ▼
   ┌────────────┐      ┌───────────┐
   │   LOCKED   │      │   ERROR   │
   │ (TTL=10min)│      │ "Seat     │
   └──────┬─────┘      │ already   │
          │            │ locked"   │
          │            └───────────┘
    ┌─────┼──────────────┐
    │     │              │
 Payment  │ Timeout    Manual
 success  │ (10 min)   release
    │     │              │
    ▼     ▼              ▼
┌───────┐ ┌───────────┐
│CONFIRM│ │ RELEASED  │
│  ED   │ │           │
└───────┘ └───────────┘
    │
    ▼
 Seats marked
 isAvailable=false
 in show_seats
```

---

### Class Diagram (Core Domain)

```
┌─────────────────────────────────────────────────────────────────┐
│                     DOMAIN MODEL                                 │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐          ┌──────────────────┐
│     <<Entity>>   │          │    <<Entity>>    │
│       User       │          │      Movie       │
├──────────────────┤          ├──────────────────┤
│- id: Integer     │          │- id: Integer     │
│- name: String    │          │- movieName: Str  │
│- email: String   │          │- genre: Genre    │
│- password: String│          │- language: Lang  │
│- mobile: String  │          │- rating: Double  │
│- age: Integer    │          │- duration: Int   │
│- gender: Gender  │          │- director: Str   │
│- role: UserRole  │          │- cast: String    │
│- ticketList: []  │          │- posterUrl: Str  │
└────────┬─────────┘          │- nowShowing: Bool│
         │                    │- shows: List     │
         │ 1                  └────────┬─────────┘
         │                             │ 1
         │ N                           │ N
┌────────┴─────────┐          ┌────────┴─────────┐
│    <<Entity>>    │          │    <<Entity>>    │
│     Ticket       │          │      Show        │
├──────────────────┤          ├──────────────────┤
│- id: Integer     │          │- id: Integer     │
│- bookedSeats: Str│  N    1  │- date: Date      │
│- totalAmount: Dbl│◄────────>│- time: Time      │
│- status: TktStat │          │- movie: Movie    │
│- qrCodeData: Str │          │- theater: Theater│
│- user: User      │          │- showSeatList: []│
│- show: Show      │          │- ticketList: []  │
└──────────────────┘          └──────────────────┘
                                       │ 1
                                       │ N
                              ┌────────┴─────────┐
                              │    <<Entity>>    │
                              │    ShowSeat      │
                              ├──────────────────┤
                              │- id: Integer     │
                              │- seatNo: String  │
                              │- seatType: Enum  │
                              │- price: Integer  │
                              │- isAvailable:Bool│
                              │- show: Show      │
                              └──────────────────┘

┌──────────────────┐          ┌──────────────────┐
│    <<Entity>>    │          │    <<Entity>>    │
│    Payment       │          │    SeatLock      │
├──────────────────┤          ├──────────────────┤
│- transactionId   │          │- show: Show      │
│- sessionId       │          │- seatNumber: Str │
│- baseAmount      │          │- user: User      │
│- convenienceFee  │          │- lockTime: LDT   │
│- tax             │          │- expiryTime: LDT │
│- totalAmount     │          │- status: Enum    │
│- paymentMethod   │          │- sessionId: Str  │
│- walletAmount    │          ├──────────────────┤
│- cardAmount      │          │+ isExpired(): bool│
│- status: PayStat │          │+ isActive(): bool │
│+ calculateTotal()│          └──────────────────┘
└──────────────────┘

┌──────────────────┐          ┌──────────────────┐
│    <<Entity>>    │          │    <<Entity>>    │
│   UserWallet     │          │WalletTransaction │
├──────────────────┤          ├──────────────────┤
│- user: User      │ 1     N │- wallet: Wallet  │
│- balance: Double │◄────────>│- type: TxnType   │
│- version: Long   │          │- amount: Double  │
│ (@Version OCC)   │          │- balanceAfter:Dbl│
└──────────────────┘          │- description: Str│
                              └──────────────────┘

┌──────────────┐    ┌──────────────┐    ┌────────────────┐
│  <<Enum>>    │    │  <<Enum>>    │    │   <<Enum>>     │
│  SeatType    │    │ PaymentMethod│    │  TicketStatus  │
├──────────────┤    ├──────────────┤    ├────────────────┤
│ CLASSIC      │    │ CREDIT_CARD  │    │ BOOKED         │
│ PREMIUM      │    │ DEBIT_CARD   │    │ CANCELLED      │
│ GOLD         │    │ UPI          │    └────────────────┘
│ SILVER       │    │ NET_BANKING  │
│ COUPLE       │    │ WALLET       │    ┌────────────────┐
└──────────────┘    │ STRIPE       │    │   <<Enum>>     │
                    │ CASH         │    │ SeatLockStatus │
                    │WALLET_CARD   │    ├────────────────┤
                    │  _SPLIT      │    │ LOCKED         │
                    └──────────────┘    │ RELEASED       │
                                        │ CONFIRMED      │
                                        └────────────────┘
```

---

##  Design Patterns Used

### 1. Repository Pattern
**Where:** All `*Repository.java` interfaces  
**Why:** Abstracts data access logic from business logic. Spring Data JPA auto-generates SQL queries from method signatures.

```
Controller → Service → Repository → Database
                         ↕
              Spring Data JPA generates
              SQL at runtime from method names
              e.g., findByShowIdAndStatus(id, status)
```

### 2. Service Layer Pattern
**Where:** All `*Service.java` classes  
**Why:** Encapsulates all business logic between controllers and repositories. Controllers stay thin (just HTTP handling), services handle validation, orchestration, and business rules.

```
┌──────────┐     ┌────────────┐     ┌────────────┐
│Controller│────>│  Service   │────>│ Repository │
│ (HTTP)   │     │ (Business  │     │ (Data)     │
│          │     │  Logic)    │     │            │
└──────────┘     └────────────┘     └────────────┘
   Thin              Thick              Auto-gen
```

### 3. DTO (Data Transfer Object) Pattern
**Where:** `Dtos/RequestDtos/`, `Dtos/ResponseDtos/`, `Transformers/`  
**Why:** Separates internal entity representation from API contracts. Prevents exposing internal fields (passwords, relationships) to clients.

```
Client ──> RequestDTO ──> Transformer ──> Entity (DB)
Client <── ResponseDTO <── Transformer <── Entity (DB)

Example:
  SignupRequestDto → UserTransformer.toEntity() → User (saved)
  User (fetched) → UserTransformer.toDto() → ReturnUserDto
```

### 4. Builder Pattern
**Where:** Every `@Entity` and `@Data` class via Lombok `@Builder`  
**Why:** Fluent object creation for entities with many fields, avoiding telescoping constructors.

```java
Payment payment = Payment.builder()
    .transactionId(UUID.randomUUID().toString())
    .sessionId(sessionId)
    .baseAmount(baseAmount)
    .convenienceFee(fee)
    .status(PaymentStatus.PENDING)
    .build();
```

### 5. Strategy Pattern
**Where:** Payment processing — multiple payment methods handled polymorphically  
**Why:** The same payment initiation flow works regardless of payment method (Card, UPI, Wallet, Stripe, Split). Each method has its own processing logic.

```
┌──────────────────┐
│ PaymentService   │
│                  │
│ process(method)  │──── switch(method)
└──────────────────┘        │
         ┌──────────────────┼──────────────────┐
         ▼                  ▼                  ▼
   ┌───────────┐     ┌───────────┐     ┌───────────┐
   │  Stripe   │     │  Wallet   │     │   Split   │
   │  Strategy │     │  Strategy │     │  Strategy │
   └───────────┘     └───────────┘     └───────────┘
```

### 6. Observer Pattern (Async Events)
**Where:** `EmailService` — sends notifications asynchronously after booking/cancellation  
**Why:** Booking completion triggers email notification without blocking the response. Uses Spring's `@Async` for non-blocking execution.

```
BookingService.complete() ──async──> EmailService.sendConfirmation()
                          ──async──> EmailService.sendCancellation()
                          (non-blocking, doesn't delay response)
```

### 7. Template Method Pattern
**Where:** Booking flow — fixed sequence of steps with customizable payment  
**Why:** The booking flow always follows: Lock → Validate → Pay → Ticket → QR → Email, but payment varies by method.

```
bookTicket() {
  1. validateSeatLock()      ← fixed
  2. calculatePrice()        ← fixed
  3. processPayment()        ← varies (Strategy)
  4. createTicket()          ← fixed
  5. generateQR()            ← fixed
  6. sendEmail()             ← fixed (async)
}
```

### 8. Factory Pattern
**Where:** `MockPaymentGateway` — creates payment transactions  
**Why:** Centralizes payment transaction creation with consistent structure.

### 9. Singleton Pattern
**Where:** All Spring `@Service`, `@Repository`, `@Component` beans  
**Why:** Spring IoC manages bean lifecycle as singletons by default, ensuring single instance per service class.

### 10. MVC / REST Architecture
**Where:** Entire backend  
**Why:** Clean separation — Models (JPA entities), Views (React frontend), Controllers (REST endpoints). Stateless REST design with JSON payloads.

### 11. Optimistic Locking (Concurrency Control)
**Where:** `UserWallet.version` (`@Version` annotation)  
**Why:** Prevents race conditions when two operations modify the same wallet balance simultaneously. If versions mismatch, the transaction is retried.

```
Thread A reads wallet (version=1, balance=10000)
Thread B reads wallet (version=1, balance=10000)
Thread A debits ₹500 → version=2, balance=9500 
Thread B debits ₹300 → version still 1  OptimisticLockException → Retry
```

### 12. Idempotent Operations
**Where:** `Payment.transactionId` (UNIQUE constraint)  
**Why:** Prevents duplicate payments. If a client retries with the same `transactionId`, the system returns the existing payment instead of creating a new one.

### 13. Policy-as-Code (Database-Driven Rules)
**Where:** `RefundRule` entity — refund percentages stored in DB, not code  
**Why:** Business rules can be changed via database updates without deploying new code.

```
Code:  RefundPolicyService.getRefundPercentage(hoursElapsed)
       → Queries refund_rules table
       → Returns applicable percentage
       → Zero hardcoded values
```

### 14. Session-Based Tracking
**Where:** `SeatLock.sessionId`, `Payment.sessionId`  
**Why:** Links the entire booking flow (seat lock → payment → ticket) through a single session identifier.

### 15. Orchestrator Pattern
**Where:** `PaymentAddOnOrchestrationService`  
**Why:** Coordinates complex multi-step flows (ticket + food + parking payment) through a single orchestration service.

```
PaymentAddOnOrchestrator
  ├── PaymentService.process()
  ├── FoodService.createOrder()
  ├── ParkingService.book()
  └── ReceiptService.generate()
```

---

## 📡 API Reference

### Authentication
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/auth/signup` | Register new user | Public |
| `POST` | `/auth/login` | Login & get JWT tokens | Public |
| `POST` | `/auth/refresh` | Refresh access token | Public |
| `GET` | `/auth/health` | Health check | Public |

### Movies
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/movies/now-showing` | List now-showing movies | Public |
| `GET` | `/api/movies/upcoming` | List upcoming movies | Public |
| `GET` | `/api/movies/search?keyword=` | Search movies | Public |
| `GET` | `/api/movies/filter/genre?genre=ACTION` | Filter by genre | Public |
| `GET` | `/api/movies/filter/language?lang=HINDI` | Filter by language | Public |
| `GET` | `/api/movies/filter/rating?minRating=7` | Filter by rating | Public |
| `GET` | `/api/movies/city/{city}` | Movies by city | Public |
| `GET` | `/api/movies/filter/advanced` | Multi-filter search | Public |
| `GET` | `/api/movies/{movieId}/shows` | Shows for a movie | Public |

### Shows
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/show/by-movie/{movieId}` | Shows by movie | Public |
| `GET` | `/show/by-theater/{theaterId}` | Shows by theater | Public |
| `GET` | `/show/{showId}` | Show details | Public |
| `GET` | `/show/{showId}/seats/availability` | Seat availability | Public |
| `POST` | `/show/addNew` | Create new show | Admin |

### Seat Locking
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/seat-locks/lock` | Lock seats (10 min TTL) | User |
| `POST` | `/api/seat-locks/release/{sessionId}` | Release locks | User |
| `GET` | `/api/seat-locks/show/{showId}/locked-seats` | Locked seats for show | Public |
| `POST` | `/api/seat-locks/check-availability` | Check seat availability | Public |
| `GET` | `/api/seat-locks/session/{sessionId}/remaining-time` | Lock time remaining | User |
| `POST` | `/api/seat-locks/session/{sessionId}/extend` | Extend lock (+5 min) | User |

### Payments
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/payment/initiate` | Initiate payment | User |
| `POST` | `/api/payment/process/{transactionId}` | Process payment | User |
| `GET` | `/api/payment/status/{transactionId}` | Get payment status | User |
| `POST` | `/api/payment/refund/{paymentId}` | Request refund | User |
| `GET` | `/api/payment/stripe-config` | Get Stripe publishable key | Public |
| `POST` | `/api/payment/create-stripe-intent` | Create Stripe PaymentIntent | User |
| `POST` | `/api/payment/confirm-stripe` | Confirm Stripe payment | User |
| `POST` | `/api/payment/create-split-payment-intent` | Split wallet+card | User |
| `POST` | `/api/payment/confirm-split-payment` | Confirm split payment | User |
| `POST` | `/api/payment/with-addons` | Payment w/ food+parking | User |

### Bookings
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/bookings/user/{userId}` | User's bookings | User |
| `GET` | `/api/bookings/user/{userId}/upcoming` | Upcoming bookings | User |
| `GET` | `/api/bookings/user/{userId}/past` | Past bookings | User |
| `GET` | `/api/bookings/user/{userId}/count` | Booking count | User |
| `GET` | `/api/bookings/{ticketId}/refund-estimate` | Refund estimate | User |
| `POST` | `/api/bookings/{ticketId}/cancel` | Cancel booking | User |

### Wallet
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/wallet/balance/{userId}` | Get wallet balance | User |
| `POST` | `/api/wallet/add-money` | Top-up via card | User |
| `GET` | `/api/wallet/transactions/{userId}` | Transaction history | User |
| `POST` | `/api/user-wallet/{userId}/credit` | Credit wallet | Service |
| `POST` | `/api/user-wallet/{userId}/debit` | Debit wallet | Service |

### Food
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/food/menu/{theaterId}` | Theater food menu | Public |
| `POST` | `/api/food/order` | Place food order | User |

### Parking
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/parking/availability/{theaterId}` | Check availability | Public |
| `GET` | `/api/parking/pricing` | Get pricing rates | Public |
| `POST` | `/api/parking/book` | Book parking slot | User |
| `PUT` | `/api/parking/activate/{ticketId}` | Activate parking | User |
| `PUT` | `/api/parking/complete/{ticketId}` | Complete parking | User |

### Admin (Requires `ADMIN` Role)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/admin/dashboard` | Dashboard KPIs |
| `POST` | `/admin/movies` | Add movie |
| `PUT` | `/admin/movies/{id}` | Update movie |
| `DELETE` | `/admin/movies/{id}` | Delete movie |
| `POST` | `/admin/theaters` | Add theater |
| `PUT` | `/admin/theaters/{id}/assign-admin` | Assign theater owner |
| `POST` | `/admin/shows` | Create show |
| `POST` | `/admin/recommendations` | Recommend movie to theater |
| `GET` | `/admin/analytics/*` | Various analytics endpoints |
| `POST` | `/admin/food` | Add food item |
| `PUT` | `/admin/parking/{slotId}` | Manage parking slots |

### Theater Owner (Requires `THEATER_OWNER` Role)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/owner/dashboard` | Theater dashboard |
| `GET` | `/owner/theatre` | Theater details |
| `GET` | `/owner/recommendations/pending` | Pending recommendations |
| `POST` | `/owner/recommendations/{id}/accept` | Accept recommendation |
| `POST` | `/owner/recommendations/{id}/reject` | Reject recommendation |
| `POST` | `/owner/shows` | Schedule a show |
| `GET` | `/owner/analytics` | Theater analytics |
| `GET` | `/owner/analytics/seat-revenue` | Revenue by seat type |
| `GET` | `/owner/analytics/weekly-revenue` | Weekly revenue |

---

## 🗄 Database Schema

### Core Tables

| Table | Description | Key Columns |
|-------|-------------|-------------|
| `users` | User accounts | id, name, email, password, role, mobile, gender, age |
| `movies` | Movie catalog | id, movieName, genre, language, rating, duration, posterUrl, nowShowing |
| `theaters` | Theater venues | id, name, address, city_id, owner_id, totalSeats |
| `shows` | Movie screenings | id, date, time, movie_id, theater_id |
| `show_seats` | Seats per show | id, seatNo, seatType, price, isAvailable, show_id |
| `theater_seats` | Seat templates | id, seatNo, seatType, price, theater_id |
| `tickets` | Booked tickets | id, bookedSeats, totalAmount, status, qrCodeData, show_id, user_id |
| `payments` | Payment transactions | id, transactionId, sessionId, totalAmount, paymentMethod, status |
| `seat_locks` | Temporary reservations | id, show_id, seatNumber, user_id, lockTime, expiryTime, status |
| `cities` | City master data | id, name |

### Wallet Tables

| Table | Description | Key Columns |
|-------|-------------|-------------|
| `user_wallets` | User balance | id, user_id, balance, version |
| `wallet_transactions` | Immutable audit log | id, wallet_id, type, amount, balanceAfter, description |

### Module Tables

| Table | Description | Key Columns |
|-------|-------------|-------------|
| `food_items` | Menu items | id, name, category, price, theater_id |
| `food_orders` | Food orders | id, user_id, theater_id, status, totalAmount |
| `food_order_items` | Order line items | id, order_id, foodItem_id, quantity, subtotal |
| `parking_lots` | Parking venues | id, theater_id, totalSlots |
| `parking_slots` | Individual slots | id, lot_id, slotNumber, vehicleType, isOccupied |
| `parking_tickets` | Parking bookings | id, slot_id, user_id, vehicleType, fee, status |

### Policy & Config Tables

| Table | Description | Key Columns |
|-------|-------------|-------------|
| `refund_rules` | Refund policy rules | id, hoursThreshold, refundPercentage, priority, isActive |
| `movie_recommendations` | Admin → Theater suggestions | id, movie_id, theater_id, status, adminMessage |
| `static_pages` | CMS content | id, pageKey, title, content |
| `payment_addons` | Add-on tracking | id, sessionId, addonType, status |
| `receipts` | Generated receipts | id, ticket_id, type, qrCode, status |

---

##  Getting Started

### Prerequisites

- **Java 17** (JDK)
- **Maven 3.8+** (or use the included `mvnw` wrapper)
- **MySQL 8.0+**
- **Node.js 18+** and **npm** (for frontend)
- **Stripe test account** (free) — [Sign up here](https://dashboard.stripe.com/register)

### 1. Clone the Repository

```bash
git clone https://github.com/<your-username>/BookMyShow.git
cd BookMyShow
```

### 2. Database Setup

```bash
# Login to MySQL
mysql -u root -p

# Create database and user
CREATE DATABASE bookmyshow;
CREATE USER 'springuser'@'localhost' IDENTIFIED BY 'springpass123';
GRANT ALL PRIVILEGES ON bookmyshow.* TO 'springuser'@'localhost';
FLUSH PRIVILEGES;
```

> **Note:** Tables are auto-created by JPA/Hibernate on first run (`spring.jpa.hibernate.ddl-auto=update`).

### 3. Configure Environment Variables

```bash
cd Book-My-Show
cp .env.example .env
```

Edit `.env` with your configuration:

```env
# Database
DB_USERNAME=springuser
DB_PASSWORD=springpass123

# Admin (auto-seeded on startup)
MAIN_ADMIN_EMAIL=admin@gmail.com
MAIN_ADMIN_PASSWORD=password123

# JWT
JWT_SECRET=YourSuperSecureRandomJwtSecretKeyHere

# Gmail SMTP (for email notifications)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password

# Stripe (get from https://dashboard.stripe.com/test/apikeys)
STRIPE_SECRET_KEY=sk_test_your_key
STRIPE_PUBLISHABLE_KEY=pk_test_your_key
```

### 4. Start the Backend

```bash
cd Book-My-Show
./mvnw spring-boot:run
```

Backend runs on **http://localhost:8080**

> Swagger UI available at: **http://localhost:8080/swagger-ui.html**

### 5. Start the Frontend

```bash
cd Book-My-Show/frontend
npm install
npm run dev
```

Frontend runs on **http://localhost:5173**

### 6. Initialize Sample Data

On first startup, the application automatically:
- Creates default admin user
- Seeds 20+ Indian movies (Pushpa 2, Kalki 2898 AD, Stree 2, etc.)
- Creates 35+ theaters across 8 cities
- Sets up refund policy rules
- Initializes parking configurations

---

##  Project Structure

```
BookMyShow/
├── Book-My-Show/
│   ├── .env.example                    # Environment template
│   ├── pom.xml                         # Maven dependencies
│   ├── mvnw                            # Maven wrapper
│   │
│   ├── src/main/java/com/driver/bookMyShow/
│   │   ├── BookMyShowApplication.java  # Entry point
│   │   │
│   │   ├── Config/                     # Configuration
│   │   │   ├── SecurityConfig.java     # Spring Security + JWT setup
│   │   │   ├── CorsConfig.java         # CORS policy
│   │   │   ├── DotenvConfig.java       # .env file loader
│   │   │   └── WebMvcConfig.java       # Static resource mapping
│   │   │
│   │   ├── Security/                   # Authentication
│   │   │   ├── JwtAuthenticationFilter.java  # JWT request filter
│   │   │   └── CustomUserDetailsService.java # User loader
│   │   │
│   │   ├── Controllers/                # REST endpoints (20+ controllers)
│   │   │   ├── AuthController.java     # /auth/*
│   │   │   ├── MovieSearchController.java  # /api/movies/*
│   │   │   ├── SeatLockController.java # /api/seat-locks/*
│   │   │   ├── PaymentController.java  # /api/payment/*
│   │   │   ├── UserBookingController.java  # /api/bookings/*
│   │   │   ├── WalletController.java   # /api/wallet/*
│   │   │   ├── AdminController.java    # /admin/*
│   │   │   ├── TheatreAdminController.java # /owner/*
│   │   │   └── ...
│   │   │
│   │   ├── Services/                   # Business logic (22+ services)
│   │   │   ├── AuthService.java
│   │   │   ├── BookingService.java
│   │   │   ├── SeatLockService.java
│   │   │   ├── PaymentService.java
│   │   │   ├── StripePaymentService.java
│   │   │   ├── WalletService.java
│   │   │   ├── RefundPolicyService.java
│   │   │   ├── BookingCancellationService.java
│   │   │   ├── EmailService.java
│   │   │   ├── UnifiedQrCodeService.java
│   │   │   ├── AdminAnalyticsService.java
│   │   │   └── ...
│   │   │
│   │   ├── Models/                     # JPA entities (15+ entities)
│   │   │   ├── User.java
│   │   │   ├── Movie.java
│   │   │   ├── Theater.java
│   │   │   ├── Show.java
│   │   │   ├── ShowSeat.java
│   │   │   ├── Ticket.java
│   │   │   ├── Payment.java
│   │   │   ├── SeatLock.java
│   │   │   ├── UserWallet.java
│   │   │   ├── WalletTransaction.java
│   │   │   ├── RefundRule.java
│   │   │   └── ...
│   │   │
│   │   ├── Repositories/              # Data access (15+ repos)
│   │   ├── Enums/                      # Type-safe enumerations (10)
│   │   ├── Dtos/                       # Request/Response DTOs
│   │   ├── Transformers/              # Entity ↔ DTO converters
│   │   ├── Exceptions/                # Custom exceptions
│   │   ├── Gateway/                   # Payment gateway
│   │   ├── Utils/                     # JWT utility
│   │   └── common/                    # Shared (ApiResponse, GlobalExHandler)
│   │
│   │   ├── modules/                   # Feature modules
│   │   │   ├── food/                  # Food ordering module
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── entity/
│   │   │   │   ├── dto/
│   │   │   │   ├── enums/
│   │   │   │   └── repository/
│   │   │   │
│   │   │   ├── parking/               # Parking module
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── entity/
│   │   │   │   ├── dto/
│   │   │   │   ├── enums/
│   │   │   │   ├── config/
│   │   │   │   └── repository/
│   │   │   │
│   │   │   ├── addons/                # Payment add-ons orchestration
│   │   │   └── receipt/               # Receipt & QR generation
│   │
│   ├── src/main/resources/
│   │   ├── application.properties     # App configuration
│   │   └── static/uploads/            # Uploaded images
│   │
│   ├── database/                      # SQL scripts
│   │   ├── apply_schema.sql
│   │   ├── init_default_data.sql
│   │   ├── insert_sample_data.sql
│   │   └── ...
│   │
│   └── frontend/                      # React application
│       ├── package.json
│       ├── vite.config.js
│       └── src/
│           ├── App.jsx                # Route definitions
│           ├── main.jsx               # Entry point
│           ├── context/               # React Context (Auth, App)
│           ├── services/              # Axios API client
│           ├── pages/
│           │   ├── Auth/              # Login, Signup
│           │   ├── Home/              # Landing page
│           │   ├── Movie/             # Movie listing & details
│           │   ├── Booking/           # 5-step booking flow
│           │   ├── User/              # Profile & history
│           │   ├── Admin/             # Admin dashboard
│           │   └── TheaterOwner/      # Owner dashboard
│           └── components/
│               ├── Layout/            # Navbar, Footer
│               ├── Movie/             # MovieCard, FilterBar
│               └── ...
│
├── .gitignore
└── README.md
```

---

##  Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_USERNAME` | MySQL username | Yes |
| `DB_PASSWORD` | MySQL password | Yes |
| `MAIN_ADMIN_EMAIL` | Default admin email (seeded on startup) | Yes |
| `MAIN_ADMIN_PASSWORD` | Default admin password | Yes |
| `THEATRE_ADMIN_EMAIL` | Default theater owner email | Yes |
| `THEATRE_ADMIN_PASSWORD` | Default theater owner password | Yes |
| `JWT_SECRET` | Secret key for JWT token signing | Yes |
| `MAIL_USERNAME` | Gmail address for SMTP | Optional |
| `MAIL_PASSWORD` | Gmail app-specific password | Optional |
| `STRIPE_SECRET_KEY` | Stripe test secret key | Optional |
| `STRIPE_PUBLISHABLE_KEY` | Stripe test publishable key | Optional |

---

<p align="center">
  Built with ❤️ using Spring Boot & React
</p>
