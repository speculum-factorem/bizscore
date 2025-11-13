package com.bizscore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * DTO для ответа пакетного скоринга
 * Содержит результаты обработки всех компаний в пакете
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchScoringResponse {

    private String batchId;
    private int totalRequests;
    private List<EnhancedScoringResponse> successfulResults;
    private List<Map<String, Object>> failedResults;
    private Date processedAt;
    private String status = "COMPLETED";
    private String summary;
}