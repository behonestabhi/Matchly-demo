import { useState, type FormEvent } from 'react';
import { candidateApi } from '../../api/client';
import { Card } from '../../components/Card';
import { Pill } from '../../components/Badge';
import { Spinner } from '../../components/Spinner';
import { ErrorMessage } from '../../components/ErrorMessage';
import type { SkillGap } from '../../types';

export function SkillGapSection({
  initialJobId,
}: {
  initialJobId: string | null;
}) {
  const [jobId, setJobId] = useState(initialJobId ?? '');
  const [data, setData] = useState<SkillGap | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const run = async (e?: FormEvent) => {
    e?.preventDefault();
    if (!jobId.trim()) return;
    setLoading(true);
    setError(null);
    setData(null);
    try {
      const result = await candidateApi.skillGap(jobId.trim());
      setData(result);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl space-y-6">
      <Card>
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-400">
          Skill gap for a job
        </h2>
        <p className="mb-4 text-sm text-gray-500">
          Enter a job ID to see which skills you are missing and a suggested
          roadmap.
        </p>
        <form onSubmit={run} className="flex gap-3">
          <input
            className="input"
            placeholder="Job ID"
            value={jobId}
            onChange={(e) => setJobId(e.target.value)}
          />
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Checking…' : 'Check'}
          </button>
        </form>
      </Card>

      {loading && <Spinner label="Analyzing skill gap…" />}
      {error != null && <ErrorMessage error={error} />}

      {data && (
        <Card className="space-y-5">
          {data.matchedSkills && data.matchedSkills.length > 0 && (
            <div>
              <p className="mb-2 text-xs font-semibold uppercase text-gray-400">
                Skills you already have
              </p>
              <div className="flex flex-wrap gap-1.5">
                {data.matchedSkills.map((s) => (
                  <Pill key={s}>{s}</Pill>
                ))}
              </div>
            </div>
          )}

          <div>
            <p className="mb-2 text-xs font-semibold uppercase text-gray-400">
              Missing skills
            </p>
            {data.missingSkills.length === 0 ? (
              <p className="text-sm text-emerald-600">
                No gaps — you meet the required skills.
              </p>
            ) : (
              <div className="flex flex-wrap gap-1.5">
                {data.missingSkills.map((s) => (
                  <span
                    key={s}
                    className="inline-flex items-center rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-medium text-rose-700"
                  >
                    {s}
                  </span>
                ))}
              </div>
            )}
          </div>

          {data.roadmap && data.roadmap.length > 0 && (
            <div>
              <p className="mb-2 text-xs font-semibold uppercase text-gray-400">
                Suggested roadmap
              </p>
              <ol className="list-decimal space-y-1 pl-5 text-sm text-gray-700">
                {data.roadmap.map((step, i) => (
                  <li key={i}>{step}</li>
                ))}
              </ol>
            </div>
          )}
        </Card>
      )}
    </div>
  );
}
