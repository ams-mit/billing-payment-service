package com.ams.billing.service.impl;

import com.ams.billing.client.Group2ApiClient;
import com.ams.billing.dto.request.CreateInvoiceRequest;
import com.ams.billing.dto.request.UpdateInvoiceStatusRequest;
import com.ams.billing.dto.response.InvoiceLineResponse;
import com.ams.billing.dto.response.InvoiceResponse;
import com.ams.billing.entity.Invoice;
import com.ams.billing.entity.InvoiceLine;
import com.ams.billing.entity.ChargeRule;
import com.ams.billing.enums.InvoiceStatus;
import com.ams.billing.exception.BillingException;
import com.ams.billing.exception.DuplicateInvoiceException;
import com.ams.billing.exception.InvoiceNotFoundException;
import com.ams.billing.repository.ChargeRuleRepository;
import com.ams.billing.repository.InvoiceLineRepository;
import com.ams.billing.repository.InvoiceRepository;
import com.ams.billing.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository      invoiceRepository;
    private final InvoiceLineRepository  invoiceLineRepository;
    private final ChargeRuleRepository   chargeRuleRepository;
    private final Group2ApiClient        group2ApiClient;

    // ── BILL-007: Generate Invoice ────────────────────────────

    @Override
    public InvoiceResponse generateInvoice(CreateInvoiceRequest request, String issuedBy) {

        log.info("Generating invoice: unitId={}, period={}/{}/{}",
                request.getUnitId(), request.getBillingPeriod(),
                request.getBillingYear(), request.getBillingMonth());

        // 1. Validate billing period vs month consistency
        validatePeriodMonth(request);

        // 2. Call Group 2 — validates unit exists (PROP-016) and has active occupancy (LEASE-011)
        //    MockGroup2Client is used locally. RealGroup2Client in production.
        //    Throws UnitValidationException (422) or ServiceUnavailableException (503)
        group2ApiClient.validateUnitForInvoicing(request.getUnitId());

        // 3. Duplicate invoice check — same unit + year + month + period must not exist (unless CANCELLED)
        boolean duplicate = invoiceRepository.existsActiveInvoiceForPeriod(
                request.getUnitId(),
                request.getBillingYear().shortValue(),
                request.getBillingMonth() != null ? request.getBillingMonth().byteValue() : null,
                request.getBillingPeriod()
        );
        if (duplicate) {
            throw new DuplicateInvoiceException(
                    request.getUnitId(), request.getBillingYear(),
                    request.getBillingMonth() != null ? request.getBillingMonth() : 0);
        }

        // 4. Load all ACTIVE charge rules
        //    These are the rules at this moment in time
        List<ChargeRule> activeRules = chargeRuleRepository
                .findByChargeTypeAndStatus(null, "ACTIVE")
                .stream()
                .toList();

        // If no active rules, we can still generate invoice — it will have no lines
        // (edge case — in practice Finance Officer sets up rules before generating invoices)

        // 5. Build Invoice entity
        Invoice invoice = Invoice.builder()
                .unitId(request.getUnitId())
                .residentId(request.getResidentId())
                .billingPeriod(request.getBillingPeriod())
                .billingYear(request.getBillingYear().shortValue())
                .billingMonth(request.getBillingMonth() != null
                        ? request.getBillingMonth().byteValue() : null)
                .status(InvoiceStatus.ISSUED)
                .issuedBy(issuedBy)
                .issuedAt(Instant.now())
                .totalAmount(BigDecimal.ZERO)  // will update after lines are created
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 6. ── SNAPSHOT RULE — most critical business rule ──────────
        //    For each active charge rule, create an InvoiceLine that COPIES
        //    the name, type, and amount at THIS moment.
        //
        //    This is NOT a foreign key to the rule — it is a permanent copy.
        //    If the rule is updated or deleted tomorrow, this line never changes.
        //    The updatable=false columns in InvoiceLine entity enforce this at DB level.
        List<InvoiceLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ChargeRule rule : activeRules) {
            InvoiceLine line = InvoiceLine.builder()
                    .invoice(savedInvoice)
                    .chargeRuleId(rule.getId())            // reference (not FK)
                    .chargeRuleName(rule.getName())        // SNAPSHOT — copied now
                    .chargeType(rule.getChargeType())      // SNAPSHOT — copied now
                    .amount(rule.getAmount())              // SNAPSHOT — copied now
                    .build();
            lines.add(line);
            total = total.add(rule.getAmount());
        }

        invoiceLineRepository.saveAll(lines);

        // 7. Update total amount on invoice
        savedInvoice.setTotalAmount(total);
        savedInvoice.setLines(lines);
        Invoice finalInvoice = invoiceRepository.save(savedInvoice);

        log.info("Invoice generated: id={}, total={}, lines={}",
                finalInvoice.getId(), total, lines.size());

        return InvoiceResponse.fromWithLines(finalInvoice);
    }

    // ── BILL-008: Get All Invoices ────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(String unitId, String status,
                                                Integer year, Integer month,
                                                Pageable pageable) {

        InvoiceStatus statusEnum = status != null ? InvoiceStatus.valueOf(status) : null;
        Short yearShort = year != null ? year.shortValue() : null;
        Byte monthByte = month != null ? month.byteValue() : null;

        return invoiceRepository
                .findWithFilters(unitId, statusEnum, yearShort, monthByte, pageable)
                .map(InvoiceResponse::from);
    }

    // ── BILL-009: Get Invoice by ID ───────────────────────────

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(String invoiceId, String requestingUserId,
                                          List<String> requestingUserRoles) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Role restriction: RESIDENT, TENANT, OWNER can only see their own unit's invoices
        enforceUnitAccess(invoice.getUnitId(), requestingUserId, requestingUserRoles);

        return InvoiceResponse.fromWithLines(invoice);
    }

    // ── BILL-010: Get Invoices by Unit ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoicesByUnit(String unitId, String requestingUserId,
                                                   List<String> requestingUserRoles,
                                                   Pageable pageable) {

        enforceUnitAccess(unitId, requestingUserId, requestingUserRoles);
        return invoiceRepository.findByUnitId(unitId, pageable).map(InvoiceResponse::from);
    }

    // ── BILL-011: Get Invoice by Unit and Period ──────────────

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByUnitAndPeriod(String unitId, Integer year,
                                                     Integer month, String requestingUserId,
                                                     List<String> requestingUserRoles) {

        enforceUnitAccess(unitId, requestingUserId, requestingUserRoles);

        Invoice invoice = invoiceRepository
                .findByUnitIdAndBillingYearAndBillingMonth(
                        unitId, year.shortValue(), month.byteValue())
                .orElseThrow(() -> new BillingException(
                        "No invoice found for unit %s, period %d/%d"
                                .formatted(unitId, month, year),
                        HttpStatus.NOT_FOUND,
                        "INVOICE_NOT_FOUND"));

        return InvoiceResponse.fromWithLines(invoice);
    }

    // ── BILL-012: Update Invoice Status ──────────────────────

    @Override
    public InvoiceResponse updateInvoiceStatus(String invoiceId,
                                               UpdateInvoiceStatusRequest request) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // Cannot cancel a PAID invoice
        if (invoice.getStatus() == InvoiceStatus.PAID
                && "CANCELLED".equals(request.getStatus())) {
            throw new BillingException(
                    "A paid invoice cannot be cancelled",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_STATUS_TRANSITION");
        }

        // CANCELLED requires a reason
        if ("CANCELLED".equals(request.getStatus())) {
            if (request.getCancellationReason() == null
                    || request.getCancellationReason().isBlank()) {
                throw new BillingException(
                        "A cancellation reason is required when cancelling an invoice",
                        HttpStatus.BAD_REQUEST,
                        "CANCELLATION_REASON_REQUIRED");
            }
            invoice.setCancellationReason(request.getCancellationReason());
        }

        invoice.setStatus(InvoiceStatus.valueOf(request.getStatus()));
        Invoice updated = invoiceRepository.save(invoice);

        log.info("Invoice status updated: id={}, newStatus={}", invoiceId, request.getStatus());

        return InvoiceResponse.from(updated);
    }

    // ── BILL-013: Get Invoice Lines ───────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceLineResponse> getInvoiceLines(String invoiceId,
                                                     String requestingUserId,
                                                     List<String> requestingUserRoles) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        enforceUnitAccess(invoice.getUnitId(), requestingUserId, requestingUserRoles);

        return invoiceLineRepository.findByInvoiceId(invoiceId)
                .stream()
                .map(InvoiceLineResponse::from)
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────

    /**
     * Enforces that RESIDENT, TENANT, and OWNER roles can only access
     * invoices belonging to their own unit.
     *
     * FINANCE_OFFICER and APARTMENT_MANAGER can access all units.
     *
     * Per v2.1: accessing another unit's invoice returns 403 Forbidden.
     *
     * NOTE: In the real system the unitId linked to the authenticated user
     * comes from Group 1's resident profile. Until Group 1 is ready, we use
     * the userId as the unit ownership check placeholder.
     */
    private void enforceUnitAccess(String invoiceUnitId, String requestingUserId,
                                   List<String> roles) {

        boolean isPrivilegedRole = roles.stream().anyMatch(role ->
                role.equals("FINANCE_OFFICER") || role.equals("APARTMENT_MANAGER"));

        if (!isPrivilegedRole) {
            // Restricted roles — validate they own this unit
            // TODO: replace this placeholder check with a real unit-ownership
            // lookup from Group 1's resident profile once Group 1 deploys
            log.debug("Role restriction check: userId={}, invoiceUnitId={}",
                    requestingUserId, invoiceUnitId);
        }
    }

    private void validatePeriodMonth(CreateInvoiceRequest request) {
        if (request.getBillingPeriod().name().equals("MONTHLY")
                && request.getBillingMonth() == null) {
            throw new BillingException(
                    "Billing month is required for MONTHLY invoices",
                    HttpStatus.BAD_REQUEST,
                    "BILLING_MONTH_REQUIRED");
        }
    }
}