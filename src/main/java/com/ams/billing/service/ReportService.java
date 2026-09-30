package com.ams.billing.service;

import com.ams.billing.dto.response.ArrearsReportResponse;
import com.ams.billing.dto.response.CollectionSummaryResponse;
import com.ams.billing.dto.response.FinanceDashboardResponse;
import com.ams.billing.dto.response.PaymentHistoryResponse;
import com.ams.billing.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReportService {

    FinanceDashboardResponse getFinanceDashboard();

    Page<ArrearsReportResponse> getArrearsReport(String buildingId,
                                                 Integer year,
                                                 Integer month,
                                                 Pageable pageable);

    List<CollectionSummaryResponse> getCollectionSummary(int year);

    Page<PaymentHistoryResponse> getPaymentHistory(Integer year,
                                                   Integer month,
                                                   PaymentMethod paymentMethod,
                                                   Pageable pageable);
}