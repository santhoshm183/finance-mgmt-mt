package com.example.financemgmt.dto;

import com.example.financemgmt.model.CashFlow;
import java.math.BigDecimal;

public record CashFlowResponse(BigDecimal investmentAmount, BigDecimal financialBalance,
                               BigDecimal profitAmount, BigDecimal collectionAmount) {
    public static CashFlowResponse from(CashFlow cashFlow) {
        return new CashFlowResponse(cashFlow.getInvestmentAmount(), cashFlow.getFinancialBalance(),
                cashFlow.getProfitAmount(), cashFlow.getCollectionAmount());
    }
}
