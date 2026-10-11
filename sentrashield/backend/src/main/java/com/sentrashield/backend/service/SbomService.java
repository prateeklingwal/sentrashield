package com.sentrashield.backend.service;

import com.sentrashield.backend.dto.CycloneDx;
import com.sentrashield.backend.dto.DependencyResult;
import com.sentrashield.backend.dto.VulnerabilityResult;
import com.sentrashield.backend.entity.Repo;
import com.sentrashield.backend.entity.Scan;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SbomService {

    public CycloneDx.Bom generate(Repo repo, Scan scan, List<DependencyResult> dependencies) {
        String repoName = extractRepoName(repo.getGithubRepoUrl());
        String appVersion = scan.getCommitSha() != null ? scan.getCommitSha() : "scan-" + scan.getId();

        CycloneDx.Component appComponent =
                new CycloneDx.Component("application", null, repoName, appVersion, null);
        CycloneDx.Metadata metadata =
                new CycloneDx.Metadata(OffsetDateTime.now().toString(), appComponent);

        List<CycloneDx.Component> components = new ArrayList<>();
        List<CycloneDx.Vulnerability> vulnerabilities = new ArrayList<>();

        for (DependencyResult dep : dependencies) {
            String purl = "pkg:npm/" + dep.name() + "@" + dep.version();
            components.add(new CycloneDx.Component("library", purl, dep.name(), dep.version(), purl));

            for (VulnerabilityResult vuln : dep.vulnerabilities()) {
                vulnerabilities.add(new CycloneDx.Vulnerability(
                        vuln.osvId(),
                        new CycloneDx.Source("OSV"),
                        vuln.summary(),
                        List.of(new CycloneDx.Affect(purl))
                ));
            }
        }

        return new CycloneDx.Bom(
                "CycloneDX",
                "1.7",
                "urn:uuid:" + UUID.randomUUID(),
                1,
                metadata,
                components,
                vulnerabilities
        );
    }

    private String extractRepoName(String githubRepoUrl) {
        if (githubRepoUrl == null) {
            return "unknown-repository";
        }
        String trimmed = githubRepoUrl.replaceAll("/+$", "");
        int lastSlash = trimmed.lastIndexOf('/');
        return lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
    }
}
