package com.sentrashield.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;

public record ScanRequest(
        @JsonAlias("repo_id") Long repoId,
        @JsonAlias("package_json") JsonNode packageJson
) {}
