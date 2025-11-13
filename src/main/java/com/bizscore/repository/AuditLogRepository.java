package com.bizscore.repository;

import com.bizscore.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByOperation(String operation, Pageable pageable);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM AuditLog a WHERE a.username = :username ORDER BY a.timestamp DESC")
    List<AuditLog> findByUsername(String username);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.status = 'ERROR' AND a.timestamp >= :since")
    Long countErrorsSince(LocalDateTime since);

    @Query("SELECT a.operation, COUNT(a) FROM AuditLog a GROUP BY a.operation")
    List<Object[]> countByOperation();
}