package com.bizscore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DecisionUpdateRequest {
    private String finalDecision;
    private String managerNotes;
    private String resolvedBy;
}