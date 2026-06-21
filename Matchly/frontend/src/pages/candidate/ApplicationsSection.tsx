import { Link } from 'react-router-dom';
import { applicationApi } from '../../api/client';
import { useAsync } from '../../hooks/useAsync';
import { Card } from '../../components/Card';
import { StageBadge } from '../../components/Badge';
import { Spinner } from '../../components/Spinner';
import { ErrorMessage } from '../../components/ErrorMessage';

function formatDate(value?: string): string {
  if (!value) return '—';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString();
}

export function ApplicationsSection() {
  const { data, loading, error } = useAsync(() => applicationApi.mine(), []);

  if (loading) return <Spinner label="Loading applications…" />;
  if (error != null) return <ErrorMessage error={error} />;

  const apps = data ?? [];

  if (apps.length === 0) {
    return (
      <Card>
        <p className="text-sm text-gray-500">
          You have not applied to any jobs yet.{' '}
          <Link to="/jobs" className="text-brand-600 hover:underline">
            Browse jobs
          </Link>
          .
        </p>
      </Card>
    );
  }

  return (
    <Card className="overflow-x-auto p-0">
      <table className="w-full text-sm">
        <thead className="border-b border-gray-200 bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500">
          <tr>
            <th className="px-5 py-3">Job</th>
            <th className="px-5 py-3">Stage</th>
            <th className="px-5 py-3">Applied</th>
            <th className="px-5 py-3">Updated</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {apps.map((app) => (
            <tr key={app.id} className="hover:bg-gray-50">
              <td className="px-5 py-3">
                <Link
                  to={`/jobs/${app.job.id}`}
                  className="font-medium text-gray-900 hover:text-brand-700"
                >
                  {app.job.title}
                </Link>
              </td>
              <td className="px-5 py-3">
                <StageBadge stage={app.currentStage} />
              </td>
              <td className="px-5 py-3 text-gray-500">
                {formatDate(app.appliedAt)}
              </td>
              <td className="px-5 py-3 text-gray-500">
                {formatDate(app.lastUpdated)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}
