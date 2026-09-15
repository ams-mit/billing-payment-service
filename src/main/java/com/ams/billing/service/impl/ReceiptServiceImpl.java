package com.ams.billing.service.impl;

import com.ams.billing.dto.response.ReceiptResponse;
import com.ams.billing.entity.Invoice;
import com.ams.billing.entity.Payment;
import com.ams.billing.entity.Receipt;
import com.ams.billing.exception.ReceiptNotFoundException;
import com.ams.billing.repository.ReceiptRepository;
import com.ams.billing.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates and retrieves receipts.
 *
 * Receipts are IMMUTABLE — all fields use updatable=false in the entity.
 * Once generated, a receipt is a permanent proof-of-payment document.
 * No update or delete endpoints exist for receipts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;

    // ── Auto-generate on payment confirmation ─────────────────

    @Override
    public ReceiptResponse generateReceipt(Payment payment) {

        Invoice invoice = payment.getInvoice();

        // Snapshot all values at the moment of receipt generation.
        // These values are permanent — receipt entity has updatable=false on all fields.
        Receipt receipt = Receipt.builder()
                .payment(payment)
                .unitId(invoice.getUnitId())
                .billingPeriod(invoice.getBillingPeriod())
                .billingYear(invoice.getBillingYear())
                .billingMonth(invoice.getBillingMonth())
                .amountPaid(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .build();

        Receipt saved = receiptRepository.save(receipt);

        log.info("Receipt generated: id={}, paymentId={}, unitId={}",
                saved.getId(), payment.getId(), invoice.getUnitId());

        return ReceiptResponse.from(saved);
    }

    // ── BILL-021: Get Receipt by ID ───────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptById(String receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ReceiptNotFoundException(
                        "Receipt not found: " + receiptId));
        return ReceiptResponse.from(receipt);
    }

    // ── BILL-022: Get Receipt by Payment ID ──────────────────

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByPaymentId(String paymentId) {
        // Returns 404 if payment was rejected or still PENDING
        Receipt receipt = receiptRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ReceiptNotFoundException(
                        "No receipt found for payment " + paymentId +
                                ". Receipt is only generated for CONFIRMED payments."));
        return ReceiptResponse.from(receipt);
    }

    // ── BILL-023: Get Receipts by Unit ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReceiptResponse> getReceiptsByUnit(String unitId, Pageable pageable) {
        return receiptRepository.findByUnitId(unitId, pageable)
                .map(ReceiptResponse::from);
    }
}