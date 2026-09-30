# Project A — Canonical Complete API Endpoint Registry

| API ID | Method | Endpoint | API Type | Authentication | Required Role | Purpose |
|---|---|---|---|---|---|---|

## identity-access-service

| API ID | Method | Endpoint | API Type | Authentication | Required Role | Purpose |
|---|---|---|---|---|---|---|
| AUTH-001 | POST | `/api/v1/auth/login` | PUBLIC | None | None | Authenticate user and issue User JWT |
| AUTH-002 | POST | `/api/v1/auth/register` | PUBLIC | None | None | Self-register an allowed user account |
| AUTH-003 | GET | `/api/v1/profile` | USER | Gateway User JWT | Authenticated user | Get current user's profile |
| AUTH-004 | PUT | `/api/v1/profile/password` | USER | Gateway User JWT | Authenticated user | Change current user's password |
| AUTH-005 | POST | `/api/v1/admin/users` | USER | Gateway User JWT | SYSTEM_ADMIN | Create role-based user account |
| AUTH-006 | GET | `/api/v1/admin/users` | USER | Gateway User JWT | SYSTEM_ADMIN | List and filter users |
| AUTH-007 | GET | `/api/v1/admin/users/{userId}` | USER | Gateway User JWT | SYSTEM_ADMIN | Get user details |
| AUTH-008 | PATCH | `/api/v1/admin/users/{userId}/status` | USER | Gateway User JWT | SYSTEM_ADMIN | Activate or deactivate user |
| AUTH-009 | GET | `/api/v1/admin/roles` | USER | Gateway User JWT | SYSTEM_ADMIN | List roles and permissions |
| AUTH-010 | GET | `/api/v1/internal/users/{userId}/validate` | INTERNAL | Gateway Service JWT | Authorized services | Validate user existence, active status and primary roles |
| AUTH-011 | GET | `/api/v1/internal/owners/{ownerId}/validate` | INTERNAL | Gateway Service JWT | Authorized services | Validate owner |
| AUTH-012 | GET | `/api/v1/internal/residents/{residentId}/validate` | INTERNAL | Gateway Service JWT | Authorized services | Validate resident |
| AUTH-013 | GET | `/actuator/health` | HEALTH | None | None | Service health |
| AUTH-014 | GET | `/actuator/info` | HEALTH | None | None | Service metadata |

## resident-management-service

| API ID | Method | Endpoint | API Type | Authentication | Required Role | Purpose |
|---|---|---|---|---|---|---|
| RES-001 | POST | `/api/v1/residents` | USER | Gateway User JWT | APARTMENT_MANAGER | Create resident profile |
| RES-002 | GET | `/api/v1/residents` | USER | Gateway User JWT | APARTMENT_MANAGER | List/filter residents |
| RES-003 | GET | `/api/v1/residents/{residentId}` | USER | Gateway User JWT | APARTMENT_MANAGER, RESIDENT | Get resident profile |
| RES-004 | PUT | `/api/v1/residents/{residentId}` | USER | Gateway User JWT | APARTMENT_MANAGER, RESIDENT | Update resident profile |
| RES-005 | PATCH | `/api/v1/residents/{residentId}/status` | USER | Gateway User JWT | APARTMENT_MANAGER | Change resident status |
| RES-006 | POST | `/api/v1/owners` | USER | Gateway User JWT | APARTMENT_MANAGER | Create owner profile |
| RES-007 | GET | `/api/v1/owners` | USER | Gateway User JWT | APARTMENT_MANAGER | List/filter owners |
| RES-008 | GET | `/api/v1/owners/{ownerId}` | USER | Gateway User JWT | APARTMENT_MANAGER, OWNER | Get owner profile |
| RES-009 | PUT | `/api/v1/owners/{ownerId}` | USER | Gateway User JWT | APARTMENT_MANAGER, OWNER | Update owner profile |
| RES-010 | PATCH | `/api/v1/owners/{ownerId}/status` | USER | Gateway User JWT | APARTMENT_MANAGER | Change owner status |
| RES-011 | POST | `/api/v1/tenants` | USER | Gateway User JWT | APARTMENT_MANAGER | Create tenant profile |
| RES-012 | GET | `/api/v1/tenants` | USER | Gateway User JWT | APARTMENT_MANAGER | List/filter tenants |
| RES-013 | GET | `/api/v1/tenants/{tenantId}` | USER | Gateway User JWT | APARTMENT_MANAGER, TENANT | Get tenant profile |
| RES-014 | PUT | `/api/v1/tenants/{tenantId}` | USER | Gateway User JWT | APARTMENT_MANAGER, TENANT | Update tenant profile |
| RES-015 | PATCH | `/api/v1/tenants/{tenantId}/status` | USER | Gateway User JWT | APARTMENT_MANAGER | Change tenant status |
| RES-016 | POST | `/api/v1/staff` | USER | Gateway User JWT | APARTMENT_MANAGER, SYSTEM_ADMIN | Create staff profile |
| RES-017 | GET | `/api/v1/staff` | USER | Gateway User JWT | APARTMENT_MANAGER, SYSTEM_ADMIN | List/filter staff |
| RES-018 | GET | `/api/v1/staff/{staffId}` | USER | Gateway User JWT | APARTMENT_MANAGER, SYSTEM_ADMIN | Get staff profile |
| RES-019 | PUT | `/api/v1/staff/{staffId}` | USER | Gateway User JWT | APARTMENT_MANAGER, SYSTEM_ADMIN | Update staff profile |
| RES-020 | PATCH | `/api/v1/staff/{staffId}/status` | USER | Gateway User JWT | APARTMENT_MANAGER, SYSTEM_ADMIN | Change staff status |
| RES-021 | GET | `/actuator/health` | HEALTH | None | None | Service health |
| RES-022 | GET | `/actuator/info` | HEALTH | None | None | Service metadata |

## property-unit-service

| API ID | Method | Endpoint | API Type | Authentication | Required Role | Purpose |
|---|---|---|---|---|---|---|
| PROP-001 | POST | `/api/v1/buildings` | USER | Gateway User JWT | APARTMENT_MANAGER | Create building |
| PROP-002 | GET | `/api/v1/buildings` | USER | Gateway User JWT | Authenticated roles | List buildings |
| PROP-003 | GET | `/api/v1/buildings/{buildingId}` | USER | Gateway User JWT | Authenticated roles | Get building |
| PROP-004 | POST | `/api/v1/floors` | USER | Gateway User JWT | APARTMENT_MANAGER | Create floor |
| PROP-005 | GET | `/api/v1/floors` | USER | Gateway User JWT | Authenticated roles | List floors |
| PROP-006 | GET | `/api/v1/floors/{floorId}` | USER | Gateway User JWT | Authenticated roles | Get floor |
| PROP-007 | POST | `/api/v1/unit-types` | USER | Gateway User JWT | APARTMENT_MANAGER | Create unit type |
| PROP-008 | GET | `/api/v1/unit-types` | USER | Gateway User JWT | Authenticated roles | List unit types |
| PROP-009 | POST | `/api/v1/units` | USER | Gateway User JWT | APARTMENT_MANAGER | Create unit |
| PROP-010 | GET | `/api/v1/units` | USER | Gateway User JWT | Authenticated roles | List/filter units |
| PROP-011 | GET | `/api/v1/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get unit, including status |
| PROP-012 | PATCH | `/api/v1/units/{unitId}/status` | USER | Gateway User JWT | APARTMENT_MANAGER | Change unit status |
| PROP-013 | POST | `/api/v1/ownerships` | USER | Gateway User JWT | APARTMENT_MANAGER | Create ownership record |
| PROP-014 | GET | `/api/v1/ownerships/units/{unitId}` | USER | Gateway User JWT | APARTMENT_MANAGER, OWNER | Get ownership for unit |
| PROP-015 | GET | `/api/v1/ownerships/owners/{ownerId}` | USER | Gateway User JWT | APARTMENT_MANAGER, OWNER | Get units owned by owner |
| PROP-016 | GET | `/api/v1/internal/units/{unitId}/exists` | INTERNAL | Gateway Service JWT | Authorized services | Validate unit existence |
| PROP-017 | GET | `/actuator/health` | HEALTH | None | None | Service health |
| PROP-018 | GET | `/actuator/info` | HEALTH | None | None | Service metadata |

## lease-occupancy-service

| API ID | Method | Endpoint | API Type | Authentication | Required Role | Purpose |
|---|---|---|---|---|---|---|
| LEASE-001 | POST | `/api/v1/leases` | USER | Gateway User JWT | APARTMENT_MANAGER | Create lease |
| LEASE-002 | GET | `/api/v1/leases` | USER | Gateway User JWT | APARTMENT_MANAGER | List/filter leases |
| LEASE-003 | GET | `/api/v1/leases/{leaseId}` | USER | Gateway User JWT | APARTMENT_MANAGER, TENANT, RESIDENT | Get lease |
| LEASE-004 | GET | `/api/v1/leases/units/{unitId}` | USER | Gateway User JWT | APARTMENT_MANAGER, OWNER | Get leases for unit |
| LEASE-005 | PATCH | `/api/v1/leases/{leaseId}/status` | USER | Gateway User JWT | APARTMENT_MANAGER | Activate/terminate/complete lease |
| LEASE-006 | GET | `/api/v1/leases/validate` | INTERNAL | Gateway Service JWT | Authorized services | Validate active lease for a unit/date range |
| LEASE-007 | POST | `/api/v1/occupancies` | USER | Gateway User JWT | APARTMENT_MANAGER | Create occupancy |
| LEASE-008 | GET | `/api/v1/occupancies/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get unit occupancy |
| LEASE-009 | GET | `/api/v1/occupancies/residents/{residentId}` | USER | Gateway User JWT | Authenticated roles | Get resident occupancy |
| LEASE-010 | PATCH | `/api/v1/occupancies/{occupancyId}/status` | USER | Gateway User JWT | APARTMENT_MANAGER | Change occupancy status |
| LEASE-011 | GET | `/api/v1/internal/occupancies/active-billing` | INTERNAL | Gateway Service JWT | Authorized services | Return occupancy eligible for billing |
| LEASE-012 | GET | `/api/v1/internal/occupancies/validate` | INTERNAL | Gateway Service JWT | Authorized services | Validate occupancy |
| LEASE-013 | GET | `/actuator/health` | HEALTH | None | None | Service health |
| LEASE-014 | GET | `/actuator/info` | HEALTH | None | None | Service metadata |

## billing-payment-service

| API ID | Method | Endpoint | API Type | Authentication | Required Role | Purpose |
|---|---|---|---|---|---|---|
| BILL-001 | POST | `/api/v1/charge-rules` | USER | Gateway User JWT | FINANCE_OFFICER | Create charge rule |
| BILL-002 | GET | `/api/v1/charge-rules` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List/filter charge rules |
| BILL-003 | GET | `/api/v1/charge-rules/{chargeRuleId}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Get charge rule |
| BILL-004 | PUT | `/api/v1/charge-rules/{chargeRuleId}` | USER | Gateway User JWT | FINANCE_OFFICER | Update charge rule |
| BILL-005 | PATCH | `/api/v1/charge-rules/{chargeRuleId}/status` | USER | Gateway User JWT | FINANCE_OFFICER | Change charge rule status |
| BILL-006 | GET | `/api/v1/charge-rules/type/{chargeType}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List charge rules by type |
| BILL-007 | POST | `/api/v1/invoices` | USER | Gateway User JWT | FINANCE_OFFICER | Generate invoice |
| BILL-008 | GET | `/api/v1/invoices` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List/filter invoices |
| BILL-009 | GET | `/api/v1/invoices/{invoiceId}` | USER | Gateway User JWT | Authenticated roles | Get invoice |
| BILL-010 | GET | `/api/v1/invoices/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get unit invoices |
| BILL-011 | GET | `/api/v1/invoices/units/{unitId}/period/{year}/{month}` | USER | Gateway User JWT | Authenticated roles | Get invoice for unit and period |
| BILL-012 | PATCH | `/api/v1/invoices/{invoiceId}/status` | USER | Gateway User JWT | FINANCE_OFFICER | Change invoice status |
| BILL-013 | GET | `/api/v1/invoices/{invoiceId}/lines` | USER | Gateway User JWT | Authenticated roles | Get invoice lines |
| BILL-014 | POST | `/api/v1/payments` | USER | Gateway User JWT | FINANCE_OFFICER | Record simulated payment |
| BILL-015 | GET | `/api/v1/payments` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List/filter payments |
| BILL-016 | GET | `/api/v1/payments/{paymentId}` | USER | Gateway User JWT | Authenticated roles | Get payment |
| BILL-017 | GET | `/api/v1/payments/invoices/{invoiceId}` | USER | Gateway User JWT | Authenticated roles | Get payments for invoice |
| BILL-018 | GET | `/api/v1/payments/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get payments for unit |
| BILL-019 | GET | `/api/v1/payments/residents/{residentId}` | USER | Gateway User JWT | Authenticated roles | Get payments for resident |
| BILL-020 | PATCH | `/api/v1/payments/{paymentId}/status` | USER | Gateway User JWT | FINANCE_OFFICER | Confirm/reject payment |
| BILL-021 | GET | `/api/v1/receipts/{receiptId}` | USER | Gateway User JWT | Authenticated roles | Get receipt |
| BILL-022 | GET | `/api/v1/receipts/payments/{paymentId}` | USER | Gateway User JWT | Authenticated roles | Get receipt for payment |
| BILL-023 | GET | `/api/v1/receipts/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get receipts for unit |
| BILL-024 | POST | `/api/v1/adjustments` | USER | Gateway User JWT | FINANCE_OFFICER | Create credit/debit adjustment |
| BILL-025 | GET | `/api/v1/adjustments/invoices/{invoiceId}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List invoice adjustments |
| BILL-026 | GET | `/api/v1/balance/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get unit outstanding balance |
| BILL-027 | GET | `/api/v1/balance/residents/{residentId}` | USER | Gateway User JWT | Authenticated roles | Get resident balance |
| BILL-028 | GET | `/api/v1/internal/balance/{unitId}` | INTERNAL | Gateway Service JWT | Authorized services | Provide unit balance to internal consumers |
| BILL-029 | GET | `/api/v1/reports/finance-dashboard` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Financial dashboard |
| BILL-030 | GET | `/api/v1/reports/arrears` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Arrears report |
| BILL-031 | GET | `/api/v1/reports/collection-summary` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Collection summary |
| BILL-032 | GET | `/api/v1/reports/payment-history` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Payment history report |
| BILL-033 | GET | `/actuator/health` | HEALTH | None | None | Service health |
| BILL-034 | GET | `/actuator/info` | HEALTH | None | None | Service metadata |

## utility-charge-service

| API ID | Method | Endpoint | API Type | Authentication | Required Role | Purpose |
|---|---|---|---|---|---|---|
| UTIL-001 | POST | `/api/v1/utility-rates` | USER | Gateway User JWT | FINANCE_OFFICER | Create utility rate |
| UTIL-002 | GET | `/api/v1/utility-rates` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List/filter utility rates |
| UTIL-003 | GET | `/api/v1/utility-rates/{utilityRateId}` | USER | Gateway User JWT | FINANCE_OFFICER | Get utility rate |
| UTIL-004 | PUT | `/api/v1/utility-rates/{utilityRateId}` | USER | Gateway User JWT | FINANCE_OFFICER | Update utility rate |
| UTIL-005 | PATCH | `/api/v1/utility-rates/{utilityRateId}/status` | USER | Gateway User JWT | FINANCE_OFFICER | Change utility rate status |
| UTIL-006 | POST | `/api/v1/utility-charges` | USER | Gateway User JWT | FINANCE_OFFICER | Create utility charge |
| UTIL-007 | GET | `/api/v1/utility-charges` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | List/filter utility charges |
| UTIL-008 | GET | `/api/v1/utility-charges/{utilityChargeId}` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Get utility charge |
| UTIL-009 | PUT | `/api/v1/utility-charges/{utilityChargeId}` | USER | Gateway User JWT | FINANCE_OFFICER | Update utility charge |
| UTIL-010 | DELETE | `/api/v1/utility-charges/{utilityChargeId}` | USER | Gateway User JWT | FINANCE_OFFICER | Delete utility charge before invoice generation |
| UTIL-011 | GET | `/api/v1/utility-charges/units/{unitId}` | USER | Gateway User JWT | Authenticated roles | Get utility charges for unit |
| UTIL-012 | GET | `/api/v1/utility-charges/units/{unitId}/period/{year}/{month}` | USER | Gateway User JWT | Authenticated roles | Get utility charge for unit and period |
| UTIL-013 | GET | `/api/v1/utility-charges/summary` | USER | Gateway User JWT | FINANCE_OFFICER, APARTMENT_MANAGER | Get utility charge summary |
| UTIL-014 | GET | `/api/v1/internal/utility-charges/units/{unitId}` | INTERNAL | Gateway Service JWT | Authorized services | Provide period utility charges to billing |
| UTIL-015 | GET | `/actuator/health` | HEALTH | None | None | Service health |
| UTIL-016 | GET | `/actuator/info` | HEALTH | None | None | Service metadata |

## operations-service

| API ID | Method | Endpoint | API Type | Authentication | Required Role | Purpose |
|---|---|---|---|---|---|---|
| OPS-001 | POST | `/api/v1/maintenance-requests` | USER | Gateway User JWT | RESIDENT, TENANT, APARTMENT_MANAGER, MAINTENANCE_COORDINATOR, SERVICE_STAFF | Submit maintenance request or complaint |
| OPS-002 | GET | `/api/v1/maintenance-requests` | USER | Gateway User JWT | Authorized requester/staff roles | List and track maintenance requests |
| OPS-003 | GET | `/api/v1/maintenance-requests/{requestId}` | USER | Gateway User JWT | Authorized user | Get maintenance request |
| OPS-004 | POST | `/api/v1/work-orders` | USER | Gateway User JWT | MAINTENANCE_COORDINATOR, SYSTEM_ADMIN | Create work order |
| OPS-005 | GET | `/api/v1/work-orders` | USER | Gateway User JWT | TECHNICIAN, SERVICE_STAFF, MAINTENANCE_COORDINATOR | List/filter work orders |
| OPS-006 | PATCH | `/api/v1/work-orders/{orderId}/status` | USER | Gateway User JWT | TECHNICIAN, MAINTENANCE_COORDINATOR | Update work-order status and resolution |
| OPS-007 | GET | `/actuator/health` | HEALTH | None | None | Service health |
| OPS-008 | GET | `/actuator/info` | HEALTH | None | None | Service metadata |

## community-service

| API ID | Method | Endpoint | API Type | Authentication | Required Role | Purpose |
|---|---|---|---|---|---|---|
| COMM-001 | GET | `/api/v1/facilities` | USER | Gateway User JWT | Authenticated roles | List available facilities |
| COMM-002 | POST | `/api/v1/facilities/reservations` | USER | Gateway User JWT | RESIDENT, TENANT, OWNER | Create facility reservation |
| COMM-003 | PATCH | `/api/v1/facilities/reservations/{bookingId}/status` | USER | Gateway User JWT | APARTMENT_MANAGER, SERVICE_STAFF, SYSTEM_ADMIN | Approve/reject/cancel reservation |
| COMM-004 | POST | `/api/v1/visitors` | USER | Gateway User JWT | RESIDENT, TENANT, OWNER | Register visitor |
| COMM-005 | PATCH | `/api/v1/visitors/{visitorId}/check-in` | USER | Gateway User JWT | SECURITY_OFFICER | Check visitor in |
| COMM-006 | POST | `/api/v1/announcements` | USER | Gateway User JWT | SYSTEM_ADMIN, APARTMENT_MANAGER | Publish announcement |
| COMM-007 | GET | `/api/v1/announcements` | USER | Gateway User JWT | Authenticated roles | List visible announcements |
| COMM-008 | POST | `/api/v1/notifications` | INTERNAL | Gateway Service JWT | Authorized services | Create workflow notification |
| COMM-009 | GET | `/api/v1/notifications` | USER | Gateway User JWT | Authenticated roles | List current user's notifications |
| COMM-010 | PATCH | `/api/v1/notifications/{notificationId}/read` | USER | Gateway User JWT | Authenticated user | Mark notification as read |
| COMM-011 | GET | `/actuator/health` | HEALTH | None | None | Service health |
| COMM-012 | GET | `/actuator/info` | HEALTH | None | None | Service metadata |
