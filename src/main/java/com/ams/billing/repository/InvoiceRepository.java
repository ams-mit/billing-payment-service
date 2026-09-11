package com.ams.billing.repository;

import com.ams.billing.entity.Invoice;
import com.ams.billing.enums.BillingPeriod;
import com.ams.billing.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    // Duplicate invoice check — called before generating a new invoice
    // An invoice is duplicate if same unitId + year + month + period exists
    // and it is NOT cancelled
    @Query("""
            SELECT COUNT(i) > 0 FROM Invoice i
            WHERE i.unitId = :unitId
            AND i.billingYear = :year
            AND (:month IS NULL OR i.billingMonth = :month)
            AND i.billingPeriod = :period
            AND i.status <> 'CANCELLED'
            """)
    boolean existsActiveInvoiceForPeriod(
            @Param("unitId") String unitId,
            @Param("year") Short year,
            @Param("month") Byte month,
            @Param("period") BillingPeriod period
    );

    // BILL-010: GET /invoices/units/{unitId}
    Page<Invoice> findByUnitId(String unitId, Pageable pageable);

    // BILL-011: GET /invoices/units/{unitId}/period/{year}/{month}
    Optional<Invoice> findByUnitIdAndBillingYearAndBillingMonth(
            String unitId, Short billingYear, Byte billingMonth);

    // BILL-008: GET /invoices with filters
    @Query("""
            SELECT i FROM Invoice i
            WHERE (:unitId IS NULL OR i.unitId = :unitId)
            AND (:status IS NULL OR i.status = :status)
            AND (:year IS NULL OR i.billingYear = :year)
            AND (:month IS NULL OR i.billingMonth = :month)
            ORDER BY i.issuedAt DESC
            """)
    Page<Invoice> findWithFilters(
            @Param("unitId") String unitId,
            @Param("status") InvoiceStatus status,
            @Param("year") Short year,
            @Param("month") Byte month,
            Pageable pageable
    );

    // Used by BalanceService — finds all unpaid invoices for a unit
    List<Invoice> findByUnitIdAndStatusIn(String unitId, List<InvoiceStatus> statuses);
}