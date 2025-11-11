package com.bizscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnhancedScoringResponse {
    private Long id;
    private String companyName;
    private String inn;
    private Double score;
    private String riskLevel;
    private String processingStatus;
    private String priority;
    private String decisionReason;
    private LocalDateTime createdAt;
    private ScoringDecisionResponse decisionDetails;
}