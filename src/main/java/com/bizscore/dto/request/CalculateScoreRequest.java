package com.bizscore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculateScoreRequest {
    private String companyName;
    private String inn;
    private String businessType;
    private Integer yearsInBusiness;
    private Double annualRevenue;
    private Integer employeeCount;
    private Double requestedAmount;
    private Boolean hasExistingLoans;
    private String industry;
    private Integer creditHistory;
}