package com.bizscore.controller;

import com.bizscore.dto.request.DecisionUpdateRequest;
import com.bizscore.entity.ScoringDecision;
import com.bizscore.service.PolicyEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/decisions")
@RequiredArgsConstructor
@Tag(name = "Decision API", description = "API для управления решениями по скорингу")
public class DecisionController {

    private final PolicyEngineService policyEngineService;

    @Operation(summary = "Получить все ожидающие решения")
    @GetMapping("/pending")
    public ResponseEntity<List<ScoringDecision>> getPendingDecisions() {
        List<ScoringDecision> decisions = policyEngineService.getPendingDecisions();
        return ResponseEntity.ok(decisions);
    }

    @Operation(summary = "Получить ожидающие решения по приоритету")
    @GetMapping("/pending/priority/{priority}")
    public ResponseEntity<List<ScoringDecision>> getPendingDecisionsByPriority(@PathVariable String priority) {
        List<ScoringDecision> decisions = policyEngineService.getPendingDecisionsByPriority(priority);
        return ResponseEntity.ok(decisions);
    }

    @Operation(summary = "Обновить решение по скорингу")
    @PutMapping("/{decisionId}")
    public ResponseEntity<ScoringDecision> updateDecision(
            @PathVariable Long decisionId,
            @RequestBody DecisionUpdateRequest request) {
        ScoringDecision decision = policyEngineService.updateDecision(
                decisionId, request.getFinalDecision(), request.getManagerNotes(), request.getResolvedBy());
        return ResponseEntity.ok(decision);
    }
}