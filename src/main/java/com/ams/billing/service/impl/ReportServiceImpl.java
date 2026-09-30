package com.ams.billing.service.impl;

import com.ams.billing.dto.response.ArrearsReportResponse;
import com.ams.billing.dto.response.CollectionSummaryResponse;
import com.ams.billing.dto.response.FinanceDashboardResponse;
import com.ams.billing.dto.response.PaymentHistoryResponse;
import com.ams.billing.entity.Invoice;
import com.ams.billing.entity.Payment;
import com.ams.billing.enums.InvoiceStatus;
import com.ams.billing.enums.PaymentMethod;
import com.ams.billing.enums.PaymentStatus;
import com.ams.billing.repository.InvoiceRepository;
import com.ams.billing.repository.PaymentRepository;
import com.ams.billing.service.BalanceService;
import com.ams.billing.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final BalanceService    balanceService;

    // ── BILL-029: Finance Dashboard ───────────────────────────

    @Override
    public FinanceDashboardResponse getFinanceDashboard() {

        log.debug("Generating finance dashboard report");

        LocalDate now = LocalDate.now();
        int currentYear  = now.getYear();
        int currentMonth = now.getMonthValue();

        // All invoices issued this month
        List<Invoice> thisMonthInvoices = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getBillingYear() != null && inv.getBillingYear().intValue() == currentYear
                        && inv.getBillingMonth() != null
                        && inv.getBillingMonth().intValue() == currentMonth)
                .toList();

        BigDecimal totalInvoicedThisMonth = thisMonthInvoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // All payments confirmed this month
        List<Payment> thisMonthPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.CONFIRMED
                        && p.getPaymentDate().getYear() == currentYear
                        && p.getPaymentDate().getMonthValue() == currentMonth)
                .toList();

        BigDecimal totalCollectedThisMonth = thisMonthPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total outstanding across ALL units (all time)
        BigDecimal totalOutstanding = invoiceRepository
                .findAll().stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.ISSUED
                        || inv.getStatus() == InvoiceStatus.PARTIALLY_PAID
                        || inv.getStatus() == InvoiceStatus.OVERDUE)
                .map(Invoice::getUnitId)
                .distinct()
                .map(unitId -> balanceService.getBalanceByUnit(unitId)
                        .getOutstandingBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Count units with OVERDUE invoices
        long overdueAccountsCount = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.OVERDUE)
                .map(Invoice::getUnitId)
                .distinct()
                .count();

        // Collection rate = collected / invoiced * 100
        double collectionRate = totalInvoicedThisMonth.compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : totalCollectedThisMonth
                .divide(totalInvoicedThisMonth, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        long paidThisMonth = thisMonthInvoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .count();

        String reportMonth = YearMonth.of(currentYear, currentMonth).toString();

        return FinanceDashboardResponse.builder()
                .totalInvoicedThisMonth(totalInvoicedThisMonth
                        .setScale(2, RoundingMode.HALF_UP))
                .totalCollectedThisMonth(totalCollectedThisMonth
                        .setScale(2, RoundingMode.HALF_UP))
                .totalOutstanding(totalOutstanding
                        .setScale(2, RoundingMode.HALF_UP))
                .overdueAccountsCount((int) overdueAccountsCount)
                .collectionRatePercent(Math.round(collectionRate * 100.0) / 100.0)
                .totalActiveInvoices(thisMonthInvoices.size())
                .totalPaidInvoicesThisMonth((int) paidThisMonth)
                .reportMonth(reportMonth)
                .build();
    }

    // ── BILL-030: Arrears Report ──────────────────────────────

    @Override
    public Page<ArrearsReportResponse> getArrearsReport(String buildingId,
                                                        Integer year,
                                                        Integer month,
                                                        Pageable pageable) {

        log.debug("Generating arrears report: year={}, month={}", year, month);

        // Load all unpaid/overdue invoices
        List<Invoice> unpaidInvoices = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.ISSUED
                        || inv.getStatus() == InvoiceStatus.PARTIALLY_PAID
                        || inv.getStatus() == InvoiceStatus.OVERDUE)
                .filter(inv -> year == null || (inv.getBillingYear() != null && inv.getBillingYear().intValue() == year.intValue()))
                .filter(inv -> month == null
                        || (inv.getBillingMonth() != null
                        && inv.getBillingMonth().intValue() == month.intValue()))
                .toList();

        // Group by unitId
        Map<String, List<Invoice>> byUnit = unpaidInvoices.stream()
                .collect(Collectors.groupingBy(Invoice::getUnitId));

        List<ArrearsReportResponse> rows = byUnit.entrySet().stream()
                .map(entry -> {
                    String unitId = entry.getKey();
                    List<Invoice> unitInvoices = entry.getValue();

                    BigDecimal outstanding = balanceService
                            .getBalanceByUnit(unitId).getOutstandingBalance();

                    long overdueCount = unitInvoices.stream()
                            .filter(inv -> inv.getStatus() == InvoiceStatus.OVERDUE)
                            .count();

                    Invoice oldest = unitInvoices.stream()
                            .min(Comparator.comparing(Invoice::getIssuedAt))
                            .orElse(null);

                    String residentId = unitInvoices.get(0).getResidentId();

                    return ArrearsReportResponse.builder()
                            .unitId(unitId)
                            .residentId(residentId)
                            .outstandingBalance(outstanding)
                            .unpaidInvoiceCount(unitInvoices.size())
                            .overdueInvoiceCount((int) overdueCount)
                            .oldestUnpaidInvoiceDate(
                                    oldest != null ? oldest.getIssuedAt() : null)
                            .lastPaymentDate(null)
                            .build();
                })
                .filter(row -> row.getOutstandingBalance()
                        .compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(ArrearsReportResponse::getOutstandingBalance)
                        .reversed())
                .toList();

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), rows.size());
        List<ArrearsReportResponse> pageContent =
                start >= rows.size() ? List.of() : rows.subList(start, end);

        return new PageImpl<>(pageContent, pageable, rows.size());
    }

    // ── BILL-031: Collection Summary ─────────────────────────

    @Override
    public List<CollectionSummaryResponse> getCollectionSummary(int year) {

        log.debug("Generating collection summary for year={}", year);

        List<CollectionSummaryResponse> summary = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            final int m = month;

            // Invoices issued in this month
            List<Invoice> monthInvoices = invoiceRepository.findAll().stream()
                    .filter(inv -> inv.getBillingYear() != null && inv.getBillingYear().intValue() == year
                            && inv.getBillingMonth() != null
                            && inv.getBillingMonth().intValue() == m)
                    .toList();

            BigDecimal totalBilled = monthInvoices.stream()
                    .map(Invoice::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Payments confirmed in this month
            List<Payment> monthPayments = paymentRepository.findAll().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.CONFIRMED
                            && p.getPaymentDate().getYear() == year
                            && p.getPaymentDate().getMonthValue() == m)
                    .toList();

            BigDecimal totalCollected = monthPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal outstanding = totalBilled.subtract(totalCollected);

            double rate = totalBilled.compareTo(BigDecimal.ZERO) == 0
                    ? 0.0
                    : totalCollected
                    .divide(totalBilled, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            String monthLabel = Month.of(month)
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " " + year;

            summary.add(CollectionSummaryResponse.builder()
                    .year(year)
                    .month(month)
                    .monthLabel(monthLabel)
                    .totalBilled(totalBilled.setScale(2, RoundingMode.HALF_UP))
                    .totalCollected(totalCollected.setScale(2, RoundingMode.HALF_UP))
                    .totalOutstanding(outstanding.setScale(2, RoundingMode.HALF_UP))
                    .collectionRatePercent(
                            Math.round(rate * 100.0) / 100.0)
                    .invoiceCount(monthInvoices.size())
                    .paymentCount(monthPayments.size())
                    .build());
        }

        return summary;
    }

    // ── BILL-032: Payment History ─────────────────────────────

    @Override
    public Page<PaymentHistoryResponse> getPaymentHistory(Integer year,
                                                          Integer month,
                                                          PaymentMethod paymentMethod,
                                                          Pageable pageable) {

        log.debug("Generating payment history: year={}, month={}, method={}",
                year, month, paymentMethod);

        List<PaymentHistoryResponse> rows = paymentRepository.findAll().stream()
                .filter(p -> year == null
                        || p.getPaymentDate().getYear() == year)
                .filter(p -> month == null
                        || p.getPaymentDate().getMonthValue() == month)
                .filter(p -> paymentMethod == null
                        || p.getPaymentMethod() == paymentMethod)
                .sorted(Comparator.comparing(Payment::getRecordedAt).reversed())
                .map(p -> {
                    String receiptId = p.getReceipt() != null
                            ? p.getReceipt().getId() : null;
                    return PaymentHistoryResponse.builder()
                            .paymentId(p.getId())
                            .invoiceId(p.getInvoice().getId())
                            .unitId(p.getInvoice().getUnitId())
                            .residentId(p.getInvoice().getResidentId())
                            .amount(p.getAmount())
                            .paymentDate(p.getPaymentDate())
                            .paymentMethod(p.getPaymentMethod())
                            .referenceNumber(p.getReferenceNumber())
                            .status(p.getStatus())
                            .recordedBy(p.getRecordedBy())
                            .recordedAt(p.getRecordedAt())
                            .receiptId(receiptId)
                            .build();
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), rows.size());
        List<PaymentHistoryResponse> pageContent =
                start >= rows.size() ? List.of() : rows.subList(start, end);

        return new PageImpl<>(pageContent, pageable, rows.size());
    }
}