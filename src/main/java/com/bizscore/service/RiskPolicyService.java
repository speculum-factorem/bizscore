package com.bizscore.service;

import com.bizscore.dto.request.PolicyConditionRequest;
import com.bizscore.dto.request.RiskPolicyRequest;
import com.bizscore.entity.PolicyCondition;
import com.bizscore.entity.RiskPolicy;
import com.bizscore.repository.RiskPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskPolicyService {

    private final RiskPolicyRepository riskPolicyRepository;

    public RiskPolicy createPolicy(RiskPolicyRequest request) {
        log.info("Creating new risk policy: {}", request.getName());

        RiskPolicy policy = new RiskPolicy();
        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        policy.setPolicyType(request.getPolicyType());
        policy.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        policy.setPriority(request.getPriority() != null ? request.getPriority() : 1);
        policy.setAction(request.getAction());
        policy.setActionValue(request.getActionValue());

        if (request.getConditions() != null) {
            List<PolicyCondition> conditions = request.getConditions().stream()
                    .map(cond -> {
                        PolicyCondition condition = new PolicyCondition();
                        condition.setField(cond.getField());
                        condition.setOperator(cond.getOperator());
                        condition.setValue(cond.getValue());
                        condition.setNumericValue(cond.getNumericValue());
                        condition.setBooleanValue(cond.getBooleanValue());
                        condition.setLogicalOperator(cond.getLogicalOperator());
                        condition.setPolicy(policy);
                        return condition;
                    })
                    .collect(Collectors.toList());
            policy.setConditions(conditions);
        }

        RiskPolicy savedPolicy = riskPolicyRepository.save(policy);
        log.info("Risk policy created successfully with ID: {}", savedPolicy.getId());
        return savedPolicy;
    }

    public List<RiskPolicy> getAllActivePolicies() {
        return riskPolicyRepository.findByIsActiveTrueOrderByPriorityAsc();
    }

    public List<RiskPolicy> getPoliciesByType(String policyType) {
        return riskPolicyRepository.findByPolicyTypeAndIsActiveTrue(policyType);
    }

    public RiskPolicy updatePolicyStatus(Long policyId, Boolean isActive) {
        RiskPolicy policy = riskPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found with ID: " + policyId));

        policy.setIsActive(isActive);
        RiskPolicy updatedPolicy = riskPolicyRepository.save(policy);

        log.info("Policy {} status updated to: {}", policyId, isActive);
        return updatedPolicy;
    }

    public void deletePolicy(Long policyId) {
        RiskPolicy policy = riskPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found with ID: " + policyId));

        riskPolicyRepository.delete(policy);
        log.info("Policy deleted with ID: {}", policyId);
    }
}