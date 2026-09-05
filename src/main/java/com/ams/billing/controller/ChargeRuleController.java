package com.ams.billing.controller;

import com.ams.billing.dto.request.CreateChargeRuleRequest;
import com.ams.billing.dto.request.UpdateChargeRuleRequest;
import com.ams.billing.dto.request.UpdateStatusRequest;
import com.ams.billing.dto.response.ApiResponse;
import com.ams.billing.dto.response.ChargeRuleResponse;
import com.ams.billing.enums.ChargeType;
import com.ams.billing.service.ChargeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @RestController  — combines @Controller + @ResponseBody.
 *   Every method return value is automatically serialized to JSON.
 * @RequestMapping  — all endpoints in this class start with /api/v1/charge-rules
 * @SecurityRequirement — tells Swagger UI to show the lock icon on these endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/charge-rules")
@RequiredArgsConstructor
@Tag(name = "Charge Rules", description = "Manage recurring charge rule definitions")
@SecurityRequirement(name = "BearerAuth")
public class ChargeRuleController {

    private final ChargeRuleService chargeRuleService;

    // ─────────────────────────────────────────────────────────────
    // POST /api/v1/charge-rules
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Create a new charge rule",
            description = "Creates a recurring charge rule. Finance Officer only.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Charge rule created")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate rule name")
    @PostMapping
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<ChargeRuleResponse>> createChargeRule(
            @Valid @RequestBody CreateChargeRuleRequest request,
            @AuthenticationPrincipal String userId,  // extracted from JWT by our filter
            HttpServletRequest httpRequest) {

        ChargeRuleResponse response = chargeRuleService.createChargeRule(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Charge rule created successfully",
                        response,
                        httpRequest.getHeader("X-Request-ID")));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/v1/charge-rules
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get all charge rules",
            description = "Returns paginated charge rules. Filterable by status and chargeType.")
    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<ChargeRuleResponse>>> getAllChargeRules(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) ChargeType chargeType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChargeRuleResponse> result = chargeRuleService.getAllChargeRules(status, chargeType, pageable);

        return ResponseEntity.ok(ApiResponse.ok(
                "Charge rules retrieved successfully",
                result,
                httpRequest.getHeader("X-Request-ID")));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/v1/charge-rules/{chargeRuleId}
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get a charge rule by ID")
    @GetMapping("/{chargeRuleId}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'MANAGER')")
    public ResponseEntity<ApiResponse<ChargeRuleResponse>> getChargeRuleById(
            @PathVariable String chargeRuleId,
            HttpServletRequest httpRequest) {

        ChargeRuleResponse response = chargeRuleService.getChargeRuleById(chargeRuleId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Charge rule retrieved successfully",
                response,
                httpRequest.getHeader("X-Request-ID")));
    }

    // ─────────────────────────────────────────────────────────────
    // PUT /api/v1/charge-rules/{chargeRuleId}
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Update a charge rule",
            description = "Updates name, amount, and billing period. ChargeType cannot be changed.")
    @PutMapping("/{chargeRuleId}")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<ChargeRuleResponse>> updateChargeRule(
            @PathVariable String chargeRuleId,
            @Valid @RequestBody UpdateChargeRuleRequest request,
            HttpServletRequest httpRequest) {

        ChargeRuleResponse response = chargeRuleService.updateChargeRule(chargeRuleId, request);

        return ResponseEntity.ok(ApiResponse.ok(
                "Charge rule updated successfully",
                response,
                httpRequest.getHeader("X-Request-ID")));
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/v1/charge-rules/{chargeRuleId}/status
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Activate or deactivate a charge rule",
            description = "Deactivated rules are kept for audit. They are never deleted.")
    @PatchMapping("/{chargeRuleId}/status")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<ChargeRuleResponse>> updateChargeRuleStatus(
            @PathVariable String chargeRuleId,
            @Valid @RequestBody UpdateStatusRequest request,
            HttpServletRequest httpRequest) {

        ChargeRuleResponse response = chargeRuleService.updateChargeRuleStatus(chargeRuleId, request);

        return ResponseEntity.ok(ApiResponse.ok(
                "Charge rule status updated successfully",
                response,
                httpRequest.getHeader("X-Request-ID")));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/v1/charge-rules/type/{chargeType}
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "Get all active charge rules by type",
            description = "Returns all ACTIVE rules for a given charge type.")
    @GetMapping("/type/{chargeType}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ChargeRuleResponse>>> getChargeRulesByType(
            @PathVariable ChargeType chargeType,
            HttpServletRequest httpRequest) {

        List<ChargeRuleResponse> response = chargeRuleService.getChargeRulesByType(chargeType);

        return ResponseEntity.ok(ApiResponse.ok(
                "Charge rules retrieved successfully",
                response,
                httpRequest.getHeader("X-Request-ID")));
    }
}