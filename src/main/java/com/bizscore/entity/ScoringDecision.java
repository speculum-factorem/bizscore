package com.bizscore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "scoring_decisions")
public class ScoringDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scoring_request_id")
    private Long scoringRequestId;

    @Column(nullable = false)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private String appliedPolicy;

    private String priority;

    @Column(name = "manager_notes")
    private String managerNotes;

    @Column(name = "final_decision")
    private String finalDecision;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (finalDecision == null) {
            finalDecision = "PENDING";
        }
    }
}