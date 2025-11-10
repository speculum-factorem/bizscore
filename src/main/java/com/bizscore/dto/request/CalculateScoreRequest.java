package com.bizscore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculateScoreRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "INN is required")
    private String inn;

    private String businessType;

    @NotNull(message = "Years in business is required")
    @PositiveOrZero(message = "Years in business must be positive or zero")
    private Integer yearsInBusiness;

    @NotNull(message = "Annual revenue is required")
    @PositiveOrZero(message = "Annual revenue must be positive or zero")
    private Double annualRevenue;

    @NotNull(message = "Employee count is required")
    @PositiveOrZero(message = "Employee count must be positive or zero")
    private Integer employeeCount;

    @NotNull(message = "Requested amount is required")
    @PositiveOrZero(message = "Requested amount must be positive or zero")
    private Double requestedAmount;

    private Boolean hasExistingLoans;

    private String industry;

    private Integer creditHistory;
}