CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE repositories (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_repo_url  VARCHAR(500) NOT NULL,
    default_branch   VARCHAR(100) NOT NULL DEFAULT 'main',
    last_scanned_at  TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_repositories_user_id ON repositories(user_id);

CREATE TABLE scans (
    id           BIGSERIAL PRIMARY KEY,
    repo_id      BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    commit_sha   VARCHAR(64),
    status       VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    started_at   TIMESTAMPTZ,
    finished_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_scans_repo_id ON scans(repo_id);

CREATE TABLE dependencies (
    id                 BIGSERIAL PRIMARY KEY,
    scan_id            BIGINT NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    name               VARCHAR(255) NOT NULL,
    version            VARCHAR(100) NOT NULL,
    risk_score         DOUBLE PRECISION,
    is_malicious_flag  BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX idx_dependencies_scan_id ON dependencies(scan_id);

CREATE TABLE vulnerabilities (
    id                   BIGSERIAL PRIMARY KEY,
    dependency_id        BIGINT NOT NULL REFERENCES dependencies(id) ON DELETE CASCADE,
    osv_id               VARCHAR(100) NOT NULL,
    cvss_score           DOUBLE PRECISION,
    description_source   TEXT
);
CREATE INDEX idx_vulnerabilities_dependency_id ON vulnerabilities(dependency_id);

CREATE TABLE sbom_documents (
    id           BIGSERIAL PRIMARY KEY,
    scan_id      BIGINT NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    format       VARCHAR(50) NOT NULL,
    storage_url  VARCHAR(500) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
