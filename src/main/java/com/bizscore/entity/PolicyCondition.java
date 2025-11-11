package com.bizscore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "policy_conditions")
public class PolicyCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String field;

    @Column(nullable = false)
    private String operator;

    private String value;

    private Double numericValue;

    private Boolean booleanValue;

    @Column(name = "logical_operator")
    private String logicalOperator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private RiskPolicy policy;
}