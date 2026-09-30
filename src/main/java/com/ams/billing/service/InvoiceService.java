package com.ams.billing.service;

import com.ams.billing.dto.request.CreateInvoiceRequest;
import com.ams.billing.dto.request.UpdateInvoiceStatusRequest;
import com.ams.billing.dto.response.InvoiceLineResponse;
import com.ams.billing.dto.response.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InvoiceService {

    InvoiceResponse generateInvoice(CreateInvoiceRequest request, String issuedBy);

    Page<InvoiceResponse> getAllInvoices(String unitId, String status,
                                         Integer year, Integer month, Pageable pageable);

    InvoiceResponse getInvoiceById(String invoiceId, String requestingUserId,
                                   List<String> requestingUserRoles);

    Page<InvoiceResponse> getInvoicesByUnit(String unitId, String requestingUserId,
                                            List<String> requestingUserRoles,
                                            Pageable pageable);

    InvoiceResponse getInvoiceByUnitAndPeriod(String unitId, Integer year,
                                              Integer month, String requestingUserId,
                                              List<String> requestingUserRoles);

    InvoiceResponse updateInvoiceStatus(String invoiceId, UpdateInvoiceStatusRequest request);

    List<InvoiceLineResponse> getInvoiceLines(String invoiceId, String requestingUserId,
                                              List<String> requestingUserRoles);
}