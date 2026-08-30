# billing-payment-service

AMS Group 3 — Billing, Utilities, and Payments  
University of Kelaniya | Software Architecture and Process Models

![CI](https://github.com/<org-name>/billing-payment-service/actions/workflows/ci.yml/badge.svg)

## Overview
Handles charge rule management, invoice generation (with immutable line item snapshots),
payment recording, receipt generation, balance calculation, and financial reporting.

- **Port (local):** 8081
- **Database:** `billing_db` (MySQL 8)
- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **Health check:** http://localhost:8081/actuator/health

## Prerequisites
- Java 21
- Maven 3.9+
- MySQL 8 running locally (or via Docker)

## Setup

```bash
# 1. Copy environment variables
cp .env.example .env
# Edit .env with your database credentials and JWT secret

# 2. Create the database schema
mysql -u root -p < src/main/resources/db/schema.sql

# 3. Run the service
./mvnw spring-boot:run
```

## Running Tests
```bash
./mvnw test
```

## Branch Naming
`feature/AMS-G3-{issue-number}-{short-description}`  
Example: `feature/AMS-G3-12-charge-rule-crud`

## API Contract
Full API specification: `project-docs/api-contracts/billing-payment-service.md`