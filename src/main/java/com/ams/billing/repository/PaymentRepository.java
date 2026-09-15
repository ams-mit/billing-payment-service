package com.ams.billing.repository;

import com.ams.billing.entity.Payment;
import com.ams.billing.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    // BILL-017: GET /payments/invoices/{invoiceId}
    Page<Payment> findByInvoiceId(String invoiceId, Pageable pageable);

    // BILL-018: GET /payments/units/{unitId}
    // Joins through invoice to get payments for a unit
    @Query("""
            SELECT p FROM Payment p
            WHERE p.invoice.unitId = :unitId
            ORDER BY p.recordedAt DESC
            """)
    Page<Payment> findByUnitId(@Param("unitId") String unitId, Pageable pageable);

    // BILL-019: GET /payments/residents/{residentId}
    @Query("""
            SELECT p FROM Payment p
            WHERE p.invoice.residentId = :residentId
            ORDER BY p.recordedAt DESC
            """)
    Page<Payment> findByResidentId(
            @Param("residentId") String residentId, Pageable pageable);

    // BILL-015: GET /payments with filters
    @Query("""
            SELECT p FROM Payment p
            WHERE (:invoiceId IS NULL OR p.invoice.id = :invoiceId)
            AND (:status IS NULL OR p.status = :status)
            ORDER BY p.recordedAt DESC
            """)
    Page<Payment> findWithFilters(
            @Param("invoiceId") String invoiceId,
            @Param("status") PaymentStatus status,
            Pageable pageable
    );

    // Used by BalanceService — sum of all CONFIRMED payments for an invoice
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.invoice.id = :invoiceId
            AND p.status = 'CONFIRMED'
            """)
    BigDecimal sumConfirmedAmountByInvoiceId(@Param("invoiceId") String invoiceId);

    // Used by BalanceService — sum of all CONFIRMED payments for a unit
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.invoice.unitId = :unitId
            AND p.status = 'CONFIRMED'
            """)
    BigDecimal sumConfirmedAmountByUnitId(@Param("unitId") String unitId);

    // Used to check if any payments exist before allowing invoice cancellation
    List<Payment> findByInvoiceIdAndStatus(String invoiceId, PaymentStatus status);
}