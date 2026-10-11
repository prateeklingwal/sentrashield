package com.sentrashield.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Minimal CycloneDX 1.7 JSON shape -- just the fields SentraShield needs
 * (metadata, components, vulnerabilities). Not a full schema implementation.
 */
public class CycloneDx {

    private CycloneDx() {}

    public record Bom(
            String bomFormat,
            String specVersion,
            String serialNumber,
            int version,
            Metadata metadata,
            List<Component> components,
            List<Vulnerability> vulnerabilities
    ) {}

    public record Metadata(
            String timestamp,
            Component component
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Component(
            String type,
            @JsonProperty("bom-ref") String bomRef,
            String name,
            String version,
            String purl
    ) {}

    public record Vulnerability(
            String id,
            Source source,
            String description,
            List<Affect> affects
    ) {}

    public record Source(String name) {}

    public record Affect(String ref) {}
}
