package com.bizscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoringDecisionResponse {
    private Long id;
    private Long scoringRequestId;
    private String decision;
    private String reason;
    private String appliedPolicy;
    private String priority;
    private String managerNotes;
    private String finalDecision;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}