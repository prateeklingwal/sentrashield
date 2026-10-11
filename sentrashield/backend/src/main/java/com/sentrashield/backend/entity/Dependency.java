package com.sentrashield.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "dependencies")
public class Dependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    private String name;

    private String version;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "is_malicious_flag", nullable = false)
    private boolean malicious = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Scan getScan() { return scan; }
    public void setScan(Scan scan) { this.scan = scan; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public boolean isMalicious() { return malicious; }
    public void setMalicious(boolean malicious) { this.malicious = malicious; }
}
