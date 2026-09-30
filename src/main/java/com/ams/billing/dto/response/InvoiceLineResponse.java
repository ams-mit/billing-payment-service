package com.ams.billing.dto.response;

import com.ams.billing.entity.InvoiceLine;
import com.ams.billing.enums.ChargeType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response for a single invoice line item.
 *
 * The amounts here are SNAPSHOTS — permanently recorded at invoice generation time.
 * They will never change even if the parent charge rule is updated.
 * This is the most important invariant in the billing system.
 */
@Getter
@Builder
public class InvoiceLineResponse {

    private String id;
    private String invoiceId;
    private String chargeRuleId;      // reference only — not a live FK
    private String chargeRuleName;    // snapshot
    private ChargeType chargeType;    // snapshot
    private BigDecimal amount;        // snapshot — permanently fixed
    private Instant createdAt;

    public static InvoiceLineResponse from(InvoiceLine line) {
        return InvoiceLineResponse.builder()
                .id(line.getId())
                .invoiceId(line.getInvoice().getId())
                .chargeRuleId(line.getChargeRuleId())
                .chargeRuleName(line.getChargeRuleName())
                .chargeType(line.getChargeType())
                .amount(line.getAmount())
                .createdAt(line.getCreatedAt())
                .build();
    }
}