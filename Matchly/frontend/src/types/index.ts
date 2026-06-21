// TypeScript types mirroring the DTOs documented in docs/API_CONTRACTS.md.

export type Role = 'CANDIDATE' | 'RECRUITER' | 'HIRING_MANAGER' | 'ADMIN';

// ---------- Generic ----------

/** Spring-style page wrapper: { content, page }. */
export interface Page<T> {
  content: T[];
  page: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

/** RFC7807-ish problem detail returned on errors. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  correlationId?: string;
  errors?: unknown[];
}

// ---------- Auth ----------

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName?: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface CurrentUser {
  id: string;
  email: string;
  fullName?: string;
  roles: Role[];
}

// ---------- Candidate ----------

export interface CandidateProfile {
  id: string;
  email: string;
  fullName?: string;
  headline?: string;
  location?: string;
  links?: string[];
  skills?: string[];
  totalExpYrs?: number;
}

export interface CandidateProfileUpdate {
  fullName?: string;
  headline?: string;
  location?: string;
  links?: string[];
}

export type ParseStatus = 'PENDING' | 'PARSING' | 'PARSED' | 'FAILED';

export interface ResumeUploadResponse {
  resumeId: string;
  parseStatus: ParseStatus;
  poll?: string;
}

export interface ResumeExperience {
  company?: string;
  title?: string;
  startDate?: string;
  endDate?: string;
  description?: string;
}

export interface ResumeEducation {
  institution?: string;
  degree?: string;
  field?: string;
  year?: string;
}

export interface Resume {
  id: string;
  parseStatus: ParseStatus;
  candidate?: {
    fullName?: string;
    email?: string;
    totalExpYrs?: number;
  };
  skills?: string[];
  experiences?: ResumeExperience[];
  education?: ResumeEducation[];
  projects?: string[];
  certifications?: string[];
}

// ---------- Jobs ----------

export type JobStatus = 'DRAFT' | 'OPEN' | 'CLOSED';
export type EducationLevel =
  | 'HIGH_SCHOOL'
  | 'ASSOCIATE'
  | 'BACHELOR'
  | 'MASTER'
  | 'PHD';

export interface RequiredSkill {
  skillName: string;
  weight: number;
  required: boolean;
}

export interface Job {
  id: string;
  title: string;
  description: string;
  status?: JobStatus;
  location?: string;
  minExpYrs?: number;
  maxExpYrs?: number;
  educationLevel?: EducationLevel;
  requiredSkills?: RequiredSkill[];
  createdAt?: string;
}

export interface JobCreateRequest {
  title: string;
  description: string;
  location?: string;
  minExpYrs?: number;
  maxExpYrs?: number;
  educationLevel?: EducationLevel;
  requiredSkills: RequiredSkill[];
}

// ---------- Applications ----------

export type Stage =
  | 'APPLIED'
  | 'SCREENING'
  | 'SHORTLISTED'
  | 'INTERVIEW'
  | 'OFFER'
  | 'REJECTED';

export const STAGES: Stage[] = [
  'APPLIED',
  'SCREENING',
  'SHORTLISTED',
  'INTERVIEW',
  'OFFER',
  'REJECTED',
];

export interface Application {
  id: string;
  job: { id: string; title: string };
  candidate?: { id: string; name?: string };
  currentStage: Stage;
  appliedAt?: string;
  lastUpdated?: string;
}

export interface ApplicationCreateRequest {
  jobId: string;
}

export interface StageChangeRequest {
  toStage: Stage;
  reason?: string;
}

// ---------- Matching ----------

export interface ScoreBreakdown {
  semantic: number;
  skillOverlap: number;
  experienceFit: number;
  educationFit: number;
}

export interface MatchScore {
  candidateId: string;
  jobId: string;
  finalScore: number;
  breakdown: ScoreBreakdown;
  modelVersion?: string;
  scoredAt?: string;
}

export interface RankedCandidate {
  candidateId: string;
  name?: string;
  finalScore: number;
  topSkills?: string[];
}

export interface SkillGap {
  candidateId?: string;
  jobId?: string;
  missingSkills: string[];
  matchedSkills?: string[];
  roadmap?: string[];
}

// ---------- Interviews ----------

export type InterviewStatus = 'GENERATING' | 'READY' | 'FAILED';
export type QuestionCategory = 'TECHNICAL' | 'BEHAVIORAL' | 'SCENARIO';
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface InterviewQuestion {
  category: QuestionCategory;
  question: string;
  difficulty?: Difficulty;
  suggestedAnswer?: string;
}

export interface InterviewCreateRequest {
  candidateId: string;
  jobId: string;
}

export interface InterviewSet {
  id: string;
  status: InterviewStatus;
  llmModel?: string;
  poll?: string;
  questions?: InterviewQuestion[];
}

// ---------- Analytics ----------

export interface AnalyticsOverview {
  applicationsReceived: number;
  shortlisted: number;
  hires: number;
  avgTimeToHireDays: number;
}

export interface FunnelStage {
  stage: Stage;
  count: number;
}

export interface FunnelResponse {
  jobId?: string;
  stages: FunnelStage[];
}

export interface InDemandSkill {
  skill: string;
  count: number;
}
