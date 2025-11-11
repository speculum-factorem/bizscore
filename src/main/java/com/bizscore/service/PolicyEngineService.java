package com.bizscore.service;

import com.bizscore.entity.RiskPolicy;
import com.bizscore.entity.ScoringDecision;
import com.bizscore.entity.ScoringRequest;
import com.bizscore.repository.RiskPolicyRepository;
import com.bizscore.repository.ScoringDecisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.bizscore.entity.PolicyCondition;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyEngineService {

    private final RiskPolicyRepository riskPolicyRepository;
    private final ScoringDecisionRepository scoringDecisionRepository;

    public ScoringDecision evaluatePolicies(ScoringRequest scoringRequest) {
        log.info("Evaluating policies for scoring request ID: {}", scoringRequest.getId());

        List<RiskPolicy> activePolicies = riskPolicyRepository.findActivePoliciesByTypes(
                List.of("APPROVAL", "REJECTION", "ESCALATION", "PRIORITY"));

        String finalDecision = "MANUAL_REVIEW";
        String reason = "No matching policies found";
        String appliedPolicy = null;
        String priority = "MEDIUM";

        // Проверяем политики в порядке приоритета
        for (RiskPolicy policy : activePolicies) {
            if (evaluatePolicy(policy, scoringRequest)) {
                appliedPolicy = policy.getName();
                finalDecision = policy.getAction();
                reason = String.format("Policy '%s' triggered", policy.getName());

                if ("SET_PRIORITY".equals(policy.getAction()) && policy.getActionValue() != null) {
                    priority = policy.getActionValue();
                    // Продолжаем проверять другие политики для приоритета
                } else {
                    break; // Останавливаемся на первом совпадении для APPROVAL/REJECTION
                }
            }
        }

        ScoringDecision decision = createScoringDecision(scoringRequest.getId(), finalDecision, reason, appliedPolicy, priority);
        log.info("Policy evaluation completed. Decision: {}, Priority: {}", finalDecision, priority);

        return decision;
    }

    private boolean evaluatePolicy(RiskPolicy policy, ScoringRequest scoringRequest) {
        if (policy.getConditions() == null || policy.getConditions().isEmpty()) {
            return false;
        }

        boolean result = true;
        String lastLogicalOperator = null;

        for (PolicyCondition condition : policy.getConditions()) {
            boolean conditionResult = evaluateCondition(condition, scoringRequest);

            if (lastLogicalOperator != null) {
                if ("AND".equalsIgnoreCase(lastLogicalOperator)) {
                    result = result && conditionResult;
                } else if ("OR".equalsIgnoreCase(lastLogicalOperator)) {
                    result = result || conditionResult;
                }
            } else {
                result = conditionResult;
            }

            lastLogicalOperator = condition.getLogicalOperator();
        }

        return result;
    }

    private boolean evaluateCondition(PolicyCondition condition, ScoringRequest scoringRequest) {
        String field = condition.getField();
        String operator = condition.getOperator();

        try {
            switch (field) {
                case "annualRevenue":
                    return evaluateNumericCondition(scoringRequest.getAnnualRevenue(), condition);
                case "yearsInBusiness":
                    return evaluateNumericCondition(scoringRequest.getYearsInBusiness(), condition);
                case "employeeCount":
                    return evaluateNumericCondition(scoringRequest.getEmployeeCount(), condition);
                case "requestedAmount":
                    return evaluateNumericCondition(scoringRequest.getRequestedAmount(), condition);
                case "hasExistingLoans":
                    return evaluateBooleanCondition(scoringRequest.getHasExistingLoans(), condition);
                case "creditHistory":
                    return evaluateNumericCondition(scoringRequest.getCreditHistory(), condition);
                case "companyName":
                    return evaluateStringCondition(scoringRequest.getCompanyName(), condition);
                case "industry":
                    return evaluateStringCondition(scoringRequest.getIndustry(), condition);
                default:
                    return false;
            }
        } catch (Exception e) {
            log.warn("Error evaluating condition for field {}: {}", field, e.getMessage());
            return false;
        }
    }

    private boolean evaluateNumericCondition(Double value, PolicyCondition condition) {
        if (value == null) return false;

        return switch (condition.getOperator()) {
            case "GREATER_THAN" -> value > (condition.getNumericValue() != null ? condition.getNumericValue() : 0);
            case "LESS_THAN" -> value < (condition.getNumericValue() != null ? condition.getNumericValue() : 0);
            case "EQUALS" -> value.equals(condition.getNumericValue());
            case "GREATER_THAN_OR_EQUAL" -> value >= (condition.getNumericValue() != null ? condition.getNumericValue() : 0);
            case "LESS_THAN_OR_EQUAL" -> value <= (condition.getNumericValue() != null ? condition.getNumericValue() : 0);
            default -> false;
        };
    }

    private boolean evaluateNumericCondition(Integer value, PolicyCondition condition) {
        if (value == null) return false;
        return evaluateNumericCondition(value.doubleValue(), condition);
    }

    private boolean evaluateStringCondition(String value, PolicyCondition condition) {
        if (value == null) return false;

        return switch (condition.getOperator()) {
            case "EQUALS" -> value.equalsIgnoreCase(condition.getValue());
            case "CONTAINS" -> value.toLowerCase().contains(condition.getValue().toLowerCase());
            case "STARTS_WITH" -> value.toLowerCase().startsWith(condition.getValue().toLowerCase());
            case "ENDS_WITH" -> value.toLowerCase().endsWith(condition.getValue().toLowerCase());
            default -> false;
        };
    }

    private boolean evaluateBooleanCondition(Boolean value, PolicyCondition condition) {
        if (value == null) return false;

        if ("EQUALS".equals(condition.getOperator())) {
            return value.equals(condition.getBooleanValue());
        }

        return false;
    }

    private ScoringDecision createScoringDecision(Long scoringRequestId, String decision, String reason,
                                                  String appliedPolicy, String priority) {
        ScoringDecision scoringDecision = new ScoringDecision();
        scoringDecision.setScoringRequestId(scoringRequestId);
        scoringDecision.setDecision(decision);
        scoringDecision.setReason(reason);
        scoringDecision.setAppliedPolicy(appliedPolicy);
        scoringDecision.setPriority(priority);
        scoringDecision.setFinalDecision("PENDING");

        return scoringDecisionRepository.save(scoringDecision);
    }

    public ScoringDecision updateDecision(Long decisionId, String finalDecision, String managerNotes, String resolvedBy) {
        ScoringDecision decision = scoringDecisionRepository.findById(decisionId)
                .orElseThrow(() -> new RuntimeException("Decision not found with ID: " + decisionId));

        decision.setFinalDecision(finalDecision);
        decision.setManagerNotes(managerNotes);
        decision.setResolvedBy(resolvedBy);
        decision.setResolvedAt(LocalDateTime.now());

        ScoringDecision updatedDecision = scoringDecisionRepository.save(decision);
        log.info("Decision {} updated to: {}", decisionId, finalDecision);

        return updatedDecision;
    }

    public List<ScoringDecision> getPendingDecisions() {
        return scoringDecisionRepository.findPendingDecisions();
    }

    public List<ScoringDecision> getPendingDecisionsByPriority(String priority) {
        return scoringDecisionRepository.findPendingByPriority(priority);
    }
}