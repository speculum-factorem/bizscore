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
@Table(name = "scoring_request")
public class ScoringRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;
    private Double revenue;
    private Integer employees;
    private Integer businessAge;

    private Double score;
    private String riskLevel;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Поля для ML сервиса
    private String inn;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "years_in_business")
    private Integer yearsInBusiness;

    @Column(name = "annual_revenue")
    private Double annualRevenue;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "requested_amount")
    private Double requestedAmount;

    @Column(name = "has_existing_loans")
    private Boolean hasExistingLoans;

    private String industry;

    @Column(name = "credit_history")
    private Integer creditHistory;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}