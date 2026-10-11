from pydantic import BaseModel, ConfigDict, Field


class PackageJson(BaseModel):
    model_config = ConfigDict(extra="allow")
    dependencies: dict[str, str] = Field(default_factory=dict)
    devDependencies: dict[str, str] = Field(default_factory=dict)


class VulnerabilityResult(BaseModel):
    osv_id: str
    summary: str = ""


class DependencyResult(BaseModel):
    name: str
    version: str
    vulnerabilities: list[VulnerabilityResult] = Field(default_factory=list)


class ScanResponse(BaseModel):
    dependencies: list[DependencyResult]
