package com.bizscore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PolicyConditionRequest {
    private String field;
    private String operator;
    private String value;
    private Double numericValue;
    private Boolean booleanValue;
    private String logicalOperator;
}