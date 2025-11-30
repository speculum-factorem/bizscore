package com.bizscore.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter scoringRequestsCounter;
    private final Counter scoringSuccessCounter;
    private final Counter scoringFailureCounter;
    private final Counter mlServiceCallsCounter;
    private final Counter fallbackScoringCounter;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.scoringRequestsCounter = Counter.builder("scoring.requests.total")
                .description("Total number of scoring requests")
                .register(meterRegistry);

        this.scoringSuccessCounter = Counter.builder("scoring.requests.success")
                .description("Number of successful scoring requests")
                .register(meterRegistry);

        this.scoringFailureCounter = Counter.builder("scoring.requests.failure")
                .description("Number of failed scoring requests")
                .register(meterRegistry);

        this.mlServiceCallsCounter = Counter.builder("scoring.ml.service.calls")
                .description("Number of ML service calls")
                .register(meterRegistry);

        this.fallbackScoringCounter = Counter.builder("scoring.fallback.used")
                .description("Number of times fallback scoring was used")
                .register(meterRegistry);
    }

    public void incrementScoringRequests() {
        scoringRequestsCounter.increment();
    }

    public void incrementScoringSuccess() {
        scoringSuccessCounter.increment();
    }

    public void incrementScoringFailure() {
        scoringFailureCounter.increment();
    }

    public void incrementMlServiceCalls() {
        mlServiceCallsCounter.increment();
    }

    public void incrementFallbackScoring() {
        fallbackScoringCounter.increment();
    }

    public Timer.Sample startScoringTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopScoringTimer(Timer.Sample sample, String riskLevel) {
        sample.stop(Timer.builder("scoring.processing.time")
                .description("Time taken to process scoring requests")
                .tag("risk_level", riskLevel)
                .register(meterRegistry));
    }

    public void recordScoreValue(double score) {
        meterRegistry.gauge("scoring.score.value", score);
    }
}