import { useEffect, useState, type FormEvent } from 'react';
import { candidateApi } from '../../api/client';
import { useAsync } from '../../hooks/useAsync';
import { Card } from '../../components/Card';
import { Pill } from '../../components/Badge';
import { Spinner } from '../../components/Spinner';
import { ErrorMessage } from '../../components/ErrorMessage';
import type { CandidateProfileUpdate } from '../../types';

export function ProfileSection() {
  const { data, loading, error, reload } = useAsync(
    () => candidateApi.me(),
    [],
  );

  const [form, setForm] = useState<CandidateProfileUpdate>({});
  const [linksText, setLinksText] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (data) {
      setForm({
        fullName: data.fullName ?? '',
        headline: data.headline ?? '',
        location: data.location ?? '',
        links: data.links ?? [],
      });
      setLinksText((data.links ?? []).join(', '));
    }
  }, [data]);

  const onSave = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setSaveError(null);
    setSaved(false);
    try {
      const links = linksText
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      await candidateApi.updateMe({ ...form, links });
      setSaved(true);
      reload();
    } catch (err) {
      setSaveError(err);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Spinner label="Loading profile…" />;
  if (error != null) return <ErrorMessage error={error} />;

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <Card className="lg:col-span-2">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-gray-400">
          Edit profile
        </h2>
        <form onSubmit={onSave} className="space-y-4">
          {saveError != null && <ErrorMessage error={saveError} />}
          {saved && (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-700">
              Profile saved.
            </div>
          )}
          <div>
            <label className="label">Full name</label>
            <input
              className="input"
              value={form.fullName ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, fullName: e.target.value }))}
            />
          </div>
          <div>
            <label className="label">Headline</label>
            <input
              className="input"
              placeholder="Senior Backend Engineer"
              value={form.headline ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, headline: e.target.value }))}
            />
          </div>
          <div>
            <label className="label">Location</label>
            <input
              className="input"
              placeholder="Remote / City"
              value={form.location ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, location: e.target.value }))}
            />
          </div>
          <div>
            <label className="label">Links (comma separated)</label>
            <input
              className="input"
              placeholder="https://github.com/me, https://linkedin.com/in/me"
              value={linksText}
              onChange={(e) => setLinksText(e.target.value)}
            />
          </div>
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? 'Saving…' : 'Save changes'}
          </button>
        </form>
      </Card>

      <Card>
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-400">
          Summary
        </h2>
        <p className="text-lg font-semibold text-gray-900">
          {data?.fullName || '—'}
        </p>
        <p className="text-sm text-gray-600">{data?.headline || 'No headline'}</p>
        <p className="mt-1 text-sm text-gray-500">{data?.email}</p>
        {data?.totalExpYrs != null && (
          <p className="mt-2 text-sm text-gray-500">
            {data.totalExpYrs} yrs experience
          </p>
        )}
        {data?.skills && data.skills.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1.5">
            {data.skills.map((s) => (
              <Pill key={s}>{s}</Pill>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
