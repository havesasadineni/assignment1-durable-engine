package com.durable.engine;

public record StepRecord(
        String workflowId,
        String stepKey,
        StepStatus status,
        String outputJson,
        String errorMessage,
        long startedAtEpochMs,
        long updatedAtEpochMs
) {}
