package com.ams.billing.service;

import com.ams.billing.dto.response.ReceiptResponse;
import com.ams.billing.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReceiptService {

    // Called internally by PaymentServiceImpl on CONFIRMED status
    ReceiptResponse generateReceipt(Payment payment);

    ReceiptResponse getReceiptById(String receiptId);

    ReceiptResponse getReceiptByPaymentId(String paymentId);

    Page<ReceiptResponse> getReceiptsByUnit(String unitId, Pageable pageable);
}