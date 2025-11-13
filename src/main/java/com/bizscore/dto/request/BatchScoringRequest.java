package com.bizscore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для пакетного запроса скоринга
 * Содержит список компаний для одновременной обработки
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchScoringRequest {

    @NotNull(message = "Список запросов не может быть пустым")
    @Size(min = 1, max = 100, message = "Количество запросов должно быть от 1 до 100")
    @Valid
    private List<CalculateScoreRequest> requests;

    private String batchName;
    private String priority = "NORMAL";
}