package com.bizscore.repository;

import com.bizscore.entity.ScoringDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoringDecisionRepository extends JpaRepository<ScoringDecision, Long> {

    Optional<ScoringDecision> findByScoringRequestId(Long scoringRequestId);

    List<ScoringDecision> findByFinalDecision(String finalDecision);

    List<ScoringDecision> findByDecision(String decision);

    @Query("SELECT d FROM ScoringDecision d WHERE d.finalDecision = 'PENDING' ORDER BY d.createdAt DESC")
    List<ScoringDecision> findPendingDecisions();

    @Query("SELECT d FROM ScoringDecision d WHERE d.priority = :priority AND d.finalDecision = 'PENDING'")
    List<ScoringDecision> findPendingByPriority(String priority);
}