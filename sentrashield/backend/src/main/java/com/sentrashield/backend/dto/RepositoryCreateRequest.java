package com.sentrashield.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record RepositoryCreateRequest(
        @JsonAlias("github_repo_url") String githubRepoUrl,
        @JsonAlias("default_branch") String defaultBranch
) {}
