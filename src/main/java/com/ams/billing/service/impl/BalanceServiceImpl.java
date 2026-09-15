package com.ams.billing.service.impl;

import com.ams.billing.dto.response.BalanceResponse;
import com.ams.billing.entity.Invoice;
import com.ams.billing.enums.AdjustmentType;
import com.ams.billing.enums.InvoiceStatus;
import com.ams.billing.repository.AdjustmentRepository;
import com.ams.billing.repository.InvoiceRepository;
import com.ams.billing.repository.PaymentRepository;
import com.ams.billing.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Updated in Part 4 to include adjustments in the balance formula.
 *
 * Full formula per v2.1 Section 3.3:
 *   outstandingBalance =
 *     SUM(ISSUED + PARTIALLY_PAID + OVERDUE invoice amounts)
 *     - SUM(CONFIRMED payment amounts)
 *     - SUM(CREDIT adjustment amounts)
 *     + SUM(DEBIT adjustment amounts)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceServiceImpl implements BalanceService {

    private final InvoiceRepository    invoiceRepository;
    private final PaymentRepository    paymentRepository;
    private final AdjustmentRepository adjustmentRepository;

    @Override
    public BalanceResponse getBalanceByUnit(String unitId) {

        log.debug("Calculating live balance for unitId={}", unitId);

        List<Invoice> unpaidInvoices = invoiceRepository.findByUnitIdAndStatusIn(
                unitId,
                List.of(InvoiceStatus.ISSUED,
                        InvoiceStatus.PARTIALLY_PAID,
                        InvoiceStatus.OVERDUE)
        );

        // Step 1: Total invoiced (unpaid only)
        BigDecimal totalInvoiced = unpaidInvoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 2: Total confirmed payments
        BigDecimal totalConfirmed = paymentRepository
                .sumConfirmedAmountByUnitId(unitId);

        // Step 3: Total credit adjustments (reduce balance)
        BigDecimal totalCredits = adjustmentRepository
                .sumByUnitIdAndType(unitId, AdjustmentType.CREDIT);

        // Step 4: Total debit adjustments (increase balance)
        BigDecimal totalDebits = adjustmentRepository
                .sumByUnitIdAndType(unitId, AdjustmentType.DEBIT);

        // Step 5: Full formula
        BigDecimal outstanding = totalInvoiced
                .subtract(totalConfirmed)
                .subtract(totalCredits)
                .add(totalDebits);

        long overdueCount = unpaidInvoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.OVERDUE)
                .count();

        Instant lastInvoiceDate = unpaidInvoices.stream()
                .map(Invoice::getIssuedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return BalanceResponse.builder()
                .unitId(unitId)
                .outstandingBalance(outstanding.max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP))
                .overdueCount((int) overdueCount)
                .hasOverdueInvoices(overdueCount > 0)
                .lastInvoiceDate(lastInvoiceDate)
                .lastPaymentDate(null)
                .build();
    }

    @Override
    public BalanceResponse getBalanceByResident(String residentId) {

        List<Invoice> residentInvoices = invoiceRepository.findAll().stream()
                .filter(inv -> residentId.equals(inv.getResidentId()))
                .filter(inv -> inv.getStatus() == InvoiceStatus.ISSUED
                        || inv.getStatus() == InvoiceStatus.PARTIALLY_PAID
                        || inv.getStatus() == InvoiceStatus.OVERDUE)
                .toList();

        BigDecimal totalInvoiced = residentInvoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = residentInvoices.stream()
                .map(inv -> paymentRepository
                        .sumConfirmedAmountByInvoiceId(inv.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = residentInvoices.stream()
                .map(inv -> adjustmentRepository
                        .sumByInvoiceIdAndType(inv.getId(), AdjustmentType.CREDIT))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = residentInvoices.stream()
                .map(inv -> adjustmentRepository
                        .sumByInvoiceIdAndType(inv.getId(), AdjustmentType.DEBIT))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = totalInvoiced
                .subtract(totalPaid)
                .subtract(totalCredits)
                .add(totalDebits);

        long overdueCount = residentInvoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.OVERDUE)
                .count();

        return BalanceResponse.builder()
                .unitId(residentInvoices.isEmpty()
                        ? null : residentInvoices.get(0).getUnitId())
                .outstandingBalance(outstanding.max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP))
                .overdueCount((int) overdueCount)
                .hasOverdueInvoices(overdueCount > 0)
                .lastInvoiceDate(null)
                .lastPaymentDate(null)
                .build();
    }
}
