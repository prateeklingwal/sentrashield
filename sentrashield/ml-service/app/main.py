import re

import httpx
from fastapi import FastAPI

from app.models import PackageJson, ScanResponse, DependencyResult, VulnerabilityResult

app = FastAPI(title="SentraShield ML/Scan Service")

OSV_QUERYBATCH_URL = "https://api.osv.dev/v1/querybatch"
OSV_VULN_URL = "https://api.osv.dev/v1/vulns/{}"


@app.get("/health")
def health():
    return {"status": "ok", "service": "ml-service"}


def _normalize_version(raw: str) -> str:
    """Strip semver range operators so OSV gets a bare version.
    Simplification for the prototype -- a real implementation would
    resolve exact versions from package-lock.json instead."""
    cleaned = raw.strip().split(" ")[0].split("||")[0].strip()
    return re.sub(r"^[\^~>=<]+", "", cleaned)


@app.post("/scan", response_model=ScanResponse)
def scan(package_json: PackageJson):
    deps = {**package_json.dependencies, **package_json.devDependencies}
    names_versions = [(name, _normalize_version(v)) for name, v in deps.items()]

    batch_results: list[dict] = []
    if names_versions:
        queries = [
            {"package": {"name": name, "ecosystem": "npm"}, "version": version}
            for name, version in names_versions
        ]
        resp = httpx.post(OSV_QUERYBATCH_URL, json={"queries": queries}, timeout=30.0)
        resp.raise_for_status()
        batch_results = resp.json().get("results", [])

    # Unique vulnerability ids across every queried package
    all_ids = {v["id"] for result in batch_results for v in result.get("vulns", [])}

    # Hydrate each id with its full record (we only need the summary for now)
    summaries: dict[str, str] = {}
    for vuln_id in all_ids:
        r = httpx.get(OSV_VULN_URL.format(vuln_id), timeout=30.0)
        if r.status_code == 200:
            summaries[vuln_id] = r.json().get("summary", "")

    dependencies_out = []
    for (name, version), result in zip(names_versions, batch_results):
        vulns = [
            VulnerabilityResult(osv_id=v["id"], summary=summaries.get(v["id"], ""))
            for v in result.get("vulns", [])
        ]
        dependencies_out.append(DependencyResult(name=name, version=version, vulnerabilities=vulns))

    return ScanResponse(dependencies=dependencies_out)
