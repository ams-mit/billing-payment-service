package com.ams.billing.repository;

import com.ams.billing.entity.Adjustment;
import com.ams.billing.enums.AdjustmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AdjustmentRepository extends JpaRepository<Adjustment, String> {

    // BILL-025: GET /adjustments/invoices/{invoiceId}
    List<Adjustment> findByInvoiceIdOrderByCreatedAtDesc(String invoiceId);

    // Used by BalanceService — sum credits for a unit
    @Query("""
            SELECT COALESCE(SUM(a.amount), 0)
            FROM Adjustment a
            WHERE a.invoice.unitId = :unitId
            AND a.adjustmentType = :type
            """)
    BigDecimal sumByUnitIdAndType(
            @Param("unitId") String unitId,
            @Param("type") AdjustmentType type
    );

    // Used by BalanceService — sum credits for a specific invoice
    @Query("""
            SELECT COALESCE(SUM(a.amount), 0)
            FROM Adjustment a
            WHERE a.invoice.id = :invoiceId
            AND a.adjustmentType = :type
            """)
    BigDecimal sumByInvoiceIdAndType(
            @Param("invoiceId") String invoiceId,
            @Param("type") AdjustmentType type
    );
}