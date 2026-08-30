-- ============================================================
-- AMS Group 3 — billing_db schema
-- billing-payment-service
-- Run once manually on a clean database.
-- DO NOT use spring.sql.init.mode=always — use this file via
-- the DevOps deployment guide or DataGrip.
-- ============================================================

CREATE DATABASE IF NOT EXISTS billing_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE billing_db;

-- ── Enums are handled at application layer (VARCHAR columns) ──

CREATE TABLE IF NOT EXISTS charge_rules (
                                            id              VARCHAR(36)       NOT NULL,
    name            VARCHAR(100)   NOT NULL,
    charge_type     VARCHAR(50)    NOT NULL,   -- MANAGEMENT_FEE | PARKING_FEE | FACILITY_FEE | UTILITY
    amount          DECIMAL(12,2)  NOT NULL,
    billing_period  VARCHAR(20)    NOT NULL,   -- MONTHLY | QUARTERLY
    applicable_to_all_units BOOLEAN NOT NULL DEFAULT TRUE,
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE
    created_by      VARCHAR(36)       NOT NULL,
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_charge_rules PRIMARY KEY (id),
    CONSTRAINT chk_charge_rules_amount CHECK (amount > 0)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS invoices (
                                        id              VARCHAR(36)       NOT NULL,
    unit_id         VARCHAR(36)       NOT NULL,
    resident_id     VARCHAR(36)       NOT NULL,
    billing_period  VARCHAR(20)    NOT NULL,   -- MONTHLY | QUARTERLY
    billing_year    SMALLINT       NOT NULL,
    billing_month   TINYINT        NULL,       -- NULL for QUARTERLY invoices
    total_amount    DECIMAL(12,2)  NOT NULL,
    status          VARCHAR(30)    NOT NULL DEFAULT 'ISSUED',
    -- ISSUED | PARTIALLY_PAID | PAID | OVERDUE | CANCELLED
    cancellation_reason VARCHAR(500) NULL,
    issued_by       VARCHAR(36)       NOT NULL,
    issued_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_invoices PRIMARY KEY (id),
    CONSTRAINT uq_invoices_unit_period
    UNIQUE (unit_id, billing_year, billing_month, billing_period, status)
    -- enforces duplicate prevention; status excluded by partial index logic in app
    ) ENGINE=InnoDB;

CREATE INDEX idx_invoices_unit_id    ON invoices (unit_id);
CREATE INDEX idx_invoices_resident   ON invoices (resident_id);
CREATE INDEX idx_invoices_status     ON invoices (status);
CREATE INDEX idx_invoices_period     ON invoices (billing_year, billing_month);

CREATE TABLE IF NOT EXISTS invoice_lines (
                                             id                  VARCHAR(36)       NOT NULL,
    invoice_id          VARCHAR(36)       NOT NULL,
    charge_rule_id      VARCHAR(36)       NOT NULL,    -- reference only — NOT a FK (snapshot pattern)
    charge_rule_name    VARCHAR(100)   NOT NULL,    -- snapshot
    charge_type         VARCHAR(50)    NOT NULL,    -- snapshot
    amount              DECIMAL(12,2)  NOT NULL,    -- snapshot — NEVER updated after creation
    created_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_invoice_lines PRIMARY KEY (id),
    CONSTRAINT fk_invoice_lines_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
                                        id               VARCHAR(36)       NOT NULL,
    invoice_id       VARCHAR(36)       NOT NULL,
    amount           DECIMAL(12,2)  NOT NULL,
    payment_date     DATE           NOT NULL,
    payment_method   VARCHAR(30)    NOT NULL,   -- BANK_TRANSFER | CASH | CHEQUE | ONLINE
    reference_number VARCHAR(100)   NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',  -- PENDING | CONFIRMED | REJECTED
    rejection_reason VARCHAR(500)   NULL,
    recorded_by      VARCHAR(36)       NOT NULL,
    recorded_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at        DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
    ) ENGINE=InnoDB;

CREATE INDEX idx_payments_invoice_id ON payments (invoice_id);
CREATE INDEX idx_payments_status     ON payments (status);

CREATE TABLE IF NOT EXISTS receipts (
                                        id               VARCHAR(36)       NOT NULL,
    payment_id       VARCHAR(36)       NOT NULL,
    unit_id          VARCHAR(36)       NOT NULL,
    billing_period   VARCHAR(20)    NOT NULL,
    billing_year     SMALLINT       NOT NULL,
    billing_month    TINYINT        NULL,
    amount_paid      DECIMAL(12,2)  NOT NULL,
    payment_date     DATE           NOT NULL,
    payment_method   VARCHAR(30)    NOT NULL,
    reference_number VARCHAR(100)   NOT NULL,
    issued_at        DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_receipts PRIMARY KEY (id),
    CONSTRAINT uq_receipts_payment UNIQUE (payment_id),
    CONSTRAINT fk_receipts_payment
    FOREIGN KEY (payment_id) REFERENCES payments(id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS adjustments (
                                           id               VARCHAR(36)       NOT NULL,
    invoice_id       VARCHAR(36)       NOT NULL,
    adjustment_type  VARCHAR(10)    NOT NULL,   -- CREDIT | DEBIT
    amount           DECIMAL(12,2)  NOT NULL,
    reason           VARCHAR(1000)  NOT NULL,
    created_by       VARCHAR(36)       NOT NULL,
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_adjustments PRIMARY KEY (id),
    CONSTRAINT fk_adjustments_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT chk_adjustments_amount CHECK (amount > 0),
    CONSTRAINT chk_adjustments_reason CHECK (CHAR_LENGTH(reason) >= 10)
    ) ENGINE=InnoDB;