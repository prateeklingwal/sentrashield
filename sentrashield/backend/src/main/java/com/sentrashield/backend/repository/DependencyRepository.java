package com.sentrashield.backend.repository;

import com.sentrashield.backend.entity.Dependency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependencyRepository extends JpaRepository<Dependency, Long> {
}
