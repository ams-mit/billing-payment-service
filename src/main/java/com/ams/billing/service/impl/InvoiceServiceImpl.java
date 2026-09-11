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
        List<ChargeRule> activeRules = chargeRuleRepository
                .findByChargeTypeAndStatus(null, "ACTIVE")
                .stream()
                .toList();

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
                .totalAmount(BigDecimal.ZERO)
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 6. ── SNAPSHOT RULE ──────────
        List<InvoiceLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ChargeRule rule : activeRules) {
            InvoiceLine line = InvoiceLine.builder()
                    .invoice(savedInvoice)
                    .chargeRuleId(rule.getId())
                    .chargeRuleName(rule.getName())
                    .chargeType(rule.getChargeType())
                    .amount(rule.getAmount())
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

        // Role restriction: RESIDENT, TENANT, OWNER can only see their own invoices
        enforceInvoiceAccess(invoice, requestingUserId, requestingUserRoles);

        return InvoiceResponse.fromWithLines(invoice);
    }

    // ── BILL-010: Get Invoices by Unit ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoicesByUnit(String unitId, String requestingUserId,
                                                   List<String> requestingUserRoles,
                                                   Pageable pageable) {

        boolean isPrivileged = isPrivilegedRole(requestingUserRoles);

        if (!isPrivileged) {
            // For non-privileged roles, we filter by both unitId AND requestingUserId (residentId)
            return invoiceRepository.findByUnitIdAndResidentId(unitId, requestingUserId, pageable)
                    .map(InvoiceResponse::from);
        }

        return invoiceRepository.findByUnitId(unitId, pageable).map(InvoiceResponse::from);
    }

    // ── BILL-011: Get Invoice by Unit and Period ──────────────

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByUnitAndPeriod(String unitId, Integer year,
                                                     Integer month, String requestingUserId,
                                                     List<String> requestingUserRoles) {

        boolean isPrivileged = isPrivilegedRole(requestingUserRoles);

        if (!isPrivileged) {
            Invoice invoice = invoiceRepository.findByUnitIdAndBillingYearAndBillingMonthAndResidentId(
                            unitId, year.shortValue(), month.byteValue(), requestingUserId)
                    .orElseThrow(() -> new BillingException(
                            "No invoice found for your account for unit %s, period %d/%d"
                                    .formatted(unitId, month, year),
                            HttpStatus.NOT_FOUND,
                            "INVOICE_NOT_FOUND"));
            return InvoiceResponse.fromWithLines(invoice);
        }

        Invoice invoice = invoiceRepository.findByUnitIdAndBillingYearAndBillingMonth(
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

        if (invoice.getStatus() == InvoiceStatus.PAID
                && "CANCELLED".equals(request.getStatus())) {
            throw new BillingException(
                    "A paid invoice cannot be cancelled",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_STATUS_TRANSITION");
        }

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

        enforceInvoiceAccess(invoice, requestingUserId, requestingUserRoles);

        return invoiceLineRepository.findByInvoiceId(invoiceId)
                .stream()
                .map(InvoiceLineResponse::from)
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────

    private boolean isPrivilegedRole(List<String> roles) {
        log.debug("Checking privileged access for roles: {}", roles);
        boolean privileged = roles.stream().anyMatch(role ->
                role.equalsIgnoreCase("FINANCE_OFFICER") || role.equalsIgnoreCase("APARTMENT_MANAGER"));
        log.debug("Is privileged: {}", privileged);
        return privileged;
    }

    private void enforceInvoiceAccess(Invoice invoice, String requestingUserId,
                                       List<String> roles) {

        if (isPrivilegedRole(roles)) {
            log.debug("Privileged access granted to user: {}", requestingUserId);
            return;
        }

        if (!invoice.getResidentId().equals(requestingUserId)) {
            log.warn("Access denied: User {} attempted to access invoice {} belonging to resident {}",
                    requestingUserId, invoice.getId(), invoice.getResidentId());
            throw new AccessDeniedException("You are not authorized to view this invoice.");
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
