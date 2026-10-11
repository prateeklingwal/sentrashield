package com.sentrashield.backend.dto;

import java.util.List;

public record ScanResponse(
        List<DependencyResult> dependencies
) {}
