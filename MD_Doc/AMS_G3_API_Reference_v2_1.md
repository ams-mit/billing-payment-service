# GROUP 3 — API REFERENCE

**billing-payment-service · utility-charge-service**

Version 2.1 | Apartment Management System | Project A | AMS-G3
University of Kelaniya | Software Architecture and Process Models

Base URL: `/api/v1` | Security Standard: `Project-A-JWT-Authentication-v1`

## v2.1 corrections from v2.0

- Header name fixed: `Authorization: Bearer <JWT>` for all requests (removed `X-Service-Token` / `X-Gateway-Token`)
- Token expiry fixed: Service JWT and Gateway JWT are 5 minutes (not 30 seconds)
- JWT claim fixed: `type: service` (not `purpose: internal`)
- Role names aligned to canonical registry: `FINANCE_OFFICER`, `APARTMENT_MANAGER`, `RESIDENT`, `TENANT`, `OWNER`
- API Type column aligned to canonical registry: `USER`, `INTERNAL`, `PUBLIC`, `HEALTH`
- All 50 endpoint paths confirmed identical to canonical registry — no changes to endpoints

| Item | v2.0 | v2.1 |
|---|---|---|
| Internal call header | `X-Service-Token` / `X-Gateway-Token` | `Authorization: Bearer <JWT>` |
| Service / Gateway JWT expiry | 30 seconds | 5 minutes |
| Service JWT type claim | `purpose: internal` | `type: service` |
| Role name format | Finance Officer, FO / Manager | `FINANCE_OFFICER`, `APARTMENT_MANAGER` |
| API Type column values | Custom labels | `USER`, `INTERNAL`, `PUBLIC`, `HEALTH` |
| Endpoint paths (50 total) | Unchanged | Unchanged — all match canonical registry |

---

## SECTION 1 — SERVICE OVERVIEW

### 1.1 Group 3 Responsibilities

Group 3 owns the financial layer of the Apartment Management System. Two microservices handle all billing and utility operations.

| Service | Port (local) | Database | Responsibility |
|---|---|---|---|
| billing-payment-service | 8081 | billing_db | Charge rules, invoice generation, payment recording, receipts, adjustments, balance calculation, financial reports |
| utility-charge-service | 8082 | utility_db | Utility rate management, utility charge recording, usage calculation, utility summaries |

### 1.2 Repositories

| Repository | Type | Group 3 Role |
|---|---|---|
| billing-payment-service | Owned | Primary owner — all source code, schema, tests, Swagger, Dockerfile, CI |
| utility-charge-service | Owned | Primary owner — all source code, schema, tests, Swagger, Dockerfile, CI |
| shared-frontend | Shared — contribute | Feature modules for /charges, /invoices, /payments, /receipts, /utilities, /finance-dashboard |
| api-gateway | Shared — contribute | Register billing and utility service routes |
| deployment-infrastructure | Shared — contribute | Dockerfiles and Docker Compose entries for both services |
| project-docs | Shared — contribute | SRS, API contracts, ADRs, ERDs, architecture diagrams |

### 1.3 Cross-Team Dependencies

Group 3 consumes APIs from other groups and provides internal APIs for consumption by other groups. No group may directly access another group's database.

| Direction | Group | Service | What | Used In Group 3 |
|---|---|---|---|---|
| We CONSUME | Group 1 | identity-access-service | Gateway User JWT — validated on every protected endpoint via `Authorization: Bearer` | All 32 public USER endpoints |
| We CONSUME | Group 2 | lease-occupancy-service | LEASE-011 `GET /api/v1/internal/occupancies/active-billing` — confirm occupancy eligible for billing | `POST /api/v1/invoices` (BILL-007) before invoice generation |
| We CONSUME | Group 2 | property-unit-service | PROP-016 `GET /api/v1/internal/units/{unitId}/exists` — validate unit exists | `POST /api/v1/invoices` (BILL-007) before invoice generation |
| We PROVIDE | Group 4 | billing-payment-service | BILL-028 `GET /api/v1/internal/balance/{unitId}` — unit balance status | Group 4 community-service facility booking eligibility check |
| We PROVIDE | Group 4 | utility-charge-service | UTIL-014 `GET /api/v1/internal/utility-charges/units/{unitId}` — utility charges for period | Group 4 or billing service fetching utility data |

---

## SECTION 2 — SECURITY STANDARD

This section reflects the Project A JWT Authentication and Service-to-Service Security Standard agreed by all four teams. Group 3 implements this standard identically in both services.

### 2.1 Architecture

There are two request types in the system:

- **User request:** Frontend → API Gateway → Backend Service
- **Internal service request:** Service A → API Gateway → Service B

The API Gateway is the central trust point. Backend services never communicate directly with each other. Every call — including internal service-to-service calls — is routed through the Gateway.

### 2.2 Signing Algorithm

Algorithm: **RS256** (RSA asymmetric signing)

The private key signs the JWT. The corresponding public key verifies it.

This is used for all JWT types: User JWT, Service JWT, and Gateway JWT.

### 2.3 JWT Types

**User JWT** — issued by identity-access-service

Issued after successful login. Signed with Identity Access private key. Lifetime: 30 minutes.

```
Header: { "alg": "RS256", "typ": "JWT" }
Payload: {
  "sub": "user_123",
  "type": "user",
  "roles": ["TENANT"],
  "iat": 1750000000,
  "exp": 1750001800
}
```

**Service JWT** — issued by a backend service calling another service

Created by the calling service using its own private key. Lifetime: 5 minutes. Not forwarded to the target service.

```
Header: { "alg": "RS256", "typ": "JWT" }
Payload: {
  "sub": "billing-payment-service",
  "type": "service",
  "iat": 1750000000,
  "exp": 1750000300
}
```

**Gateway JWT** — issued by API Gateway after verifying an incoming JWT

Created by the Gateway after verifying either a User JWT or a Service JWT. Signed with the Gateway private key. Lifetime: 5 minutes. This is what backend services actually receive and verify.

```
// Gateway User JWT (forwarded to backend for user requests):
Payload: {
  "sub": "user_123",
  "type": "user",
  "roles": ["TENANT"],
  "iat": 1750000000,
  "exp": 1750000300
}

// Gateway Service JWT (forwarded to backend for internal requests):
Payload: {
  "sub": "billing-payment-service",
  "type": "service",
  "iat": 1750000000,
  "exp": 1750000300
}
```

### 2.4 Key Ownership

| Component | Holds | Shared With |
|---|---|---|
| identity-access-service | Identity Access Private Key + Public Key | Public key shared with API Gateway only |
| API Gateway | Gateway Private Key + Public Key | Public key shared with all backend services. Also stores public keys of all trusted services. |
| billing-payment-service | Service Private Key + Public Key | Public key shared with API Gateway only |
| utility-charge-service | Service Private Key + Public Key | Public key shared with API Gateway only |
| All other backend services | Their own Service Private Key + Public Key | Public key shared with API Gateway only |

**Note:** A backend service never holds another backend service's key. Service B does not need Service A's key — it only needs the Gateway's public key.

### 2.5 Token Lifetimes

| Token | Issuer | Lifetime | Environment Variable |
|---|---|---|---|
| User JWT | identity-access-service | 30 minutes | `JWT_ACCESS_TOKEN_EXPIRES_IN=30m` |
| Service JWT | Any backend service | 5 minutes | `SERVICE_JWT_EXPIRES_IN=5m` |
| Gateway JWT | API Gateway | 5 minutes | `GATEWAY_JWT_EXPIRES_IN=5m` |

### 2.6 User Request Workflow

How a frontend user request reaches billing-payment-service or utility-charge-service:

1. Frontend sends: `Authorization: Bearer <USER_JWT>` → API Gateway
2. API Gateway:
   a. Parse JWT structure
   b. Confirm algorithm = RS256
   c. Verify signature using Identity Access public key
   d. Check `type = user`
   e. Check expiration (`exp`)
   f. Validate required claims (`sub`, `roles`, `iat`, `exp`)
   g. Create NEW Gateway JWT signed with Gateway private key
3. Gateway sends: `Authorization: Bearer <GATEWAY_JWT>` → Backend Service
4. Backend Service (billing-payment-service / utility-charge-service):
   a. Parse JWT structure
   b. Confirm algorithm = RS256
   c. Verify signature using Gateway public key
   d. Check `type = user`
   e. Check expiration (`exp`)
   f. Validate required claims
   g. Apply role authorization (`@PreAuthorize`)
   h. Process request

### 2.7 Internal Service Request Workflow

How billing-payment-service calls utility-charge-service, or how Group 4 calls our internal balance endpoint:

1. Service A creates a Service JWT signed with its own private key:
   ```
   { "sub": "billing-payment-service", "type": "service", "iat": ..., "exp": ... }
   ```
2. Service A sends: `Authorization: Bearer <SERVICE_JWT>` → API Gateway
   (to the Gateway URL — never directly to Service B)
3. API Gateway:
   a. Parse JWT structure
   b. Confirm algorithm = RS256
   c. Identify claimed service from `sub` (for key selection only)
   d. Verify signature using Service A's registered public key
   e. Check `type = service`
   f. Check expiration (`exp`)
   g. Verify service is authorized to call the requested endpoint
   h. Create NEW Gateway Service JWT signed with Gateway private key
      ```
      { "sub": "billing-payment-service", "type": "service", "iat": ..., "exp": ... }
      ```
4. Gateway sends: `Authorization: Bearer <GATEWAY_JWT>` → Service B
   (original Service JWT is NOT forwarded)
5. Service B (e.g. utility-charge-service):
   a. Parse JWT structure
   b. Confirm algorithm = RS256
   c. Verify signature using Gateway public key
   d. Check `type = service`
   e. Check expiration (`exp`)
   f. Identify calling service from `sub`
   g. Check service-level authorization (is billing-payment-service allowed to call this?)
   h. Process request

### 2.8 Environment Variables

**billing-payment-service**

```
SERVICE_NAME=billing-payment-service
PORT=8081
JWT_ALGORITHM=RS256
GATEWAY_JWT_PUBLIC_KEY=<gateway-public-key>
SERVICE_JWT_PRIVATE_KEY=<billing-service-private-key>
SERVICE_JWT_EXPIRES_IN=5m
API_GATEWAY_URL=https://api-gateway.ams.com
DB_URL=jdbc:mysql://<host>:3306/billing_db
DB_USER=<db-user>
DB_PASSWORD=<db-password>
```

**utility-charge-service**

```
SERVICE_NAME=utility-charge-service
PORT=8082
JWT_ALGORITHM=RS256
GATEWAY_JWT_PUBLIC_KEY=<gateway-public-key>
SERVICE_JWT_PRIVATE_KEY=<utility-service-private-key>
SERVICE_JWT_EXPIRES_IN=5m
API_GATEWAY_URL=https://api-gateway.ams.com
DB_URL=jdbc:mysql://<host>:3306/utility_db
DB_USER=<db-user>
DB_PASSWORD=<db-password>
```

**Note:** Private keys must never be committed to Git. Use `.env` files locally and secret management in cloud. Only commit `.env.example` with placeholder values.

### 2.9 Authorization Header — All Requests

ALL requests (user and internal) use the same standard header format:

```
Authorization: Bearer <JWT>
```

There are no custom headers (`X-Service-Token`, `X-Gateway-Token`, `X-Internal-Secret`, etc.).

The JWT `type` claim (`type: user` or `type: service`) distinguishes user from internal requests.

### 2.10 HTTP Error Codes for Authentication and Authorization

| Code | When |
|---|---|
| 401 Unauthorized | Missing JWT, malformed JWT, invalid RS256 signature, expired JWT, unsupported algorithm, invalid required claims, unregistered service |
| 403 Forbidden | JWT is valid but user role is not allowed for this endpoint, or calling service is not authorized for this internal endpoint |

### 2.11 Spring Boot Implementation Notes

Both Group 3 services use JJWT (already in `pom.xml` for JWT parsing) with RS256. All public endpoints protected by Spring Security and `@PreAuthorize`. Internal endpoints additionally check `type = service` and authorize the calling service `sub`.

```xml
<!-- pom.xml dependency (already present for JWT validation): -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```

```java
// Verify Gateway JWT in a filter (both user and internal):
Jwts.parser()
    .verifyWith(gatewayRsaPublicKey) // loaded from GATEWAY_JWT_PUBLIC_KEY env var
    .build()
    .parseSignedClaims(token); // throws JwtException if invalid or expired

// Create Service JWT when making an internal call:
String serviceJwt = Jwts.builder()
    .subject("billing-payment-service")
    .claim("type", "service")
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + 5 * 60 * 1000)) // 5 minutes
    .signWith(serviceRsaPrivateKey, Jwts.SIG.RS256)
    .compact();

// Send internal call to Gateway (not directly to the other service):
headers.set("Authorization", "Bearer " + serviceJwt);
restTemplate.exchange(apiGatewayUrl + "/api/v1/internal/...", HttpMethod.GET, ...);
```

---

## SECTION 3 — RESPONSE FORMAT AND HTTP STATUS CODES

### 3.1 Standard Response Envelope

All responses from both services use this standard envelope format agreed across all four groups:

```json
// Success response:
{
  "success": true,
  "message": "Invoice retrieved successfully",
  "data": { ... },
  "timestamp": "2026-08-28T10:30:00Z",
  "requestId": "req-7f83a9b2"
}

// List response (includes pagination):
{
  "success": true,
  "message": "Invoices retrieved",
  "data": [ ... ],
  "pagination": { "page": 0, "size": 20, "totalElements": 42, "totalPages": 3, "hasNext": true },
  "timestamp": "2026-08-28T10:30:00Z",
  "requestId": "req-7f83a9b2"
}

// Error response:
{
  "success": false,
  "message": "Invoice not found",
  "error": { "code": "INVOICE_NOT_FOUND", "details": null },
  "timestamp": "2026-08-28T10:30:00Z",
  "requestId": "req-7f83a9b2"
}
```

### 3.2 HTTP Status Codes

| Code | Meaning | When Used in Group 3 Services |
|---|---|---|
| 200 | OK | Success — GET, PUT, PATCH completed successfully |
| 201 | Created | Resource created — POST — new charge rule, invoice, payment, utility charge |
| 204 | No Content | Deleted — no body — DELETE utility charge |
| 400 | Bad Request | Invalid input — Missing required fields, wrong data types, invalid format |
| 401 | Unauthorized | Authentication failed — Missing JWT, invalid RS256 signature, expired JWT, unregistered service |
| 403 | Forbidden | Authorization failed — Valid JWT but role not permitted, or service not authorized for internal endpoint |
| 404 | Not Found | Resource not found — Invoice ID, payment ID, charge rule ID, unit ID does not exist |
| 409 | Conflict | Duplicate record — Invoice already exists for same unit + billing period |
| 422 | Unprocessable Entity | Business rule violation — Overpayment attempt, cancelling a paid invoice, invalid status transition |
| 500 | Internal Server Error | Unexpected error — Uncaught exception — log and return safe generic message |
| 503 | Service Unavailable | Dependency unavailable — Group 2 lease or property API unreachable during invoice generation |

### 3.3 Business Rules Enforced in Service Layer

- **Invoice snapshot rule:** When an invoice is generated, charge amounts are permanently copied into `InvoiceLine` records at that moment. Changing a charge rule after invoice generation must never alter any existing invoice.
- **No overpayment:** A payment amount cannot exceed the outstanding invoice balance. Returns 422 if attempted.
- **No duplicate invoice:** Two invoices cannot exist for the same `unitId` + `billingYear` + `billingMonth` combination unless the first is `CANCELLED`. Returns 409 if attempted.
- **Balance reconciliation:** Outstanding balance is always computed live: total `ISSUED`/`PARTIALLY_PAID` invoice amounts minus total `CONFIRMED` payment amounts, adjusted by credits and debits.
- **Unit validation:** billing-payment-service must confirm with Group 2 (PROP-016 and LEASE-011) that the unit exists and has an active occupancy before generating an invoice. If Group 2 is unavailable, returns 503.
- **Utility charge protection:** A utility charge record can only be updated or deleted if no invoice has been generated for that unit and period. Returns 409 if invoice already exists.

---

## SECTION 4 — EXTERNAL APIs CONSUMED BY GROUP 3

These endpoints belong to other groups. Group 3 services call them during internal workflows. Group 3 does not own, implement, or document these endpoints — they are listed here for reference only.

### 4.1 From property-unit-service (Group 2) — PROP-016

Called by billing-payment-service before generating an invoice to confirm the unit exists in the system.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| PROP-016 | GET | `/api/v1/internal/units/{unitId}/exists` | INTERNAL | Gateway Service JWT | Authorized services | Validate unit existence |

billing-payment-service sends `Authorization: Bearer <SERVICE_JWT>` signed with its own private key. The Gateway verifies and forwards. Group 2's service checks `type = service` and authorizes billing-payment-service. Returns whether the unit exists.

### 4.2 From lease-occupancy-service (Group 2) — LEASE-011

Called by billing-payment-service before generating an invoice to confirm the unit has an active occupancy eligible for billing.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| LEASE-011 | GET | `/api/v1/internal/occupancies/active-billing` | INTERNAL | Gateway Service JWT | Authorized services | Return occupancy eligible for billing |

Both PROP-016 and LEASE-011 must succeed before `POST /api/v1/invoices` (BILL-007) creates any records. If either returns a non-200 response or is unreachable, invoice generation fails with 503 Service Unavailable.

---

## SECTION 5 — billing-payment-service API ENDPOINTS

Base URL (local): `http://localhost:8081/api/v1`
Base URL (cloud): `https://{cloud-host}/api/v1` (env var: `API_GATEWAY_URL`)
Database: `billing_db` — dedicated MySQL schema, never accessed by other services directly.

### 5.1 Charge Rules

Charge rules define recurring charge types applied to units: management fee, parking fee, facility fee, etc. Only `FINANCE_OFFICER` manages charge rules. Changing a rule never affects already-issued invoices.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| BILL-001 | POST | `/api/v1/charge-rules` | USER | Gateway User JWT | FINANCE_OFFICER | Create charge rule |
| BILL-002 | GET | `/api/v1/charge-rules` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List/filter charge rules |
| BILL-003 | GET | `/api/v1/charge-rules/{chargeRuleId}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Get charge rule |
| BILL-004 | PUT | `/api/v1/charge-rules/{chargeRuleId}` | USER | Gateway User JWT | FINANCE_OFFICER | Update charge rule |
| BILL-005 | PATCH | `/api/v1/charge-rules/{chargeRuleId}/status` | USER | Gateway User JWT | FINANCE_OFFICER | Change charge rule status |
| BILL-006 | GET | `/api/v1/charge-rules/type/{chargeType}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List charge rules by type |

**Note:** BILL-005 uses `PATCH /{id}/status` pattern consistent with the shared AMS API standard. Request body: `{ status: ACTIVE | INACTIVE }`. Deactivated rules are retained for audit — not deleted.

### 5.2 Invoices

**CRITICAL RULE — Invoice Snapshot:**
`POST /api/v1/invoices` copies charge rule name, type, and amount into `InvoiceLine` at generation time. These values are permanent. Updating a charge rule must never change any existing invoice line item.

Before creating an invoice: validates unit (PROP-016) and occupancy (LEASE-011) via Group 2.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| BILL-007 | POST | `/api/v1/invoices` | USER | Gateway User JWT | FINANCE_OFFICER | Generate invoice |
| BILL-008 | GET | `/api/v1/invoices` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List/filter invoices |
| BILL-009 | GET | `/api/v1/invoices/{invoiceId}` | USER | Gateway User JWT | Authenticated roles | Get invoice |
| BILL-010 | GET | `/api/v1/invoices/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get unit invoices |
| BILL-011 | GET | `/api/v1/invoices/units/{unitId}/period/{year}/{month}` | USER | Gateway User JWT | Authenticated roles | Get invoice for unit and period |
| BILL-012 | PATCH | `/api/v1/invoices/{invoiceId}/status` | USER | Gateway User JWT | FINANCE_OFFICER | Change invoice status |
| BILL-013 | GET | `/api/v1/invoices/{invoiceId}/lines` | USER | Gateway User JWT | Authenticated roles | Get invoice lines |

**Note:** RESIDENT, TENANT, and OWNER accessing BILL-009, BILL-010, BILL-011, BILL-013 are restricted to invoices belonging to their own `unitId`. The service enforces this — accessing another unit's invoice returns 403.

### 5.3 Payments

Payments are simulated — no real payment gateway. FINANCE_OFFICER records payment details. The service enforces the no-overpayment rule (422 if payment exceeds outstanding balance).

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| BILL-014 | POST | `/api/v1/payments` | USER | Gateway User JWT | FINANCE_OFFICER | Record simulated payment |
| BILL-015 | GET | `/api/v1/payments` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List/filter payments |
| BILL-016 | GET | `/api/v1/payments/{paymentId}` | USER | Gateway User JWT | Authenticated roles | Get payment |
| BILL-017 | GET | `/api/v1/payments/invoices/{invoiceId}` | USER | Gateway User JWT | Authenticated roles | Get payments for invoice |
| BILL-018 | GET | `/api/v1/payments/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get payments for unit |
| BILL-019 | GET | `/api/v1/payments/residents/{residentId}` | USER | Gateway User JWT | Authenticated roles | Get payments for resident |
| BILL-020 | PATCH | `/api/v1/payments/{paymentId}/status` | USER | Gateway User JWT | FINANCE_OFFICER | Confirm/reject payment |

**Note:** BILL-020 request body: `{ status: CONFIRMED | REJECTED, reason: string (required for REJECTED) }`. On CONFIRMED: invoice balance recalculated and receipt generated automatically.

### 5.4 Receipts

Receipts are generated automatically when a payment is confirmed. They are immutable — no create, update, or delete endpoints. A receipt records: unit, billing period, amount paid, payment method, reference number, and issue timestamp.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| BILL-021 | GET | `/api/v1/receipts/{receiptId}` | USER | Gateway User JWT | Authenticated roles | Get receipt |
| BILL-022 | GET | `/api/v1/receipts/payments/{paymentId}` | USER | Gateway User JWT | Authenticated roles | Get receipt for payment |
| BILL-023 | GET | `/api/v1/receipts/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get receipts for unit |

**Note:** Returns 404 if the payment was rejected or is still pending. RESIDENT, TENANT, OWNER restricted to own `unitId`.

### 5.5 Adjustments

Adjustments allow FINANCE_OFFICER to apply a credit (reduces balance) or debit (increases balance) to an invoice when needed. Every adjustment requires a documented reason of at least 10 characters.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| BILL-024 | POST | `/api/v1/adjustments` | USER | Gateway User JWT | FINANCE_OFFICER | Create credit/debit adjustment |
| BILL-025 | GET | `/api/v1/adjustments/invoices/{invoiceId}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List invoice adjustments |

### 5.6 Balance

Balance is computed live from stored invoice and payment records. Never pre-computed and stored. Formula: total `ISSUED`/`PARTIALLY_PAID` invoice amounts minus total `CONFIRMED` payment amounts, adjusted by `CREDIT` and `DEBIT` records.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| BILL-026 | GET | `/api/v1/balance/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get unit outstanding balance |
| BILL-027 | GET | `/api/v1/balance/residents/{residentId}` | USER | Gateway User JWT | Authenticated roles | Get resident balance |

**Note:** RESIDENT, TENANT, OWNER restricted to own `unitId` / `residentId`. Used on the resident personal dashboard. Response includes: `outstandingBalance`, `overdueCount`, `lastInvoiceDate`, `lastPaymentDate`.

### 5.7 Reports and Finance Dashboard

Report endpoints power the `/finance-dashboard` frontend route. Restricted to FINANCE_OFFICER and APARTMENT_MANAGER only. RESIDENT, TENANT, and OWNER do not have access to system-wide financial aggregates.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| BILL-029 | GET | `/api/v1/reports/finance-dashboard` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Financial dashboard |
| BILL-030 | GET | `/api/v1/reports/arrears` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Arrears report |
| BILL-031 | GET | `/api/v1/reports/collection-summary` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Collection summary |
| BILL-032 | GET | `/api/v1/reports/payment-history` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Payment history report |

**Note:** BILL-029 returns: `totalInvoicedThisMonth`, `totalCollectedThisMonth`, `totalOutstanding`, `overdueAccountsCount`, `collectionRatePercent`. BILL-030 supports filter by `buildingId`, `year`, `month`.

### 5.8 Internal Endpoint — Provided to Group 4

This endpoint is registered in the API Gateway under internal routing.

Caller must send `Authorization: Bearer <SERVICE_JWT>` signed with its own RSA private key.
Gateway verifies the Service JWT, creates a new Gateway Service JWT, and forwards it.
This service verifies `Authorization: Bearer <GATEWAY_JWT>` using the Gateway public key.
`type = service` is required. The calling service `sub` must be authorized for this endpoint.
Frontend users cannot call this endpoint — they have no service private key.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| BILL-028 | GET | `/api/v1/internal/balance/{unitId}` | INTERNAL | Gateway Service JWT | Authorized services | Provide unit balance to internal consumers |

**Note:** Called by Group 4 community-service to check outstanding dues before approving a facility booking. Response: `{ unitId, outstandingBalance, hasOverdueInvoices, lastPaymentDate }`.

### 5.9 Health and Info

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| BILL-033 | GET | `/actuator/health` | HEALTH | None | None | Service health |
| BILL-034 | GET | `/actuator/info` | HEALTH | None | None | Service metadata |

---

## SECTION 6 — utility-charge-service API ENDPOINTS

Base URL (local): `http://localhost:8082/api/v1`
Base URL (cloud): `https://{cloud-host}/api/v1` (env var: `API_GATEWAY_URL`)
Database: `utility_db` — dedicated MySQL schema, completely separate from `billing_db`.
Purpose: Records utility usage per unit per billing period and provides this data to billing-payment-service during invoice generation.

### 6.1 Utility Rates

Rates define the price per unit of consumption for each utility type (e.g. water: 50 per cubic metre). A rate must exist and be `ACTIVE` before utility charges can be calculated. Updating a rate does not affect already-recorded charges.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| UTIL-001 | POST | `/api/v1/utility-rates` | USER | Gateway User JWT | FINANCE_OFFICER | Create utility rate |
| UTIL-002 | GET | `/api/v1/utility-rates` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List/filter utility rates |
| UTIL-003 | GET | `/api/v1/utility-rates/{utilityRateId}` | USER | Gateway User JWT | FINANCE_OFFICER | Get utility rate |
| UTIL-004 | PUT | `/api/v1/utility-rates/{utilityRateId}` | USER | Gateway User JWT | FINANCE_OFFICER | Update utility rate |
| UTIL-005 | PATCH | `/api/v1/utility-rates/{utilityRateId}/status` | USER | Gateway User JWT | FINANCE_OFFICER | Change utility rate status |

**Note:** UTIL-005 request body: `{ status: ACTIVE | INACTIVE }`. Deactivated rates are retained for audit history.

### 6.2 Utility Charges

Records actual consumption for a unit in a billing period. Calculated amount = `usageValue x ratePerUnit` (using the active rate at the time of recording). These records are pulled into billing-payment-service during invoice generation as line items.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| UTIL-006 | POST | `/api/v1/utility-charges` | USER | Gateway User JWT | FINANCE_OFFICER | Create utility charge |
| UTIL-007 | GET | `/api/v1/utility-charges` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List/filter utility charges |
| UTIL-008 | GET | `/api/v1/utility-charges/{utilityChargeId}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Get utility charge |
| UTIL-009 | PUT | `/api/v1/utility-charges/{utilityChargeId}` | USER | Gateway User JWT | FINANCE_OFFICER | Update utility charge |
| UTIL-010 | DELETE | `/api/v1/utility-charges/{utilityChargeId}` | USER | Gateway User JWT | FINANCE_OFFICER | Delete utility charge before invoice generation |
| UTIL-011 | GET | `/api/v1/utility-charges/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get utility charges for unit |
| UTIL-012 | GET | `/api/v1/utility-charges/units/{unitId}/period/{year}/{month}` | USER | Gateway User JWT | Authenticated roles | Get utility charge for unit and period |
| UTIL-013 | GET | `/api/v1/utility-charges/summary` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Get utility charge summary |

**Note:** UTIL-009 and UTIL-010 return 409 Conflict if an invoice has already been generated for this unit and period. UTIL-011 and UTIL-012: RESIDENT, TENANT, OWNER restricted to own `unitId`.

### 6.3 Internal Endpoint — Provided to billing-payment-service

This endpoint is registered in the API Gateway under internal routing.

Caller (billing-payment-service) sends `Authorization: Bearer <SERVICE_JWT>`.
Gateway verifies, creates Gateway Service JWT, and forwards.
This service verifies `Authorization: Bearer <GATEWAY_JWT>` using Gateway public key.
Returns empty array `[]` (not 404) if no utility charges exist for the requested period.
billing-payment-service must handle an empty array gracefully during invoice generation.

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| UTIL-014 | GET | `/api/v1/internal/utility-charges/units/{unitId}` | INTERNAL | Gateway Service JWT | Authorized services | Provide period utility charges to billing |

**Note:** Query params: `?year={year}&month={month}`. Called automatically during BILL-007 invoice generation. Each returned record becomes an invoice line item in billing-payment-service.

### 6.4 Health and Info

| API ID | Method | Endpoint | API Type | Auth Required | Role | Purpose |
|---|---|---|---|---|---|---|
| UTIL-015 | GET | `/actuator/health` | HEALTH | None | None | Service health |
| UTIL-016 | GET | `/actuator/info` | HEALTH | None | None | Service metadata |

---

## SECTION 7 — COMPLETE ENDPOINT SUMMARY

### 7.1 billing-payment-service — All 34 Endpoints

| API ID | Method | Endpoint | API Type | Auth Required | Role |
|---|---|---|---|---|---|
| BILL-001 | POST | `/api/v1/charge-rules` | USER | Gateway User JWT | FINANCE_OFFICER |
| BILL-002 | GET | `/api/v1/charge-rules` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| BILL-003 | GET | `/api/v1/charge-rules/{chargeRuleId}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| BILL-004 | PUT | `/api/v1/charge-rules/{chargeRuleId}` | USER | Gateway User JWT | FINANCE_OFFICER |
| BILL-005 | PATCH | `/api/v1/charge-rules/{chargeRuleId}/status` | USER | Gateway User JWT | FINANCE_OFFICER |
| BILL-006 | GET | `/api/v1/charge-rules/type/{chargeType}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| BILL-007 | POST | `/api/v1/invoices` | USER | Gateway User JWT | FINANCE_OFFICER |
| BILL-008 | GET | `/api/v1/invoices` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| BILL-009 | GET | `/api/v1/invoices/{invoiceId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-010 | GET | `/api/v1/invoices/units/{unitId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-011 | GET | `/api/v1/invoices/units/{unitId}/period/{year}/{month}` | USER | Gateway User JWT | Authenticated roles |
| BILL-012 | PATCH | `/api/v1/invoices/{invoiceId}/status` | USER | Gateway User JWT | FINANCE_OFFICER |
| BILL-013 | GET | `/api/v1/invoices/{invoiceId}/lines` | USER | Gateway User JWT | Authenticated roles |
| BILL-014 | POST | `/api/v1/payments` | USER | Gateway User JWT | FINANCE_OFFICER |
| BILL-015 | GET | `/api/v1/payments` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| BILL-016 | GET | `/api/v1/payments/{paymentId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-017 | GET | `/api/v1/payments/invoices/{invoiceId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-018 | GET | `/api/v1/payments/units/{unitId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-019 | GET | `/api/v1/payments/residents/{residentId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-020 | PATCH | `/api/v1/payments/{paymentId}/status` | USER | Gateway User JWT | FINANCE_OFFICER |
| BILL-021 | GET | `/api/v1/receipts/{receiptId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-022 | GET | `/api/v1/receipts/payments/{paymentId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-023 | GET | `/api/v1/receipts/units/{unitId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-024 | POST | `/api/v1/adjustments` | USER | Gateway User JWT | FINANCE_OFFICER |
| BILL-025 | GET | `/api/v1/adjustments/invoices/{invoiceId}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| BILL-026 | GET | `/api/v1/balance/units/{unitId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-027 | GET | `/api/v1/balance/residents/{residentId}` | USER | Gateway User JWT | Authenticated roles |
| BILL-028 | GET | `/api/v1/internal/balance/{unitId}` | INTERNAL | Gateway Service JWT | Authorized services |
| BILL-029 | GET | `/api/v1/reports/finance-dashboard` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| BILL-030 | GET | `/api/v1/reports/arrears` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| BILL-031 | GET | `/api/v1/reports/collection-summary` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| BILL-032 | GET | `/api/v1/reports/payment-history` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| BILL-033 | GET | `/actuator/health` | HEALTH | None | None |
| BILL-034 | GET | `/actuator/info` | HEALTH | None | None |

### 7.2 utility-charge-service — All 16 Endpoints

| API ID | Method | Endpoint | API Type | Auth Required | Role |
|---|---|---|---|---|---|
| UTIL-001 | POST | `/api/v1/utility-rates` | USER | Gateway User JWT | FINANCE_OFFICER |
| UTIL-002 | GET | `/api/v1/utility-rates` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| UTIL-003 | GET | `/api/v1/utility-rates/{utilityRateId}` | USER | Gateway User JWT | FINANCE_OFFICER |
| UTIL-004 | PUT | `/api/v1/utility-rates/{utilityRateId}` | USER | Gateway User JWT | FINANCE_OFFICER |
| UTIL-005 | PATCH | `/api/v1/utility-rates/{utilityRateId}/status` | USER | Gateway User JWT | FINANCE_OFFICER |
| UTIL-006 | POST | `/api/v1/utility-charges` | USER | Gateway User JWT | FINANCE_OFFICER |
| UTIL-007 | GET | `/api/v1/utility-charges` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| UTIL-008 | GET | `/api/v1/utility-charges/{utilityChargeId}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| UTIL-009 | PUT | `/api/v1/utility-charges/{utilityChargeId}` | USER | Gateway User JWT | FINANCE_OFFICER |
| UTIL-010 | DELETE | `/api/v1/utility-charges/{utilityChargeId}` | USER | Gateway User JWT | FINANCE_OFFICER |
| UTIL-011 | GET | `/api/v1/utility-charges/units/{unitId}` | USER | Gateway User JWT | Authenticated roles |
| UTIL-012 | GET | `/api/v1/utility-charges/units/{unitId}/period/{year}/{month}` | USER | Gateway User JWT | Authenticated roles |
| UTIL-013 | GET | `/api/v1/utility-charges/summary` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER |
| UTIL-014 | GET | `/api/v1/internal/utility-charges/units/{unitId}` | INTERNAL | Gateway Service JWT | Authorized services |
| UTIL-015 | GET | `/actuator/health` | HEALTH | None | None |
| UTIL-016 | GET | `/actuator/info` | HEALTH | None | None |

### 7.3 Count

| Service | USER | INTERNAL | HEALTH | Total |
|---|---|---|---|---|
| billing-payment-service | 32 | 1 | 2 | 35 |
| utility-charge-service | 13 | 1 | 2 | 16 |
| **Grand Total** | **45** | **2** | **4** | **51** |

### 7.4 Version History

| Version | Change |
|---|---|
| v1.0 | Initial API reference. Internal endpoints used Docker network isolation + `X-Internal-Secret` header. |
| v2.0 | Security upgraded to RSA public/private key signing via API Gateway. All endpoint paths unchanged. |
| v2.1 | Fixed header names (`Authorization: Bearer` — no custom headers). Fixed token expiry (5 min). Fixed JWT type claim (`type: service`). Role names aligned to canonical registry (`FINANCE_OFFICER`, `APARTMENT_MANAGER`). API Type column aligned (`USER`, `INTERNAL`, `PUBLIC`, `HEALTH`). All 50 endpoint paths confirmed identical to canonical registry. |
