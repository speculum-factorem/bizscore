package com.bizscore.controller;

import com.bizscore.dto.request.RiskPolicyRequest;
import com.bizscore.entity.RiskPolicy;
import com.bizscore.service.RiskPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk-policies")
@RequiredArgsConstructor
@Tag(name = "Risk Policy API", description = "API для управления риск-политиками")
public class RiskPolicyController {

    private final RiskPolicyService riskPolicyService;

    @Operation(summary = "Создать новую риск-политику")
    @PostMapping
    public ResponseEntity<RiskPolicy> createPolicy(@RequestBody RiskPolicyRequest request) {
        RiskPolicy policy = riskPolicyService.createPolicy(request);
        return ResponseEntity.ok(policy);
    }

    @Operation(summary = "Получить все активные политики")
    @GetMapping
    public ResponseEntity<List<RiskPolicy>> getAllActivePolicies() {
        List<RiskPolicy> policies = riskPolicyService.getAllActivePolicies();
        return ResponseEntity.ok(policies);
    }

    @Operation(summary = "Получить политики по типу")
    @GetMapping("/type/{policyType}")
    public ResponseEntity<List<RiskPolicy>> getPoliciesByType(@PathVariable String policyType) {
        List<RiskPolicy> policies = riskPolicyService.getPoliciesByType(policyType);
        return ResponseEntity.ok(policies);
    }

    @Operation(summary = "Обновить статус политики")
    @PatchMapping("/{policyId}/status")
    public ResponseEntity<RiskPolicy> updatePolicyStatus(
            @PathVariable Long policyId,
            @RequestParam Boolean isActive) {
        RiskPolicy policy = riskPolicyService.updatePolicyStatus(policyId, isActive);
        return ResponseEntity.ok(policy);
    }

    @Operation(summary = "Удалить политику")
    @DeleteMapping("/{policyId}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long policyId) {
        riskPolicyService.deletePolicy(policyId);
        return ResponseEntity.noContent().build();
    }
}