package com.sentrashield.backend.dto;

import java.time.OffsetDateTime;

public record RepositoryResponse(
        Long id,
        String githubRepoUrl,
        String defaultBranch,
        OffsetDateTime lastScannedAt,
        OffsetDateTime createdAt
) {}
