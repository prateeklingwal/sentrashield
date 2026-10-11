package com.sentrashield.backend.dto;

import java.util.List;

public record DependencyResult(
        String name,
        String version,
        List<VulnerabilityResult> vulnerabilities
) {}
