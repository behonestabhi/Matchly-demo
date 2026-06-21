import { useState, type FormEvent } from 'react';
import { analyticsApi } from '../api/client';
import { useAsync } from '../hooks/useAsync';
import { Card, StatCard } from '../components/Card';
import { StageBadge } from '../components/Badge';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';

export function Analytics() {
  const overview = useAsync(() => analyticsApi.overview(), []);
  const skills = useAsync(() => analyticsApi.inDemandSkills('30d'), []);

  const [jobId, setJobId] = useState('');
  const [funnelJob, setFunnelJob] = useState<string | undefined>(undefined);
  const funnel = useAsync(
    () => analyticsApi.funnel(funnelJob),
    [funnelJob],
  );

  const applyFunnel = (e: FormEvent) => {
    e.preventDefault();
    setFunnelJob(jobId.trim() || undefined);
  };

  const maxSkill = Math.max(1, ...(skills.data ?? []).map((s) => s.count));
  const maxFunnel = Math.max(
    1,
    ...(funnel.data?.stages ?? []).map((s) => s.count),
  );

  return (
    <div>
      <h1 className="mb-1 text-2xl font-semibold text-gray-900">Analytics</h1>
      <p className="mb-6 text-sm text-gray-500">
        Hiring funnel and demand insights.
      </p>

      {/* Overview cards */}
      <section className="mb-8">
        {overview.loading && <Spinner label="Loading overview…" />}
        {overview.error != null && <ErrorMessage error={overview.error} />}
        {overview.data && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
              label="Applications"
              value={overview.data.applicationsReceived}
            />
            <StatCard label="Shortlisted" value={overview.data.shortlisted} />
            <StatCard label="Hires" value={overview.data.hires} />
            <StatCard
              label="Avg time to hire"
              value={`${overview.data.avgTimeToHireDays}d`}
            />
          </div>
        )}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Funnel */}
        <Card>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-400">
            Funnel
          </h2>
          <form onSubmit={applyFunnel} className="mb-4 flex gap-2">
            <input
              className="input"
              placeholder="Job ID (optional)"
              value={jobId}
              onChange={(e) => setJobId(e.target.value)}
            />
            <button type="submit" className="btn-secondary">
              Apply
            </button>
          </form>

          {funnel.loading && <Spinner label="Loading funnel…" />}
          {funnel.error != null && <ErrorMessage error={funnel.error} />}
          {funnel.data && (
            <div className="space-y-2">
              {funnel.data.stages.length === 0 && (
                <p className="text-sm text-gray-400">No funnel data.</p>
              )}
              {funnel.data.stages.map((s) => (
                <div key={s.stage} className="flex items-center gap-3">
                  <div className="w-28 flex-shrink-0">
                    <StageBadge stage={s.stage} />
                  </div>
                  <div className="h-6 flex-1 overflow-hidden rounded bg-gray-100">
                    <div
                      className="flex h-full items-center justify-end rounded bg-brand-500 px-2 text-xs font-medium text-white"
                      style={{ width: `${(s.count / maxFunnel) * 100}%` }}
                    >
                      {s.count}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* In-demand skills */}
        <Card>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-400">
            In-demand skills (30d)
          </h2>
          {skills.loading && <Spinner label="Loading skills…" />}
          {skills.error != null && <ErrorMessage error={skills.error} />}
          {skills.data && (
            <div className="space-y-2">
              {skills.data.length === 0 && (
                <p className="text-sm text-gray-400">No data.</p>
              )}
              {skills.data.map((s) => (
                <div key={s.skill} className="flex items-center gap-3">
                  <span className="w-28 flex-shrink-0 truncate text-sm text-gray-700">
                    {s.skill}
                  </span>
                  <div className="h-5 flex-1 overflow-hidden rounded bg-gray-100">
                    <div
                      className="h-full rounded bg-emerald-500"
                      style={{ width: `${(s.count / maxSkill) * 100}%` }}
                    />
                  </div>
                  <span className="w-8 text-right text-xs text-gray-500">
                    {s.count}
                  </span>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
