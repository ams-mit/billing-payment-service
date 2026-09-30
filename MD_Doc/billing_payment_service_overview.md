# Billing & Payment Service Documentation

This document provides a detailed technical and business overview of the `billing-payment-service`. It is designed for Business Analysts to use in the creation of the Software Requirements Specification (SRS) report.

## 1. Business Process Overview

The `billing-payment-service` acts as the financial engine of the Apartment Management System. It manages the transition from predefined cost rules to finalized financial records.

### 1.1 Core Financial Lifecycle
1.  **Charge Rule Definition**: Finance Officers define `Charge Rules` (e.g., Management Fee, Parking Fee). These rules define the recurring cost name, amount, and `Billing Period` (MONTHLY, QUARTERLY, YEARLY).
2.  **Utility Recording**: Variable utility consumption is recorded in the `utility-charge-service` and fetched during invoice generation.
3.  **Invoice Generation**: On a monthly or periodic basis, the system generates an `Invoice` for a specific unit and resident. This process takes a **snapshot** of all active charge rules and current utility charges.
4.  **Payment Collection**: Payments are recorded against invoices. They are initially `PENDING` and must be reviewed by a Finance Officer.
5.  **Verification & Receipting**: Once a payment is `CONFIRMED`, the system automatically generates an immutable `Receipt`.
6.  **Balance Management**: The system computes the outstanding balance for units in real-time, accounting for invoices, confirmed payments, and adjustments.
7.  **Financial Reporting**: System-wide aggregates are generated for management via the Finance Dashboard.

---

## 2. Logic Process (Internal System Mechanism)

### 2.1 Invoice Generation Logic (`InvoiceServiceImpl`)
The system enforces a strict **Snapshot Rule** to maintain financial integrity:
- **Validation**: Before generation, the system calls Group 2 APIs to ensure the unit exists and the occupancy is active.
- **Duplicate Check**: Prevents creating multiple active invoices for the same unit, year, month, and period.
- **The Snapshot Mechanism**: 
    - The system retrieves all `ACTIVE` charge rules.
    - It fetches variable utility charges for the period.
    - **Key Logic**: It copies the *current* name and amount of each charge into `InvoiceLine` entities. 
    - *Impact*: If a charge rule is updated tomorrow, today's issued invoices remain unchanged.
- **Totaling**: The sum of all snapshotted lines is calculated and stored as the invoice `totalAmount`.

### 2.2 Payment Lifecycle & Status Transition (`PaymentServiceImpl`)
- **Overpayment Prevention**: 
    - $\text{Current Outstanding} = \text{Invoice Total} - \sum(\text{Confirmed Payments})$
    - If $\text{Payment Amount} > \text{Current Outstanding}$, the system throws an `OverpaymentException` (HTTP 422).
- **Payment Status Flow**:
    - `PENDING` $\rightarrow$ `CONFIRMED`: Triggers `ReceiptService.generateReceipt()` and calls `recalculateInvoiceStatus()`.
    - `PENDING` $\rightarrow$ `REJECTED`: Requires a mandatory rejection reason.
- **Invoice Status Recalculation**:
    - If $\sum(\text{Confirmed Payments}) \ge \text{Invoice Total} \rightarrow$ Invoice Status = `PAID`.
    - If $\sum(\text{Confirmed Payments}) < \text{Invoice Total} \rightarrow$ Invoice Status = `PARTIALLY_PAID`.

### 2.3 Live Balance Calculation (`BalanceServiceImpl`)
Balance is never stored as a static value; it is computed on-demand to ensure 100% accuracy:
$$\text{Outstanding Balance} = \sum(\text{Unpaid Invoices}) - \sum(\text{Confirmed Payments}) - \sum(\text{Credits}) + \sum(\text{Debits})$$
*Where Unpaid Invoices include statuses: `ISSUED`, `PARTIALLY_PAID`, and `OVERDUE`.*

### 2.4 Receipt Immutability (`ReceiptServiceImpl`)
Receipts are designed as permanent legal documents:
- **Auto-generation**: Triggered only upon payment confirmation.
- **Immutability**: All fields in the `Receipt` entity are marked as `updatable=false`. There are no API endpoints to edit or delete a receipt.

---

## 3. Database Design & Data Model

The `billing-payment-service` uses a dedicated MySQL database (`billing_db`).

### 3.1 Entity Relationship Diagram (Logical)
The system follows a hierarchical financial structure:
- **ChargeRule** $\rightarrow$ (Template for) $\rightarrow$ **InvoiceLine**
- **Invoice** $\rightarrow$ (Owns) $\rightarrow$ **InvoiceLine**
- **Invoice** $\rightarrow$ (Has many) $\rightarrow$ **Payment**
- **Invoice** $\rightarrow$ (Has many) $\rightarrow$ **Adjustment**
- **Payment** $\rightarrow$ (Generates one) $\rightarrow$ **Receipt**

### 3.2 Data Dictionary

#### Table: `charge_rules`
| Column | Type | Constraint | Description |
|:---|:---|:---|:---|
| `id` | VARCHAR(36) | PK | Unique identifier for the rule |
| `name` | VARCHAR(100) | NOT NULL | Name of the fee (e.g., "Water Maintenance") |
| `charge_type` | VARCHAR(50) | NOT NULL | Type of charge (e.g., FIXED, VARIABLE) |
| `amount` | DECIMAL(12,2) | NOT NULL | The standard cost amount |
| `billing_period` | VARCHAR(20) | NOT NULL | MONTHLY, QUARTERLY, YEARLY |
| `status` | VARCHAR(20) | NOT NULL | ACTIVE or INACTIVE |

#### Table: `invoices`
| Column | Type | Constraint | Description |
|:---|:---|:---|:---|
| `id` | VARCHAR(36) | PK | Unique identifier for the invoice |
| `unit_id` | VARCHAR(36) | NOT NULL | Reference to the property unit |
| `resident_id` | VARCHAR(36) | NOT NULL | Reference to the resident being billed |
| `total_amount` | DECIMAL(12,2) | NOT NULL | Total sum of all invoice lines |
| `status` | VARCHAR(30) | NOT NULL | ISSUED, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED |
| `billing_year` | SMALLINT | NOT NULL | The year of billing |
| `billing_month` | TINYINT | NULLABLE | The month of billing (if applicable) |

#### Table: `invoice_lines` (The Snapshot Table)
| Column | Type | Constraint | Description |
|:---|:---|:---|:---|
| `id` | VARCHAR(36) | PK | Unique identifier for the line |
| `invoice_id` | VARCHAR(36) | FK $\rightarrow$ Invoice | The parent invoice |
| `charge_rule_id` | VARCHAR(36) | NOT NULL | ID of the rule used (No FK to allow rule deletion) |
| `charge_rule_name`| VARCHAR(100) | NOT NULL | **Snapshot**: Name at time of issuance |
| `amount` | DECIMAL(12,2) | NOT NULL | **Snapshot**: Amount at time of issuance |

#### Table: `payments`
| Column | Type | Constraint | Description |
|:---|:---|:---|:---|
| `id` | VARCHAR(36) | PK | Unique identifier for the payment |
| `invoice_id` | VARCHAR(36) | FK $\rightarrow$ Invoice | The invoice being paid |
| `amount` | DECIMAL(12,2) | NOT NULL | Amount paid |
| `status` | VARCHAR(20) | NOT NULL | PENDING, CONFIRMED, REJECTED |
| `reference_number`| VARCHAR(100) | NOT NULL | Transaction reference (e.g., bank ref) |

#### Table: `receipts`
| Column | Type | Constraint | Description |
|:---|:---|:---|:---|
| `id` | VARCHAR(36) | PK | Unique identifier for the receipt |
| `payment_id` | VARCHAR(36) | FK $\rightarrow$ Payment | The confirmed payment that triggered this |
| `amount_paid` | DECIMAL(12,2) | NOT NULL | Final amount confirmed |
| `payment_date` | DATE | NOT NULL | Date of payment |

#### Table: `adjustments`
| Column | Type | Constraint | Description |
|:---|:---|:---|:---|
| `id` | VARCHAR(36) | PK | Unique identifier for the adjustment |
| `invoice_id` | VARCHAR(36) | FK $\rightarrow$ Invoice | The invoice being adjusted |
| `adjustment_type` | VARCHAR(10) | NOT NULL | CREDIT (reduces balance) or DEBIT (increases) |
| `amount` | DECIMAL(12,2) | NOT NULL | Adjustment value |
| `reason` | VARCHAR(1000) | NOT NULL | Mandatory audit reason |

---

## 3. Functional Requirements

### 3.1 Billing & Invoicing
- **FR-BILL-1**: The system shall allow Finance Officers to manage (Create/Update/Deactivate) recurring charge rules.
- **FR-BILL-2**: The system shall generate invoices by snapshotting active charge rules and utility charges.
- **FR-BILL-3**: The system shall prevent duplicate active invoices for the same unit and billing period.
- **FR-BILL-4**: The system shall enforce role-based access, ensuring Residents can only view invoices belonging to their own unit.

### 3.2 Payment Processing
- **FR-PAY-1**: The system shall record payments in a `PENDING` state for verification.
- **FR-PAY-2**: The system shall block payments that exceed the outstanding balance of the target invoice.
- **FR-PAY-3**: The system shall allow Finance Officers to confirm or reject pending payments.
- **FR-PAY-4**: The system shall automatically generate an immutable receipt upon payment confirmation.

### 3.3 Adjustments & Balance
- **FR-ADJ-1**: The system shall allow Finance Officers to apply `CREDIT` (balance decrease) or `DEBIT` (balance increase) adjustments to non-cancelled invoices.
- **FR-ADJ-2**: Every adjustment must include a mandatory reason for audit purposes.
- **FR-BAL-1**: The system shall calculate the outstanding balance for units and residents in real-time using the defined financial formula.

### 3.4 Financial Reporting
- **FR-REP-1**: The system shall provide a Finance Dashboard showing total invoiced, total collected, and the overall collection rate.
- **FR-REP-2**: The system shall generate an Arrears Report listing units with outstanding balances, sorted by the highest amount.
- **FR-REP-3**: The system shall provide a monthly Collection Summary for a given year.

---

## 4. User Stories

| ID | User Role | Requirement | Goal/Benefit |
|:---|:---|:---|:---|
| **US-B1** | Finance Officer | I want to define recurring charge rules | To ensure standard fees are applied consistently across units. |
| **US-B2** | Finance Officer | I want to generate monthly invoices for units | To automate the billing process and notify residents of their dues. |
| **US-P1** | Finance Officer | I want to confirm a pending payment | To update the account balance and provide the resident with a receipt. |
| **US-P2** | Resident | I want to view my payment history and download receipts | To maintain a personal record of my financial transactions. |
| **US-A1** | Finance Officer | I want to apply a credit to an invoice | To correct an overcharge or apply a promotional discount. |
| **US-R1** | Apartment Manager | I want to see the total outstanding balance across the property | To assess the financial health and cash flow of the apartment complex. |
| **US-R2** | Apartment Manager | I want an arrears report | To identify and follow up with residents who have overdue payments. |
