package com.ams.billing.service.impl;

import com.ams.billing.exception.BillingException;
import org.springframework.http.HttpStatus;
import com.ams.billing.dto.request.CreateChargeRuleRequest;
import com.ams.billing.dto.request.UpdateChargeRuleRequest;
import com.ams.billing.dto.request.UpdateStatusRequest;
import com.ams.billing.dto.response.ChargeRuleResponse;
import com.ams.billing.entity.ChargeRule;
import com.ams.billing.enums.ChargeType;
import com.ams.billing.exception.ChargeRuleNotFoundException;
import com.ams.billing.exception.DuplicateChargeRuleException;
import com.ams.billing.repository.ChargeRuleRepository;
import com.ams.billing.service.ChargeRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Service  — tells Spring this is a service bean (Spring manages its lifecycle)
 * @Transactional — wraps each method in a database transaction automatically.
 *   If anything throws an exception mid-method, the whole operation rolls back.
 *   Nothing is half-saved to the database.
 * @RequiredArgsConstructor — Lombok generates a constructor that injects
 *   ChargeRuleRepository automatically (constructor injection — best practice).
 * @Slf4j — gives you a `log` variable for logging without boilerplate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChargeRuleServiceImpl implements ChargeRuleService {

    private final ChargeRuleRepository chargeRuleRepository;

    // ─────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────

    @Override
    public ChargeRuleResponse createChargeRule(CreateChargeRuleRequest request, String createdBy) {

        log.info("Creating charge rule: name={}, type={}, createdBy={}",
                request.getName(), request.getChargeType(), createdBy);

        // Business rule: no duplicate active rule with the same name
        chargeRuleRepository.findByNameAndStatus(request.getName(), "ACTIVE")
                .ifPresent(existing -> {
                    throw new DuplicateChargeRuleException(request.getName());
                });

        // Build entity from request
        // .builder() is Lombok's builder pattern — cleaner than setting each field manually
        ChargeRule chargeRule = ChargeRule.builder()
                .name(request.getName())
                .chargeType(request.getChargeType())
                .amount(request.getAmount())
                .billingPeriod(request.getBillingPeriod())
                .applicableToAllUnits(request.isApplicableToAllUnits())
                .status("ACTIVE")
                .createdBy(createdBy)
                .build();

        // save() does the INSERT — returns the saved entity with generated id and timestamps
        ChargeRule saved = chargeRuleRepository.save(chargeRule);

        log.info("Charge rule created: id={}", saved.getId());

        // Convert entity → response DTO and return
        return ChargeRuleResponse.from(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // READ ALL (with optional filters)
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)  // readOnly = true — slight performance boost for SELECT operations
    public Page<ChargeRuleResponse> getAllChargeRules(String status, ChargeType chargeType, Pageable pageable) {

        log.debug("Fetching charge rules: status={}, chargeType={}", status, chargeType);

        // findWithFilters handles nulls — if status is null, it returns all statuses
        Page<ChargeRule> page = chargeRuleRepository.findWithFilters(status, chargeType, pageable);

        // .map() converts each ChargeRule entity in the page to a ChargeRuleResponse
        return page.map(ChargeRuleResponse::from);
    }

    // ─────────────────────────────────────────────────────────────
    // READ ONE
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ChargeRuleResponse getChargeRuleById(String chargeRuleId) {

        // findById returns Optional<ChargeRule>
        // orElseThrow: if empty, throw exception → GlobalExceptionHandler returns 404
        ChargeRule chargeRule = chargeRuleRepository.findById(chargeRuleId)
                .orElseThrow(() -> new ChargeRuleNotFoundException(chargeRuleId));

        return ChargeRuleResponse.from(chargeRule);
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────

    @Override
    public ChargeRuleResponse updateChargeRule(String chargeRuleId, UpdateChargeRuleRequest request) {

        log.info("Updating charge rule: id={}", chargeRuleId);

        ChargeRule chargeRule = chargeRuleRepository.findById(chargeRuleId)
                .orElseThrow(() -> new ChargeRuleNotFoundException(chargeRuleId));

        // Business rule: cannot update an inactive rule
        if ("INACTIVE".equals(chargeRule.getStatus())) {
            throw new BillingException(
                    "Cannot update an inactive charge rule. Activate it first.",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "CHARGE_RULE_INACTIVE"
            );
        }

        // Check duplicate name — only if name is being changed
        if (!chargeRule.getName().equals(request.getName())) {
            chargeRuleRepository.findByNameAndStatus(request.getName(), "ACTIVE")
                    .ifPresent(existing -> {
                        throw new DuplicateChargeRuleException(request.getName());
                    });
        }

        // Update fields on the existing entity
        // No need to call save() explicitly here — because this method is @Transactional,
        // Hibernate detects the changes and runs UPDATE automatically at end of transaction.
        // This is called "dirty checking".
        chargeRule.setName(request.getName());
        chargeRule.setAmount(request.getAmount());
        chargeRule.setBillingPeriod(request.getBillingPeriod());
        chargeRule.setApplicableToAllUnits(request.isApplicableToAllUnits());

        // calling save() explicitly here too — makes the code more readable and intentional
        ChargeRule updated = chargeRuleRepository.save(chargeRule);

        log.info("Charge rule updated: id={}", updated.getId());

        return ChargeRuleResponse.from(updated);
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE STATUS (activate / deactivate)
    // ─────────────────────────────────────────────────────────────

    @Override
    public ChargeRuleResponse updateChargeRuleStatus(String chargeRuleId, UpdateStatusRequest request) {

        log.info("Updating charge rule status: id={}, newStatus={}", chargeRuleId, request.getStatus());

        ChargeRule chargeRule = chargeRuleRepository.findById(chargeRuleId)
                .orElseThrow(() -> new ChargeRuleNotFoundException(chargeRuleId));

        // Only ACTIVE and INACTIVE are valid for charge rules
        if (!request.getStatus().equals("ACTIVE") && !request.getStatus().equals("INACTIVE")) {
            throw new BillingException(
                    "Charge rule status must be ACTIVE or INACTIVE",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS"
            );
        }

        chargeRule.setStatus(request.getStatus());
        ChargeRule updated = chargeRuleRepository.save(chargeRule);

        return ChargeRuleResponse.from(updated);
    }

    // ─────────────────────────────────────────────────────────────
    // READ BY TYPE
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ChargeRuleResponse> getChargeRulesByType(ChargeType chargeType) {

        log.debug("Fetching charge rules by type: {}", chargeType);

        return chargeRuleRepository
                .findByChargeTypeAndStatus(chargeType, "ACTIVE")
                .stream()
                .map(ChargeRuleResponse::from)
                .toList();
    }
}