// Thin fetch wrapper + typed endpoint functions for the Matchly API gateway.
//
// All calls go through VITE_API_BASE (default http://localhost:8080/api/v1).
// The JWT access token is read from localStorage and attached as a Bearer
// header. On a 401 we clear the token and force a redirect to /login.

import type {
  AnalyticsOverview,
  Application,
  ApplicationCreateRequest,
  CandidateProfile,
  CandidateProfileUpdate,
  CurrentUser,
  FunnelResponse,
  InDemandSkill,
  InterviewCreateRequest,
  InterviewSet,
  Job,
  JobCreateRequest,
  LoginRequest,
  MatchScore,
  Page,
  ProblemDetail,
  RankedCandidate,
  RegisterRequest,
  Resume,
  ResumeUploadResponse,
  SkillGap,
  Stage,
  StageChangeRequest,
  TokenResponse,
} from '../types';

export const API_BASE: string =
  (import.meta.env.VITE_API_BASE as string | undefined) ??
  'http://localhost:8080/api/v1';

const TOKEN_KEY = 'matchly.accessToken';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

/** Error carrying the parsed problem detail and HTTP status. */
export class ApiError extends Error {
  readonly status: number;
  readonly problem?: ProblemDetail;

  constructor(status: number, message: string, problem?: ProblemDetail) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  /** When set, body is sent as-is (e.g. FormData) and no JSON content-type. */
  rawBody?: BodyInit;
  query?: Record<string, string | number | boolean | undefined>;
  /** Skip the Authorization header (used by login/register). */
  anonymous?: boolean;
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = new URL(
    API_BASE.replace(/\/$/, '') + (path.startsWith('/') ? path : `/${path}`),
    // base only used when API_BASE is relative; harmless otherwise
    window.location.origin,
  );
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null) {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.toString();
}

async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token && !opts.anonymous) {
    headers.Authorization = `Bearer ${token}`;
  }

  let body: BodyInit | undefined;
  if (opts.rawBody !== undefined) {
    body = opts.rawBody;
  } else if (opts.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(opts.body);
  }

  const res = await fetch(buildUrl(path, opts.query), {
    method: opts.method ?? 'GET',
    headers,
    body,
  });

  if (res.status === 401) {
    clearToken();
    if (window.location.pathname !== '/login') {
      window.location.assign('/login');
    }
    throw new ApiError(401, 'Unauthorized');
  }

  if (!res.ok) {
    let problem: ProblemDetail | undefined;
    try {
      problem = (await res.json()) as ProblemDetail;
    } catch {
      problem = undefined;
    }
    const message =
      problem?.detail || problem?.title || `Request failed (${res.status})`;
    throw new ApiError(res.status, message, problem);
  }

  // 204 / 202-with-no-body safety
  if (res.status === 204) {
    return undefined as T;
  }
  const text = await res.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}

// ---------------------------------------------------------------------------
// Auth
// ---------------------------------------------------------------------------

export const authApi = {
  login: (data: LoginRequest) =>
    request<TokenResponse>('/auth/login', {
      method: 'POST',
      body: data,
      anonymous: true,
    }),
  register: (data: RegisterRequest) =>
    request<TokenResponse>('/auth/register', {
      method: 'POST',
      body: data,
      anonymous: true,
    }),
  me: () => request<CurrentUser>('/auth/me'),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
};

// ---------------------------------------------------------------------------
// Candidate
// ---------------------------------------------------------------------------

export const candidateApi = {
  me: () => request<CandidateProfile>('/candidates/me'),
  updateMe: (data: CandidateProfileUpdate) =>
    request<CandidateProfile>('/candidates/me', { method: 'PUT', body: data }),
  uploadResume: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return request<ResumeUploadResponse>('/candidates/me/resume', {
      method: 'POST',
      rawBody: form,
    });
  },
  getResume: (id: string) => request<Resume>(`/resumes/${id}`),
  skillGap: (jobId: string) =>
    request<SkillGap>('/candidates/me/skill-gap', { query: { jobId } }),
};

// ---------------------------------------------------------------------------
// Jobs
// ---------------------------------------------------------------------------

export interface JobListParams {
  q?: string;
  location?: string;
  skills?: string;
  page?: number;
  size?: number;
}

export const jobApi = {
  list: (params: JobListParams = {}) =>
    request<Page<Job>>('/jobs', { query: { ...params } }),
  get: (id: string) => request<Job>(`/jobs/${id}`),
  create: (data: JobCreateRequest) =>
    request<Job>('/jobs', { method: 'POST', body: data }),
  publish: (id: string) =>
    request<Job>(`/jobs/${id}/publish`, { method: 'POST' }),
  close: (id: string) => request<Job>(`/jobs/${id}/close`, { method: 'POST' }),
};

// ---------------------------------------------------------------------------
// Applications
// ---------------------------------------------------------------------------

export const applicationApi = {
  apply: (data: ApplicationCreateRequest) =>
    request<Application>('/applications', { method: 'POST', body: data }),
  mine: () => request<Application[]>('/applications/me'),
  byJob: (jobId: string) =>
    request<Application[]>('/applications', { query: { jobId } }),
  changeStage: (id: string, data: StageChangeRequest) =>
    request<Application>(`/applications/${id}/stage`, {
      method: 'PATCH',
      body: data,
    }),
};

// ---------------------------------------------------------------------------
// Matching
// ---------------------------------------------------------------------------

export const matchingApi = {
  rankedForJob: (jobId: string, size = 20, page = 0) =>
    request<Page<RankedCandidate>>(`/matching/jobs/${jobId}/candidates`, {
      query: { size, page },
    }),
  score: (candidateId: string, jobId: string) =>
    request<MatchScore>('/matching/score', { query: { candidateId, jobId } }),
  skillGap: (candidateId: string, jobId: string) =>
    request<SkillGap>('/matching/skill-gap', {
      query: { candidateId, jobId },
    }),
};

// ---------------------------------------------------------------------------
// Interviews
// ---------------------------------------------------------------------------

export const interviewApi = {
  generate: (data: InterviewCreateRequest) =>
    request<InterviewSet>('/interviews', { method: 'POST', body: data }),
  get: (id: string) => request<InterviewSet>(`/interviews/${id}`),
  forPair: (candidateId: string, jobId: string) =>
    request<InterviewSet[]>('/interviews', {
      query: { candidateId, jobId },
    }),
};

// ---------------------------------------------------------------------------
// Analytics
// ---------------------------------------------------------------------------

export const analyticsApi = {
  overview: () => request<AnalyticsOverview>('/analytics/overview'),
  funnel: (jobId?: string) =>
    request<FunnelResponse>('/analytics/funnel', { query: { jobId } }),
  inDemandSkills: (window = '30d') =>
    request<InDemandSkill[]>('/analytics/skills/in-demand', {
      query: { window },
    }),
};

export type { Stage };
