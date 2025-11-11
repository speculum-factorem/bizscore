package com.bizscore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RiskPolicyRequest {
    private String name;
    private String description;
    private String policyType;
    private Boolean isActive;
    private Integer priority;
    private List<PolicyConditionRequest> conditions;
    private String action;
    private String actionValue;
}