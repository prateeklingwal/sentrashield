package com.sentrashield.backend.repository;

import com.sentrashield.backend.entity.Repo;
import com.sentrashield.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepoRepository extends JpaRepository<Repo, Long> {
    List<Repo> findByUser(User user);
    Optional<Repo> findByUserAndGithubRepoUrl(User user, String githubRepoUrl);
    Optional<Repo> findByIdAndUser(Long id, User user);
}
