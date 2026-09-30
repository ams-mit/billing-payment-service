# Billing & Payment Service API Reference

This document provides the complete API list for the Billing & Payment Service, including public endpoints and internal service-to-service APIs.

## 1. Implemented API Endpoints

### A. Public APIs (Exposed via Gateway)

| Method | Endpoint | Purpose | Provider | Role/Auth | Request | Response | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/charge-rules` | Create a recurring charge rule | Billing | `FINANCE_OFFICER` | `CreateChargeRuleRequest` | `ChargeRuleResponse` | Implemented |
| **GET** | `/api/v1/charge-rules` | List charge rules (paginated, filterable) | Billing | `FINANCE_OFFICER`, `APARTMENT_MANAGER` | Query Params (status, chargeType) | `Page<ChargeRuleResponse>` | Implemented |
| **GET** | `/api/v1/charge-rules/{id}` | Get charge rule by ID | Billing | `FINANCE_OFFICER`, `APARTMENT_MANAGER` | Path: `chargeRuleId` | `ChargeRuleResponse` | Implemented |
| **PUT** | `/api/v1/charge-rules/{id}` | Update charge rule details | Billing | `FINANCE_OFFICER` | `UpdateChargeRuleRequest` | `ChargeRuleResponse` | Implemented |
| **PATCH** | `/api/v1/charge-rules/{id}/status` | Activate/Deactivate charge rule | Billing | `FINANCE_OFFICER` | `UpdateStatusRequest` | `ChargeRuleResponse` | Implemented |
| **GET** | `/api/v1/charge-rules/type/{type}` | Get active rules by charge type | Billing | `FINANCE_OFFICER`, `APARTMENT_MANAGER` | Path: `chargeType` | `List<ChargeRuleResponse>` | Implemented |
| **POST** | `/api/v1/invoices` | Generate invoice for unit & period | Billing | `FINANCE_OFFICER` | `CreateInvoiceRequest` | `InvoiceResponse` | Implemented |
| **GET** | `/api/v1/invoices` | List invoices (paginated, filterable) | Billing | `FINANCE_OFFICER`, `APARTMENT_MANAGER` | Query Params (unitId, status, etc) | `Page<InvoiceResponse>` | Implemented |
| **GET** | `/api/v1/invoices/{id}` | Get invoice by ID | Billing | `Authenticated` | Path: `invoiceId` | `InvoiceResponse` | Implemented |
| **GET** | `/api/v1/invoices/units/{unitId}` | Get invoices for a specific unit | Billing | `Authenticated` | Path: `unitId` | `Page<InvoiceResponse>` | Implemented |
| **GET** | `/api/v1/invoices/units/{uId}/period/{y}/{m}` | Get invoice for unit and period | Billing | `Authenticated` | Path: `unitId, year, month` | `InvoiceResponse` | Implemented |
| **PATCH** | `/api/v1/invoices/{id}/status` | Update invoice status (CANCELLED/OVERDUE) | Billing | `FINANCE_OFFICER` | `UpdateInvoiceStatusRequest` | `InvoiceResponse` | Implemented |
| **GET** | `/api/v1/invoices/{id}/lines` | Get all line items for an invoice | Billing | `Authenticated` | Path: `invoiceId` | `List<InvoiceLineResponse>` | Implemented |
| **POST** | `/api/v1/payments` | Record a simulated payment | Billing | `FINANCE_OFFICER` | `RecordPaymentRequest` | `PaymentResponse` | Implemented |
| **GET** | `/api/v1/payments` | List payments (paginated, filterable) | Billing | `FINANCE_OFFICER`, `APARTMENT_MANAGER` | Query Params (invoiceId, status) | `Page<PaymentResponse>` | Implemented |
| **GET** | `/api/v1/payments/{id}` | Get payment by ID | Billing | `Authenticated` | Path: `paymentId` | `PaymentResponse` | Implemented |
| **GET** | `/api/v1/payments/invoices/{id}` | Get payments for an invoice | Billing | `Authenticated` | Path: `invoiceId` | `Page<PaymentResponse>` | Implemented |
| **GET** | `/api/v1/payments/units/{unitId}` | Get payment history for a unit | Billing | `Authenticated` | Path: `unitId` | `Page<PaymentResponse>` | Implemented |
| **GET** | `/api/v1/payments/residents/{id}` | Get payments for a resident | Billing | `Authenticated` | Path: `residentId` | `Page<PaymentResponse>` | Implemented |
| **PATCH** | `/api/v1/payments/{id}/status` | Confirm or reject a payment | Billing | `FINANCE_OFFICER` | `UpdatePaymentStatusRequest` | `PaymentResponse` | Implemented |
| **GET** | `/api/v1/receipts/{id}` | Get receipt by ID | Billing | `Authenticated` | Path: `receiptId` | `ReceiptResponse` | Implemented |
| **GET** | `/api/v1/receipts/payments/{id}` | Get receipt for a specific payment | Billing | `Authenticated` | Path: `paymentId` | `ReceiptResponse` | Implemented |
| **GET** | `/api/v1/receipts/units/{unitId}` | Get all receipts for a unit | Billing | `Authenticated` | Path: `unitId` | `Page<ReceiptResponse>` | Implemented |
| **POST** | `/api/v1/adjustments` | Apply CREDIT/DEBIT adjustment | Billing | `FINANCE_OFFICER` | `CreateAdjustmentRequest` | `AdjustmentResponse` | Implemented |
| **GET** | `/api/v1/adjustments/invoices/{id}` | Get adjustments for an invoice | Billing | `FINANCE_OFFICER`, `APARTMENT_MANAGER` | Path: `invoiceId` | `List<AdjustmentResponse>` | Implemented |
| **GET** | `/api/v1/balance/units/{unitId}` | Get outstanding balance for unit | Billing | `Authenticated` | Path: `unitId` | `BalanceResponse` | Implemented |
| **GET** | `/api/v1/balance/residents/{id}` | Get outstanding balance for resident | Billing | `Authenticated` | Path: `residentId` | `BalanceResponse` | Implemented |
| **GET** | `/api/v1/reports/finance-dashboard` | Get finance dashboard summary | Billing | `FINANCE_OFFICER`, `APARTMENT_MANAGER` | N/A | `FinanceDashboardResponse` | Implemented |
| **GET** | `/api/v1/reports/arrears` | Get arrears report | Billing | `FINANCE_OFFICER`, `APARTMENT_MANAGER` | Query Params (buildingId, etc) | `Page<ArrearsReportResponse>` | Implemented |
| **GET** | `/api/v1/reports/collection-summary` | Get monthly collection summary | Billing | `FINANCE_OFFICER`, `APARTMENT_MANAGER` | Query Param: `year` | `List<CollectionSummaryResponse>` | Implemented |
| **GET** | `/api/v1/reports/payment-history` | Get full payment history audit | Billing | `FINANCE_OFFICER`, `APARTMENT_MANAGER` | Query Params (year, month, etc) | `Page<PaymentHistoryResponse>` | Implemented |

### B. Internal APIs (Service-to-Service)

| Method | Endpoint | Purpose | Provider | Consumer | Role/Auth | Request | Response | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/internal/balance/{unitId}` | Check balance before booking approval | Billing | `community-service` | `SERVICE` (JWT) | Path: `unitId` | `BalanceResponse` | Implemented |

## 2. Required APIs from Other Services

| Method | Endpoint | Purpose | Provider | Consumer | Role/Auth | Request | Response | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/units/{unitId}` | Validate unit existence/status during invoice generation | `unit-service` (Group 2) | Billing | `SERVICE` | Path: `unitId` | `UnitResponse` | Required |
| **GET** | `/api/v1/residents/{id}` | Validate resident identity for balance checks | `resident-service` | Billing | `SERVICE` | Path: `residentId` | `ResidentResponse` | Required |
