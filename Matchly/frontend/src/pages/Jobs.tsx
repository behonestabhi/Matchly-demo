import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { jobApi, type JobListParams } from '../api/client';
import { useAsync } from '../hooks/useAsync';
import { Card } from '../components/Card';
import { Pill } from '../components/Badge';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';

export function Jobs() {
  const [params, setParams] = useState<JobListParams>({ page: 0, size: 20 });
  const [q, setQ] = useState('');
  const [location, setLocation] = useState('');

  const { data, loading, error } = useAsync(
    () => jobApi.list(params),
    [params.q, params.location, params.page, params.size],
  );

  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    setParams((p) => ({
      ...p,
      q: q || undefined,
      location: location || undefined,
      page: 0,
    }));
  };

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Open roles</h1>
          <p className="mt-1 text-sm text-gray-500">
            Search and apply to jobs across the platform.
          </p>
        </div>
      </div>

      <form
        onSubmit={onSearch}
        className="mb-6 flex flex-wrap items-end gap-3 rounded-xl border border-gray-200 bg-white p-4"
      >
        <div className="min-w-[200px] flex-1">
          <label className="label" htmlFor="q">
            Keyword
          </label>
          <input
            id="q"
            className="input"
            placeholder="e.g. Backend Engineer"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <div className="min-w-[160px]">
          <label className="label" htmlFor="loc">
            Location
          </label>
          <input
            id="loc"
            className="input"
            placeholder="Remote, NYC…"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
          />
        </div>
        <button type="submit" className="btn-primary">
          Search
        </button>
      </form>

      {loading && <Spinner label="Loading jobs…" />}
      {error != null && <ErrorMessage error={error} />}

      {data && (
        <>
          {data.content.length === 0 ? (
            <Card>
              <p className="text-sm text-gray-500">No jobs found.</p>
            </Card>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2">
              {data.content.map((job) => (
                <Link key={job.id} to={`/jobs/${job.id}`} className="group">
                  <Card className="h-full transition group-hover:border-brand-300 group-hover:shadow-md">
                    <div className="flex items-start justify-between gap-2">
                      <h2 className="font-semibold text-gray-900 group-hover:text-brand-700">
                        {job.title}
                      </h2>
                      {job.status && (
                        <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500">
                          {job.status}
                        </span>
                      )}
                    </div>
                    {job.location && (
                      <p className="mt-1 text-sm text-gray-500">{job.location}</p>
                    )}
                    <p className="mt-2 line-clamp-2 text-sm text-gray-600">
                      {job.description}
                    </p>
                    {job.requiredSkills && job.requiredSkills.length > 0 && (
                      <div className="mt-3 flex flex-wrap gap-1.5">
                        {job.requiredSkills.slice(0, 5).map((s) => (
                          <Pill key={s.skillName}>{s.skillName}</Pill>
                        ))}
                      </div>
                    )}
                  </Card>
                </Link>
              ))}
            </div>
          )}

          {data.page.totalPages > 1 && (
            <div className="mt-6 flex items-center justify-center gap-3">
              <button
                className="btn-secondary"
                disabled={data.page.number <= 0}
                onClick={() =>
                  setParams((p) => ({ ...p, page: (p.page ?? 0) - 1 }))
                }
              >
                Previous
              </button>
              <span className="text-sm text-gray-500">
                Page {data.page.number + 1} of {data.page.totalPages}
              </span>
              <button
                className="btn-secondary"
                disabled={data.page.number + 1 >= data.page.totalPages}
                onClick={() =>
                  setParams((p) => ({ ...p, page: (p.page ?? 0) + 1 }))
                }
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
