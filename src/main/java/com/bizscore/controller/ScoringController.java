package com.bizscore.controller;

import com.bizscore.entity.ScoringRequest;
import com.bizscore.service.ScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ScoringController {

    private final ScoringService scoringService;

    public ScoringController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @PostMapping("/score")
    public ResponseEntity<ScoringRequest> calculateScore(@RequestBody ScoringRequest request) {
        ScoringRequest result = scoringService.calculateScore(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/score/{id}")
    public ResponseEntity<ScoringRequest> getScore(@PathVariable Long id) {
        ScoringRequest result = scoringService.getById(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public String health() {
        return "BizScore Service is working!";
    }
}