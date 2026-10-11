package com.sentrashield.backend.repository;

import com.sentrashield.backend.entity.SbomDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SbomDocumentRepository extends JpaRepository<SbomDocument, Long> {
    Optional<SbomDocument> findByScanId(Long scanId);
}
