package com.bizscore.repository;

import com.bizscore.entity.ScoringRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoringRepository extends JpaRepository<ScoringRequest, Long> {}