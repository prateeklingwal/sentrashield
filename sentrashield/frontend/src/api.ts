export type Repo = {
  id: number;
  githubRepoUrl: string;
  defaultBranch: string;
  lastScannedAt: string | null;
  createdAt: string;
};
export type DependencySummary = { name: string; version: string; vulnerabilityCount: number };
export type ScanSummary = {
  scanId: number;
  repoUrl: string;
  sbomUrl: string;
  dependencies: DependencySummary[];
};
export type Session = { token: string; email: string };

export class ApiError extends Error {
  status: number;
  constructor(status: number) {
    super(`HTTP ${status}`);
    this.status = status;
  }
}

const KEY = 'sentrashield.session';

export function loadSession(): Session | null {
  try {
    return JSON.parse(localStorage.getItem(KEY) || 'null');
  } catch {
    return null;
  }
}

export function saveSession(s: Session | null) {
  if (s) localStorage.setItem(KEY, JSON.stringify(s));
  else localStorage.removeItem(KEY);
}

let onUnauthorized: () => void = () => {};
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn;
}

async function request(path: string, init: RequestInit = {}, auth = true): Promise<Response> {
  const headers = new Headers(init.headers);
  if (init.body) headers.set('Content-Type', 'application/json');
  const session = loadSession();
  if (auth && session) headers.set('Authorization', `Bearer ${session.token}`);
  const res = await fetch(path, { ...init, headers });
  // An expired or invalid token on a protected call sends the user back to the login screen.
  if (res.status === 401 && auth) onUnauthorized();
  if (!res.ok) throw new ApiError(res.status);
  return res;
}

export async function authenticate(
  mode: 'login' | 'register',
  email: string,
  password: string,
  name: string,
): Promise<Session> {
  const body = mode === 'login' ? { email, password } : { name, email, password };
  const res = await request(`/api/auth/${mode}`, { method: 'POST', body: JSON.stringify(body) }, false);
  const data = await res.json();
  return { token: data.token, email: data.email };
}

export async function listRepos(): Promise<Repo[]> {
  return (await request('/api/repositories')).json();
}

export async function addRepo(githubRepoUrl: string): Promise<Repo> {
  const res = await request('/api/repositories', {
    method: 'POST',
    body: JSON.stringify({ githubRepoUrl }),
  });
  return res.json();
}

export async function runScan(repoId: number, packageJson: unknown): Promise<ScanSummary> {
  const res = await request('/api/scans', {
    method: 'POST',
    body: JSON.stringify({ repoId, packageJson }),
  });
  return res.json();
}

// The SBOM endpoint needs the Authorization header, so a plain <a href> would get a 401.
export async function fetchSbom(sbomUrl: string): Promise<string> {
  return (await request(sbomUrl)).text();
}
