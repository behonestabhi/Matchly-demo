import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { applicationApi, jobApi, ApiError } from '../api/client';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../auth/AuthContext';
import { Card } from '../components/Card';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';

export function JobDetail() {
  const { id = '' } = useParams();
  const { hasRole } = useAuth();
  const { data: job, loading, error } = useAsync(() => jobApi.get(id), [id]);

  const isCandidate = hasRole('CANDIDATE');
  const isRecruiter = hasRole('RECRUITER', 'HIRING_MANAGER', 'ADMIN');

  const [applyState, setApplyState] = useState<
    'idle' | 'applying' | 'done' | 'error'
  >('idle');
  const [applyError, setApplyError] = useState<unknown>(null);

  const apply = async () => {
    setApplyState('applying');
    setApplyError(null);
    try {
      await applicationApi.apply({ jobId: id });
      setApplyState('done');
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        // Already applied — treat as success-ish.
        setApplyState('done');
      } else {
        setApplyError(err);
        setApplyState('error');
      }
    }
  };

  if (loading) return <Spinner label="Loading job…" />;
  if (error != null) return <ErrorMessage error={error} />;
  if (!job) return null;

  return (
    <div className="max-w-3xl">
      <Link to="/jobs" className="text-sm text-brand-600 hover:underline">
        ← Back to jobs
      </Link>

      <div className="mt-3 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">{job.title}</h1>
          <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-gray-500">
            {job.location && <span>{job.location}</span>}
            {job.status && (
              <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium">
                {job.status}
              </span>
            )}
            {(job.minExpYrs != null || job.maxExpYrs != null) && (
              <span>
                {job.minExpYrs ?? 0}–{job.maxExpYrs ?? '∞'} yrs experience
              </span>
            )}
            {job.educationLevel && <span>{job.educationLevel}</span>}
          </div>
        </div>

        {isCandidate && (
          <div className="text-right">
            <button
              className="btn-primary"
              disabled={applyState === 'applying' || applyState === 'done'}
              onClick={apply}
            >
              {applyState === 'done'
                ? 'Applied ✓'
                : applyState === 'applying'
                  ? 'Applying…'
                  : 'Apply'}
            </button>
            <div className="mt-2">
              <Link
                to={`/candidate?skillGapJob=${job.id}`}
                className="text-xs text-brand-600 hover:underline"
              >
                Check my skill gap
              </Link>
            </div>
          </div>
        )}

        {isRecruiter && (
          <div className="flex flex-col items-end gap-2 text-sm">
            <Link
              to={`/recruiter/jobs/${job.id}/candidates`}
              className="text-brand-600 hover:underline"
            >
              Ranked candidates →
            </Link>
            <Link
              to={`/recruiter/jobs/${job.id}/pipeline`}
              className="text-brand-600 hover:underline"
            >
              Pipeline board →
            </Link>
          </div>
        )}
      </div>

      {applyState === 'error' && (
        <div className="mt-4">
          <ErrorMessage error={applyError} />
        </div>
      )}

      <Card className="mt-6">
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-400">
          Description
        </h2>
        <p className="whitespace-pre-line text-sm text-gray-700">
          {job.description}
        </p>
      </Card>

      {job.requiredSkills && job.requiredSkills.length > 0 && (
        <Card className="mt-4">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-400">
            Required skills
          </h2>
          <div className="flex flex-wrap gap-2">
            {job.requiredSkills.map((s) => (
              <span
                key={s.skillName}
                className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${
                  s.required
                    ? 'bg-brand-50 text-brand-700'
                    : 'bg-gray-100 text-gray-600'
                }`}
              >
                {s.skillName}
                {s.required && <span className="text-[10px]">required</span>}
              </span>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
