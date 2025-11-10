package com.bizscore.repository;

import com.bizscore.entity.ScoringRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoringRepository extends JpaRepository<ScoringRequest, Long> {

    Optional<ScoringRequest> findByCompanyNameAndInn(String companyName, String inn);

    Page<ScoringRequest> findByRiskLevel(String riskLevel, Pageable pageable);

    @Query("SELECT s FROM ScoringRequest s WHERE s.companyName ILIKE %:companyName%")
    Page<ScoringRequest> findByCompanyNameContaining(@Param("companyName") String companyName, Pageable pageable);

    @Query("SELECT COUNT(s) FROM ScoringRequest s WHERE s.riskLevel = :riskLevel")
    Long countByRiskLevel(@Param("riskLevel") String riskLevel);

    @Query("SELECT AVG(s.score) FROM ScoringRequest s WHERE s.score IS NOT NULL")
    Double findAverageScore();
}