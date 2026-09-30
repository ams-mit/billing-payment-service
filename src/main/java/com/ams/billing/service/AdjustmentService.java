package com.ams.billing.service;

import com.ams.billing.dto.request.CreateAdjustmentRequest;
import com.ams.billing.dto.response.AdjustmentResponse;

import java.util.List;

public interface AdjustmentService {

    AdjustmentResponse createAdjustment(CreateAdjustmentRequest request,
                                        String createdBy);

    List<AdjustmentResponse> getAdjustmentsByInvoice(String invoiceId);
}