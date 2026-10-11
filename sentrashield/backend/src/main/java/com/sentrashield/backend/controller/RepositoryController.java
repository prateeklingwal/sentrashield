package com.sentrashield.backend.controller;

import com.sentrashield.backend.dto.RepositoryCreateRequest;
import com.sentrashield.backend.dto.RepositoryResponse;
import com.sentrashield.backend.entity.Repo;
import com.sentrashield.backend.entity.User;
import com.sentrashield.backend.repository.RepoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    private final RepoRepository repoRepository;

    public RepositoryController(RepoRepository repoRepository) {
        this.repoRepository = repoRepository;
    }

    @PostMapping
    public RepositoryResponse create(@RequestBody RepositoryCreateRequest request,
                                      @AuthenticationPrincipal User user) {
        if (request.githubRepoUrl() == null || request.githubRepoUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "githubRepoUrl is required");
        }

        Repo repo = repoRepository.findByUserAndGithubRepoUrl(user, request.githubRepoUrl())
                .orElseGet(() -> {
                    Repo r = new Repo();
                    r.setUser(user);
                    r.setGithubRepoUrl(request.githubRepoUrl());
                    if (request.defaultBranch() != null && !request.defaultBranch().isBlank()) {
                        r.setDefaultBranch(request.defaultBranch());
                    }
                    return repoRepository.save(r);
                });

        return toResponse(repo);
    }

    @GetMapping
    public List<RepositoryResponse> list(@AuthenticationPrincipal User user) {
        return repoRepository.findByUser(user).stream()
                .map(this::toResponse)
                .toList();
    }

    private RepositoryResponse toResponse(Repo repo) {
        return new RepositoryResponse(
                repo.getId(),
                repo.getGithubRepoUrl(),
                repo.getDefaultBranch(),
                repo.getLastScannedAt(),
                repo.getCreatedAt()
        );
    }
}
