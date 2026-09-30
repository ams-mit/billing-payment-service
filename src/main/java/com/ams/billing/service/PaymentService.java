package com.ams.billing.service;

import com.ams.billing.dto.request.RecordPaymentRequest;
import com.ams.billing.dto.request.UpdatePaymentStatusRequest;
import com.ams.billing.dto.response.PaymentResponse;
import com.ams.billing.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {

    PaymentResponse recordPayment(RecordPaymentRequest request, String recordedBy);

    Page<PaymentResponse> getAllPayments(String invoiceId, PaymentStatus status,
                                         Pageable pageable);

    PaymentResponse getPaymentById(String paymentId, String requestingUserId,
                                   List<String> roles);

    Page<PaymentResponse> getPaymentsByInvoice(String invoiceId,
                                               String requestingUserId,
                                               List<String> roles,
                                               Pageable pageable);

    Page<PaymentResponse> getPaymentsByUnit(String unitId, String requestingUserId,
                                            List<String> roles, Pageable pageable);

    Page<PaymentResponse> getPaymentsByResident(String residentId,
                                                String requestingUserId,
                                                List<String> roles,
                                                Pageable pageable);

    PaymentResponse updatePaymentStatus(String paymentId,
                                        UpdatePaymentStatusRequest request);
}