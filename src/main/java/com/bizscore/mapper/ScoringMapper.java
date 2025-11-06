package com.bizscore.mapper;

import com.bizscore.dto.request.CalculateScoreRequest;
import com.bizscore.dto.response.ScoringResponse;
import com.bizscore.entity.ScoringRequest;
import org.springframework.stereotype.Component;

@Component
public class ScoringMapper {

    public ScoringRequest toEntity(CalculateScoreRequest dto) {
        ScoringRequest entity = new ScoringRequest();
        entity.setCompanyName(dto.getCompanyName());
        entity.setInn(dto.getInn());
        entity.setBusinessType(dto.getBusinessType());
        entity.setYearsInBusiness(dto.getYearsInBusiness());
        entity.setAnnualRevenue(dto.getAnnualRevenue());
        entity.setEmployeeCount(dto.getEmployeeCount());
        entity.setRequestedAmount(dto.getRequestedAmount());
        entity.setHasExistingLoans(dto.getHasExistingLoans());
        entity.setIndustry(dto.getIndustry());
        entity.setCreditHistory(dto.getCreditHistory());
        return entity;
    }

    public ScoringResponse toResponse(ScoringRequest entity) {
        ScoringResponse response = new ScoringResponse();
        response.setId(entity.getId());
        response.setCompanyName(entity.getCompanyName());
        response.setInn(entity.getInn());
        response.setScore(entity.getScore());
        response.setRiskLevel(entity.getRiskLevel());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}