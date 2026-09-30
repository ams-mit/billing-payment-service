package com.ams.billing.repository;

import com.ams.billing.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, String> {

    // BILL-013: GET /invoices/{invoiceId}/lines
    List<InvoiceLine> findByInvoiceId(String invoiceId);
}