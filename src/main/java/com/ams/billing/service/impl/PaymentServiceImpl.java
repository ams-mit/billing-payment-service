package com.ams.billing.service.impl;

import com.ams.billing.dto.request.RecordPaymentRequest;
import com.ams.billing.dto.request.UpdatePaymentStatusRequest;
import com.ams.billing.dto.response.PaymentResponse;
import com.ams.billing.entity.Invoice;
import com.ams.billing.entity.Payment;
import com.ams.billing.enums.InvoiceStatus;
import com.ams.billing.enums.PaymentStatus;
import com.ams.billing.exception.BillingException;
import com.ams.billing.exception.InvoiceNotFoundException;
import com.ams.billing.exception.OverpaymentException;
import com.ams.billing.exception.PaymentNotFoundException;
import com.ams.billing.repository.InvoiceRepository;
import com.ams.billing.repository.PaymentRepository;
import com.ams.billing.service.BalanceService;
import com.ams.billing.service.PaymentService;
import com.ams.billing.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository  paymentRepository;
    private final InvoiceRepository  invoiceRepository;
    private final ReceiptService     receiptService;
    private final BalanceService     balanceService;

    /**
     * @Lazy on ReceiptService and BalanceService prevents circular
     * dependency issues since they indirectly reference payment data.
     */
    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            @Lazy ReceiptService receiptService,
            @Lazy BalanceService balanceService) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository  = invoiceRepository;
        this.receiptService     = receiptService;
        this.balanceService     = balanceService;
    }

    // ── BILL-014: Record Payment ──────────────────────────────

    @Override
    public PaymentResponse recordPayment(RecordPaymentRequest request,
                                         String recordedBy) {

        log.info("Recording payment: invoiceId={}, amount={}, recordedBy={}",
                request.getInvoiceId(), request.getAmount(), recordedBy);

        // Load invoice — 404 if not found
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(request.getInvoiceId()));

        // Cannot record payment against a cancelled invoice
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BillingException(
                    "Cannot record a payment against a cancelled invoice",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVOICE_CANCELLED");
        }

        // Cannot record payment against an already fully paid invoice
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException(
                    "This invoice is already fully paid",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVOICE_ALREADY_PAID");
        }

        // Calculate current outstanding balance
        // Formula: invoice total - sum of all CONFIRMED payments
        BigDecimal confirmedPayments = paymentRepository
                .sumConfirmedAmountByInvoiceId(invoice.getId());
        BigDecimal outstandingBalance = invoice.getTotalAmount()
                .subtract(confirmedPayments);

        // Overpayment check — 422 per v2.1 spec
        if (request.getAmount().compareTo(outstandingBalance) > 0) {
            log.warn("Overpayment attempted: invoiceId={}, requested={}, outstanding={}",
                    invoice.getId(), request.getAmount(), outstandingBalance);
            throw new OverpaymentException();
        }

        // Build payment — starts as PENDING
        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .status(PaymentStatus.PENDING)
                .recordedBy(recordedBy)
                .build();

        Payment saved = paymentRepository.save(payment);

        log.info("Payment recorded: id={}, status=PENDING", saved.getId());

        return PaymentResponse.from(saved);
    }

    // ── BILL-015: Get All Payments ────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(String invoiceId,
                                                PaymentStatus status,
                                                Pageable pageable) {
        return paymentRepository
                .findWithFilters(invoiceId, status, pageable)
                .map(PaymentResponse::from);
    }

    // ── BILL-016: Get Payment by ID ───────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId,
                                          String requestingUserId,
                                          List<String> roles) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        enforceUnitAccess(payment.getInvoice().getUnitId(),
                payment.getInvoice().getResidentId(), requestingUserId, roles);

        return PaymentResponse.from(payment);
    }

    // ── BILL-017: Get Payments by Invoice ────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByInvoice(String invoiceId,
                                                      String requestingUserId,
                                                      List<String> roles,
                                                      Pageable pageable) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        enforceUnitAccess(invoice.getUnitId(), invoice.getResidentId(),
                requestingUserId, roles);

        return paymentRepository.findByInvoiceId(invoiceId, pageable)
                .map(PaymentResponse::from);
    }

    // ── BILL-018: Get Payments by Unit ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByUnit(String unitId,
                                                   String requestingUserId,
                                                   List<String> roles,
                                                   Pageable pageable) {
        enforceUnitAccess(unitId, null, requestingUserId, roles);
        return paymentRepository.findByUnitId(unitId, pageable)
                .map(PaymentResponse::from);
    }

    // ── BILL-019: Get Payments by Resident ───────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByResident(String residentId,
                                                       String requestingUserId,
                                                       List<String> roles,
                                                       Pageable pageable) {
        enforceResidentAccess(residentId, requestingUserId, roles);
        return paymentRepository.findByResidentId(residentId, pageable)
                .map(PaymentResponse::from);
    }

    // ── BILL-020: Update Payment Status ──────────────────────

    @Override
    public PaymentResponse updatePaymentStatus(String paymentId,
                                               UpdatePaymentStatusRequest request) {

        log.info("Updating payment status: id={}, newStatus={}",
                paymentId, request.getStatus());

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Cannot change status of already CONFIRMED or REJECTED payment
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BillingException(
                    "Only PENDING payments can be confirmed or rejected. " +
                            "Current status: " + payment.getStatus(),
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_PAYMENT_STATUS_TRANSITION");
        }

        if ("REJECTED".equals(request.getStatus())) {
            // Rejection reason is required
            if (request.getRejectionReason() == null
                    || request.getRejectionReason().isBlank()) {
                throw new BillingException(
                        "A rejection reason is required when rejecting a payment",
                        HttpStatus.BAD_REQUEST,
                        "REJECTION_REASON_REQUIRED");
            }
            payment.setStatus(PaymentStatus.REJECTED);
            payment.setRejectionReason(request.getRejectionReason());
            Payment saved = paymentRepository.save(payment);
            log.info("Payment rejected: id={}", paymentId);
            return PaymentResponse.from(saved);
        }

        // CONFIRMED path
        payment.setStatus(PaymentStatus.CONFIRMED);
        Payment confirmed = paymentRepository.save(payment);

        // Auto-generate receipt for confirmed payment
        receiptService.generateReceipt(confirmed);

        // Recalculate and update invoice status
        recalculateInvoiceStatus(confirmed.getInvoice());

        log.info("Payment confirmed: id={}", paymentId);

        // Reload to include receipt reference
        Payment reloaded = paymentRepository.findById(paymentId)
                .orElse(confirmed);

        return PaymentResponse.from(reloaded);
    }

    // ── Private helpers ───────────────────────────────────────

    /**
     * After a payment is confirmed, recalculate whether the invoice
     * is PARTIALLY_PAID or fully PAID.
     *
     * Formula:
     *   totalConfirmed = SUM of all CONFIRMED payments
     *   if totalConfirmed >= invoice.totalAmount → PAID
     *   else → PARTIALLY_PAID
     */
    private void recalculateInvoiceStatus(Invoice invoice) {
        BigDecimal totalConfirmed = paymentRepository
                .sumConfirmedAmountByInvoiceId(invoice.getId());

        InvoiceStatus newStatus = totalConfirmed.compareTo(invoice.getTotalAmount()) >= 0
                ? InvoiceStatus.PAID
                : InvoiceStatus.PARTIALLY_PAID;

        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);

        log.info("Invoice status recalculated: invoiceId={}, newStatus={}",
                invoice.getId(), newStatus);
    }

    private void enforceUnitAccess(String unitId, String residentId,
                                   String requestingUserId, List<String> roles) {
        boolean isPrivileged = roles.stream().anyMatch(r ->
                r.equals("FINANCE_OFFICER") || r.equals("APARTMENT_MANAGER"));
        if (!isPrivileged) {
            log.debug("Unit access check: requestingUser={}, unitId={}",
                    requestingUserId, unitId);
            // TODO: replace with real unit-ownership check from Group 1
        }
    }

    private void enforceResidentAccess(String residentId, String requestingUserId,
                                       List<String> roles) {
        boolean isPrivileged = roles.stream().anyMatch(r ->
                r.equals("FINANCE_OFFICER") || r.equals("APARTMENT_MANAGER"));
        if (!isPrivileged) {
            log.debug("Resident access check: requestingUser={}, residentId={}",
                    requestingUserId, residentId);
            // TODO: replace with real resident check from Group 1
        }
    }
}