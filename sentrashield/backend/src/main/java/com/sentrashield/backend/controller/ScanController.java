package com.sentrashield.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentrashield.backend.dto.*;
import com.sentrashield.backend.entity.*;
import com.sentrashield.backend.repository.*;
import com.sentrashield.backend.service.SbomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/scans")
public class ScanController {

    private final RestClient mlServiceRestClient;
    private final ObjectMapper objectMapper;
    private final SbomService sbomService;
    private final RepoRepository repoRepository;
    private final ScanRepository scanRepository;
    private final DependencyRepository dependencyRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final SbomDocumentRepository sbomDocumentRepository;

    public ScanController(RestClient mlServiceRestClient,
                           ObjectMapper objectMapper,
                           SbomService sbomService,
                           RepoRepository repoRepository,
                           ScanRepository scanRepository,
                           DependencyRepository dependencyRepository,
                           VulnerabilityRepository vulnerabilityRepository,
                           SbomDocumentRepository sbomDocumentRepository) {
        this.mlServiceRestClient = mlServiceRestClient;
        this.objectMapper = objectMapper;
        this.sbomService = sbomService;
        this.repoRepository = repoRepository;
        this.scanRepository = scanRepository;
        this.dependencyRepository = dependencyRepository;
        this.vulnerabilityRepository = vulnerabilityRepository;
        this.sbomDocumentRepository = sbomDocumentRepository;
    }

    @PostMapping
    public ScanSummary runScan(@RequestBody ScanRequest request, @AuthenticationPrincipal User user) {
        JsonNode packageJson = request.packageJson();
        if (packageJson == null || packageJson.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "packageJson is required");
        }
        if (packageJson.isTextual()) {
            try {
                packageJson = objectMapper.readTree(packageJson.asText());
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "packageJson string is not valid JSON", e);
            }
        }

        if (request.repoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "repoId is required");
        }
        Repo repo = repoRepository.findByIdAndUser(request.repoId(), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "repository not found"));

        Scan scan = new Scan();
        scan.setRepo(repo);
        scan.setStatus("RUNNING");
        scan.setStartedAt(OffsetDateTime.now());
        scan = scanRepository.save(scan);

        ScanResponse mlResponse;
        try {
            mlResponse = mlServiceRestClient.post()
                    .uri("/scan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(packageJson)
                    .retrieve()
                    .body(ScanResponse.class);

            if (mlResponse == null || mlResponse.dependencies() == null) {
                throw new IllegalStateException("ml-service returned an empty or invalid response");
            }
        } catch (Exception e) {
            markFailed(scan);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Scan failed while contacting the ML service: " + e.getMessage(), e);
        }

        List<DependencySummary> summaries = new ArrayList<>();
        String sbomUrl = "/api/scans/" + scan.getId() + "/sbom";
        try {
            for (DependencyResult dep : mlResponse.dependencies()) {
                Dependency dependency = new Dependency();
                dependency.setScan(scan);
                dependency.setName(dep.name());
                dependency.setVersion(dep.version());
                dependency = dependencyRepository.save(dependency);

                for (VulnerabilityResult vuln : dep.vulnerabilities()) {
                    Vulnerability vulnerability = new Vulnerability();
                    vulnerability.setDependency(dependency);
                    vulnerability.setOsvId(vuln.osvId());
                    vulnerability.setDescriptionSource(vuln.summary());
                    vulnerabilityRepository.save(vulnerability);
                }

                summaries.add(new DependencySummary(dep.name(), dep.version(), dep.vulnerabilities().size()));
            }

            CycloneDx.Bom sbom = sbomService.generate(repo, scan, mlResponse.dependencies());
            String sbomJson = objectMapper.writeValueAsString(sbom);

            SbomDocument sbomDocument = new SbomDocument();
            sbomDocument.setScan(scan);
            sbomDocument.setFormat("CycloneDX-1.7");
            sbomDocument.setStorageUrl(sbomUrl);
            sbomDocument.setContent(sbomJson);
            sbomDocumentRepository.save(sbomDocument);
        } catch (Exception e) {
            markFailed(scan);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Scan failed while saving results: " + e.getMessage(), e);
        }

        scan.setStatus("COMPLETED");
        scan.setFinishedAt(OffsetDateTime.now());
        scanRepository.save(scan);

        repo.setLastScannedAt(OffsetDateTime.now());
        repoRepository.save(repo);

        return new ScanSummary(scan.getId(), repo.getGithubRepoUrl(), sbomUrl, summaries);
    }

    @GetMapping("/{scanId}/sbom")
    public ResponseEntity<String> getSbom(@PathVariable Long scanId, @AuthenticationPrincipal User user) {
        Scan scan = scanRepository.findByIdAndRepo_User(scanId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No SBOM found for scan " + scanId));

        SbomDocument sbom = sbomDocumentRepository.findByScanId(scan.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No SBOM found for scan " + scanId));

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/vnd.cyclonedx+json"))
                .body(sbom.getContent());
    }

    private void markFailed(Scan scan) {
        scan.setStatus("FAILED");
        scan.setFinishedAt(OffsetDateTime.now());
        scanRepository.save(scan);
    }
}
