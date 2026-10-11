import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react';
import { ApiError, addRepo, fetchSbom, listRepos, runScan, type Repo, type ScanSummary } from './api';
import { field, primary } from './ui';

type Advisory = { id: string; description: string };

const SAMPLE = JSON.stringify(
  { name: 'demo-app', dependencies: { lodash: '4.17.4', express: '4.18.2' } },
  null,
  2,
);

function describe(e: unknown): string {
  if (!(e instanceof ApiError)) return 'Could not reach the server.';
  if (e.status === 404) return 'Repository not found.';
  if (e.status === 502) return 'The scan service did not respond. Try again in a moment.';
  if (e.status === 400) return 'The request was rejected. Check the repository URL and package.json.';
  return `Request failed (${e.status}).`;
}

// Severity is judged by advisory count: the backend does not store CVSS scores yet.
function tone(count: number): string {
  if (count === 0) return 'bg-ok/15 text-ok';
  if (count < 3) return 'bg-warn/15 text-warn';
  return 'bg-bad/15 text-bad';
}

// The scan response only carries counts; advisory ids and summaries come from the SBOM.
function groupAdvisories(sbomText: string): Record<string, Advisory[]> {
  const out: Record<string, Advisory[]> = {};
  try {
    const bom = JSON.parse(sbomText);
    for (const v of bom.vulnerabilities ?? []) {
      for (const a of v.affects ?? []) {
        (out[a.ref] ??= []).push({ id: v.id, description: v.description ?? '' });
      }
    }
  } catch {
    // A malformed SBOM only means no advisory details are shown.
  }
  return out;
}

export default function Dashboard() {
  const [repos, setRepos] = useState<Repo[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [newUrl, setNewUrl] = useState('');
  const [pkg, setPkg] = useState(SAMPLE);
  const [result, setResult] = useState<ScanSummary | null>(null);
  const [sbomText, setSbomText] = useState('');
  const [advisories, setAdvisories] = useState<Record<string, Advisory[]>>({});
  const [open, setOpen] = useState<string | null>(null);
  const [showSbom, setShowSbom] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [error, setError] = useState('');

  const refresh = () => listRepos().then(setRepos).catch((e) => setError(describe(e)));
  useEffect(() => {
    refresh();
  }, []);

  const repo = repos.find((r) => r.id === selected) ?? null;

  const stats = useMemo(() => {
    const deps = result?.dependencies ?? [];
    return {
      total: deps.length,
      vulnerable: deps.filter((d) => d.vulnerabilityCount > 0).length,
      advisories: deps.reduce((n, d) => n + d.vulnerabilityCount, 0),
    };
  }, [result]);

  const sorted = useMemo(
    () => [...(result?.dependencies ?? [])].sort((a, b) => b.vulnerabilityCount - a.vulnerabilityCount),
    [result],
  );

  const prettySbom = useMemo(() => {
    try {
      return JSON.stringify(JSON.parse(sbomText), null, 2);
    } catch {
      return sbomText;
    }
  }, [sbomText]);

  function select(id: number) {
    setSelected(id);
    setResult(null);
    setSbomText('');
    setAdvisories({});
    setOpen(null);
    setShowSbom(false);
    setError('');
  }

  async function onAdd(e: FormEvent) {
    e.preventDefault();
    if (!newUrl.trim()) return;
    setError('');
    try {
      const created = await addRepo(newUrl.trim());
      setNewUrl('');
      await refresh();
      select(created.id);
    } catch (err) {
      setError(describe(err));
    }
  }

  async function onFile(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) setPkg(await file.text());
    e.target.value = '';
  }

  async function onScan() {
    if (!repo) return;
    let parsed: unknown;
    try {
      parsed = JSON.parse(pkg);
    } catch {
      setError('package.json is not valid JSON.');
      return;
    }
    setScanning(true);
    setError('');
    setResult(null);
    setSbomText('');
    setAdvisories({});
    setOpen(null);
    setShowSbom(false);
    try {
      const summary = await runScan(repo.id, parsed);
      setResult(summary);
      refresh();
      try {
        const text = await fetchSbom(summary.sbomUrl);
        setSbomText(text);
        setAdvisories(groupAdvisories(text));
      } catch {
        // Counts are still shown; only advisory details and the SBOM are unavailable.
      }
    } catch (err) {
      setError(describe(err));
    } finally {
      setScanning(false);
    }
  }

  function download() {
    if (!result) return;
    const url = URL.createObjectURL(new Blob([sbomText], { type: 'application/json' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `sbom-scan-${result.scanId}.cdx.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="grid gap-8 md:grid-cols-[18rem_1fr]">
      <aside>
        <h2 className="mb-3 font-medium">Repositories</h2>
        {repos.length === 0 ? (
          <p className="mb-3 text-sm text-muted">No repositories yet. Add one below.</p>
        ) : (
          <ul className="mb-4 space-y-1">
            {repos.map((r) => (
              <li key={r.id}>
                <button
                  onClick={() => select(r.id)}
                  className={`w-full truncate rounded-md px-3 py-2 text-left text-sm ${
                    r.id === selected ? 'bg-panel text-accent' : 'text-text hover:bg-panel'
                  }`}
                >
                  {r.githubRepoUrl.replace(/^https?:\/\/(www\.)?github\.com\//, '')}
                </button>
              </li>
            ))}
          </ul>
        )}
        <form onSubmit={onAdd} className="space-y-2">
          <input
            className={field}
            placeholder="https://github.com/owner/repo"
            value={newUrl}
            onChange={(e) => setNewUrl(e.target.value)}
          />
          <button className={`${primary} w-full`}>Add repository</button>
        </form>
      </aside>

      <main>
        {!repo ? (
          <p className="text-muted">Select a repository to scan its dependencies.</p>
        ) : (
          <>
            <h2 className="text-lg font-medium">{repo.githubRepoUrl}</h2>
            <p className="mb-4 text-sm text-muted">
              {repo.lastScannedAt
                ? `Last scanned ${new Date(repo.lastScannedAt).toLocaleString()}`
                : 'Never scanned'}
            </p>

            <textarea
              className={`${field} h-44 font-mono`}
              value={pkg}
              onChange={(e) => setPkg(e.target.value)}
              spellCheck={false}
              aria-label="package.json contents"
            />
            <div className="mt-2 flex items-center gap-4">
              <button className={primary} onClick={onScan} disabled={scanning}>
                {scanning ? 'Scanning…' : 'Scan now'}
              </button>
              <label className="cursor-pointer text-sm text-accent underline">
                Load a package.json file
                <input type="file" accept=".json,application/json" className="hidden" onChange={onFile} />
              </label>
            </div>
            {error && <p className="mt-3 text-sm text-bad">{error}</p>}

            {result && (
              <section className="mt-8">
                <div className="mb-4 flex gap-10 border-b border-line pb-4">
                  <div>
                    <div className="text-2xl font-semibold">{stats.total}</div>
                    <div className="text-sm text-muted">dependencies</div>
                  </div>
                  <div>
                    <div className="text-2xl font-semibold">{stats.vulnerable}</div>
                    <div className="text-sm text-muted">with advisories</div>
                  </div>
                  <div>
                    <div className="text-2xl font-semibold">{stats.advisories}</div>
                    <div className="text-sm text-muted">advisories in total</div>
                  </div>
                </div>

                <ul className="divide-y divide-line">
                  {sorted.map((d) => {
                    const key = `pkg:npm/${d.name}@${d.version}`;
                    const list = advisories[key] ?? [];
                    const isOpen = open === key;
                    return (
                      <li key={key} className="py-3">
                        <div className="flex items-center justify-between gap-3">
                          <span className="truncate">
                            {d.name} <span className="text-muted">{d.version}</span>
                          </span>
                          <span className="flex shrink-0 items-center gap-3">
                            <span className={`rounded px-2 py-0.5 text-xs ${tone(d.vulnerabilityCount)}`}>
                              {d.vulnerabilityCount === 0
                                ? 'No advisories'
                                : `${d.vulnerabilityCount} advisor${d.vulnerabilityCount === 1 ? 'y' : 'ies'}`}
                            </span>
                            {list.length > 0 && (
                              <button
                                className="text-xs text-accent underline"
                                onClick={() => setOpen(isOpen ? null : key)}
                              >
                                {isOpen ? 'Hide details' : 'Show details'}
                              </button>
                            )}
                          </span>
                        </div>
                        {isOpen && (
                          <ul className="mt-2 space-y-2 pl-3 text-sm">
                            {list.map((a) => (
                              <li key={a.id}>
                                <a
                                  className="text-accent underline"
                                  href={`https://osv.dev/vulnerability/${a.id}`}
                                  target="_blank"
                                  rel="noreferrer"
                                >
                                  {a.id}
                                </a>
                                <span className="ml-2 text-muted">{a.description}</span>
                              </li>
                            ))}
                          </ul>
                        )}
                      </li>
                    );
                  })}
                </ul>
                <p className="mt-3 text-xs text-muted">
                  Severity reflects the number of advisories. CVSS scores are not available yet.
                </p>

                {sbomText && (
                  <div className="mt-6">
                    <div className="flex gap-4 text-sm">
                      <button className="text-accent underline" onClick={() => setShowSbom(!showSbom)}>
                        {showSbom ? 'Hide SBOM' : 'View SBOM'}
                      </button>
                      <button className="text-accent underline" onClick={download}>
                        Download SBOM (CycloneDX JSON)
                      </button>
                    </div>
                    {showSbom && (
                      <pre className="mt-3 max-h-96 overflow-auto rounded-md border border-line bg-panel p-3 text-xs">
                        {prettySbom}
                      </pre>
                    )}
                  </div>
                )}
              </section>
            )}
          </>
        )}
      </main>
    </div>
  );
}
