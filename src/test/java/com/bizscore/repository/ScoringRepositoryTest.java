package com.bizscore.repository;

import com.bizscore.entity.ScoringRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты репозитория скоринга
 * Проверяет работу с базой данных
 */
@DataJpaTest
@ActiveProfiles("test")
class ScoringRepositoryTest {

    @Autowired
    private ScoringRepository scoringRepository;

    @Test
    void saveScoringRequest_ShouldPersistEntity() {
        // Given
        ScoringRequest request = new ScoringRequest();
        request.setCompanyName("Test Company");
        request.setInn("123456789012");
        request.setAnnualRevenue(1000000.0);
        request.setScore(0.75);
        request.setRiskLevel("LOW");
        request.setCreatedAt(LocalDateTime.now());

        // When
        ScoringRequest saved = scoringRepository.save(request);

        // Then
        assertNotNull(saved.getId());
        assertEquals("Test Company", saved.getCompanyName());
        assertEquals("LOW", saved.getRiskLevel());
    }

    @Test
    void findByCompanyNameAndInn_WithExistingRecord_ShouldReturnEntity() {
        // Given
        ScoringRequest request = new ScoringRequest();
        request.setCompanyName("Test Company");
        request.setInn("123456789012");
        request.setAnnualRevenue(1000000.0);
        scoringRepository.save(request);

        // When
        Optional<ScoringRequest> found = scoringRepository.findByCompanyNameAndInn("Test Company", "123456789012");

        // Then
        assertTrue(found.isPresent());
        assertEquals("Test Company", found.get().getCompanyName());
    }

    @Test
    void findByRiskLevel_WithMatchingRecords_ShouldReturnList() {
        // Given
        ScoringRequest request1 = new ScoringRequest();
        request1.setCompanyName("Company A");
        request1.setRiskLevel("LOW");
        scoringRepository.save(request1);

        ScoringRequest request2 = new ScoringRequest();
        request2.setCompanyName("Company B");
        request2.setRiskLevel("LOW");
        scoringRepository.save(request2);

        // When
        Page<ScoringRequest> lowRiskCompaniesPage = scoringRepository.findByRiskLevel("LOW", Pageable.unpaged());
        List<ScoringRequest> lowRiskCompanies = lowRiskCompaniesPage.getContent();

        // Then
        assertFalse(lowRiskCompanies.isEmpty());
        assertEquals(2, lowRiskCompanies.size());
    }
}