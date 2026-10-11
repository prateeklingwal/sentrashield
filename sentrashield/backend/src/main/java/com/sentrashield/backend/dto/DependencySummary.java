package com.sentrashield.backend.dto;

public record DependencySummary(
        String name,
        String version,
        int vulnerabilityCount
) {}
