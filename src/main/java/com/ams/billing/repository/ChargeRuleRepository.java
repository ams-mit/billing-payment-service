package com.ams.billing.repository;

import com.ams.billing.entity.ChargeRule;
import com.ams.billing.enums.ChargeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JpaRepository<ChargeRule, String> gives you these for free:
 *   save(entity)           → INSERT or UPDATE
 *   findById(id)           → SELECT WHERE id = ?
 *   findAll()              → SELECT all
 *   delete(entity)         → DELETE
 *   existsById(id)         → SELECT COUNT WHERE id = ?
 *
 * Everything below is extra — Spring generates the SQL from the method name.
 */
@Repository
public interface ChargeRuleRepository extends JpaRepository<ChargeRule, String> {

    // GET /charge-rules?status=ACTIVE
    // Spring generates: SELECT * FROM charge_rules WHERE status = ? (paginated)
    Page<ChargeRule> findByStatus(String status, Pageable pageable);

    // GET /charge-rules (no filter) — paginated
    Page<ChargeRule> findAll(Pageable pageable);

    // GET /charge-rules/type/{chargeType}
    // Spring generates: SELECT * FROM charge_rules WHERE charge_type = ? AND status = ?
    List<ChargeRule> findByChargeTypeAndStatus(ChargeType chargeType, String status);

    // Duplicate name check before creating — business rule enforcement
    // Spring generates: SELECT * FROM charge_rules WHERE name = ? AND status = 'ACTIVE'
    Optional<ChargeRule> findByNameAndStatus(String name, String status);

    // Custom JPQL query — filters by both status and chargeType when both are provided
    // JPQL uses class names and field names, not table/column names
    @Query("""
            SELECT cr FROM ChargeRule cr
            WHERE (:status IS NULL OR cr.status = :status)
            AND (:chargeType IS NULL OR cr.chargeType = :chargeType)
            ORDER BY cr.createdAt DESC
            """)
    Page<ChargeRule> findWithFilters(
            @Param("status") String status,
            @Param("chargeType") ChargeType chargeType,
            Pageable pageable
    );
}