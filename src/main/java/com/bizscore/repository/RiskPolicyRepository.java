package com.bizscore.repository;

import com.bizscore.entity.RiskPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskPolicyRepository extends JpaRepository<RiskPolicy, Long> {

    List<RiskPolicy> findByIsActiveTrueOrderByPriorityAsc();

    List<RiskPolicy> findByPolicyTypeAndIsActiveTrue(String policyType);

    @Query("SELECT p FROM RiskPolicy p WHERE p.isActive = true AND p.policyType IN :types ORDER BY p.priority ASC")
    List<RiskPolicy> findActivePoliciesByTypes(List<String> types);
}