package com.ams.billing.controller;

import com.ams.billing.dto.response.ApiResponse;
import com.ams.billing.dto.response.BalanceResponse;
import com.ams.billing.service.BalanceService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints — service-to-service only.
 *
 * @Hidden — excluded from Swagger UI entirely.
 *           Frontend users must never know these endpoints exist.
 *
 * These endpoints are NOT registered in the API Gateway's public routing.
 * They use internal routing only.
 *
 * Auth: Gateway Service JWT required.
 *   type claim must be "service" — enforced by hasRole('SERVICE').
 *   The calling service's sub must be authorized for this endpoint.
 *
 * Called by: Group 4 community-service (BILL-028)
 */
@Hidden
@Slf4j
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalController {

    private final BalanceService balanceService;

    // BILL-028
    /**
     * Called by Group 4 community-service before approving a facility booking.
     * Returns outstanding balance and overdue status for a unit.
     *
     * Auth: Gateway Service JWT — type = service.
     * hasRole('SERVICE') is set by JwtAuthenticationFilter
     * when it detects type = service in the Gateway JWT.
     */
    @GetMapping("/balance/{unitId}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<ApiResponse<BalanceResponse>> getInternalBalance(
            @PathVariable String unitId,
            HttpServletRequest httpRequest) {

        log.info("Internal balance check: unitId={}, calledBy={}",
                unitId, httpRequest.getHeader("Authorization") != null
                        ? "service-jwt" : "no-auth");

        BalanceResponse response = balanceService.getBalanceByUnit(unitId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Internal balance retrieved",
                response,
                httpRequest.getHeader("X-Request-ID")));
    }
}