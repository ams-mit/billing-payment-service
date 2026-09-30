package com.ams.billing.service;

import com.ams.billing.dto.request.CreateChargeRuleRequest;
import com.ams.billing.dto.request.UpdateChargeRuleRequest;
import com.ams.billing.dto.request.UpdateStatusRequest;
import com.ams.billing.dto.response.ChargeRuleResponse;
import com.ams.billing.enums.ChargeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Declares what operations are available for charge rules.
 * The controller depends on this interface, not the implementation.
 * This is called "programming to an interface" — a core Spring principle.
 */
public interface ChargeRuleService {

    ChargeRuleResponse createChargeRule(CreateChargeRuleRequest request, String createdBy);

    Page<ChargeRuleResponse> getAllChargeRules(String status, ChargeType chargeType, Pageable pageable);

    ChargeRuleResponse getChargeRuleById(String chargeRuleId);

    ChargeRuleResponse updateChargeRule(String chargeRuleId, UpdateChargeRuleRequest request);

    ChargeRuleResponse updateChargeRuleStatus(String chargeRuleId, UpdateStatusRequest request);

    List<ChargeRuleResponse> getChargeRulesByType(ChargeType chargeType);
}