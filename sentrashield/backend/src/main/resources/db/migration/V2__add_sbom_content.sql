ALTER TABLE sbom_documents ADD COLUMN content TEXT NOT NULL;
CREATE INDEX idx_sbom_documents_scan_id ON sbom_documents(scan_id);
