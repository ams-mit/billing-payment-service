package com.ams.billing.repository;

import com.ams.billing.entity.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, String> {

    // BILL-022: GET /receipts/payments/{paymentId}
    Optional<Receipt> findByPaymentId(String paymentId);

    // BILL-023: GET /receipts/units/{unitId}
    Page<Receipt> findByUnitId(String unitId, Pageable pageable);
}