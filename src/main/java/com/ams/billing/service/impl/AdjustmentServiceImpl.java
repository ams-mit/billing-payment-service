package com.ams.billing.service.impl;

import com.ams.billing.dto.request.CreateAdjustmentRequest;
import com.ams.billing.dto.response.AdjustmentResponse;
import com.ams.billing.entity.Adjustment;
import com.ams.billing.entity.Invoice;
import com.ams.billing.enums.InvoiceStatus;
import com.ams.billing.exception.BillingException;
import com.ams.billing.exception.InvoiceNotFoundException;
import com.ams.billing.repository.AdjustmentRepository;
import com.ams.billing.repository.InvoiceRepository;
import com.ams.billing.service.AdjustmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdjustmentServiceImpl implements AdjustmentService {

    private final AdjustmentRepository adjustmentRepository;
    private final InvoiceRepository    invoiceRepository;

    // ── BILL-024: Create Adjustment ───────────────────────────

    @Override
    public AdjustmentResponse createAdjustment(CreateAdjustmentRequest request,
                                               String createdBy) {

        log.info("Creating adjustment: invoiceId={}, type={}, amount={}, createdBy={}",
                request.getInvoiceId(), request.getAdjustmentType(),
                request.getAmount(), createdBy);

        // Load invoice — 404 if not found
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(
                        request.getInvoiceId()));

        // Cannot adjust a cancelled invoice
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BillingException(
                    "Cannot apply an adjustment to a cancelled invoice",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVOICE_CANCELLED");
        }

        // Build adjustment — all fields immutable after creation
        Adjustment adjustment = Adjustment.builder()
                .invoice(invoice)
                .adjustmentType(request.getAdjustmentType())
                .amount(request.getAmount())
                .reason(request.getReason())
                .createdBy(createdBy)
                .build();

        Adjustment saved = adjustmentRepository.save(adjustment);

        log.info("Adjustment created: id={}, type={}, amount={}",
                saved.getId(), saved.getAdjustmentType(), saved.getAmount());

        return AdjustmentResponse.from(saved);
    }

    // ── BILL-025: Get Adjustments for Invoice ─────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdjustmentResponse> getAdjustmentsByInvoice(String invoiceId) {

        // Verify invoice exists first
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new InvoiceNotFoundException(invoiceId);
        }

        return adjustmentRepository
                .findByInvoiceIdOrderByCreatedAtDesc(invoiceId)
                .stream()
                .map(AdjustmentResponse::from)
                .toList();
    }
}