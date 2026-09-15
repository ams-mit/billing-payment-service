package com.ams.billing.dto.response;

import com.ams.billing.entity.Adjustment;
import com.ams.billing.enums.AdjustmentType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class AdjustmentResponse {

    private String id;
    private String invoiceId;
    private AdjustmentType adjustmentType;
    private BigDecimal amount;
    private String reason;
    private String createdBy;
    private Instant createdAt;

    public static AdjustmentResponse from(Adjustment adjustment) {
        return AdjustmentResponse.builder()
                .id(adjustment.getId())
                .invoiceId(adjustment.getInvoice().getId())
                .adjustmentType(adjustment.getAdjustmentType())
                .amount(adjustment.getAmount())
                .reason(adjustment.getReason())
                .createdBy(adjustment.getCreatedBy())
                .createdAt(adjustment.getCreatedAt())
                .build();
    }
}