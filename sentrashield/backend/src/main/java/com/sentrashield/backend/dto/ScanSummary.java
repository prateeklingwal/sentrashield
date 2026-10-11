package com.sentrashield.backend.dto;

import java.util.List;

public record ScanSummary(
        Long scanId,
        String repoUrl,
        String sbomUrl,
        List<DependencySummary> dependencies
) {}
