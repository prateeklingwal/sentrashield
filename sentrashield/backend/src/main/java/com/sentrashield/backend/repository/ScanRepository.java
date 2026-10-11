package com.sentrashield.backend.repository;

import com.sentrashield.backend.entity.Scan;
import com.sentrashield.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScanRepository extends JpaRepository<Scan, Long> {
    Optional<Scan> findByIdAndRepo_User(Long id, User user);
}
