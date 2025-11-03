package com.bizscore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoringResponse {
    private Long id;
    private String companyName;
    private Double score;
    private String riskLevel;
    private LocalDateTime createdAt;
}