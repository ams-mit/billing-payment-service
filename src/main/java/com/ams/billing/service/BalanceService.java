package com.ams.billing.service;

import com.ams.billing.dto.response.BalanceResponse;

public interface BalanceService {

    BalanceResponse getBalanceByUnit(String unitId);

    BalanceResponse getBalanceByResident(String residentId);
}