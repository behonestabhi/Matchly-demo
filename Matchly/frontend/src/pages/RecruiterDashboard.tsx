import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { jobApi } from '../api/client';
import { useAsync } from '../hooks/useAsync';
import { Card } from '../components/Card';
import { Pill } from '../components/Badge';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';
import type {
  EducationLevel,
  Job,
  JobCreateRequest,
  RequiredSkill,
} from '../types';

const EDU_LEVELS: EducationLevel[] = [
  'HIGH_SCHOOL',
  'ASSOCIATE',
  'BACHELOR',
  'MASTER',
  'PHD',
];

export function RecruiterDashboard() {
  const { data, loading, error, reload } = useAsync(
    () => jobApi.list({ page: 0, size: 50 }),
    [],
  );

  const [showForm, setShowForm] = useState(false);
  const [busy, setBusy] = useState<string | null>(null);

  const publish = async (job: Job) => {
    setBusy(job.id);
    try {
      await jobApi.publish(job.id);
      reload();
    } finally {
      setBusy(null);
    }
  };

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">
            Recruiter dashboard
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Create jobs, review ranked candidates and manage pipelines.
          </p>
        </div>
        <button
          className="btn-primary"
          onClick={() => setShowForm((v) => !v)}
        >
          {showForm ? 'Close form' : 'New job'}
        </button>
      </div>

      {showForm && (
        <div className="mb-6">
          <CreateJobForm
            educationLevels={EDU_LEVELS}
            onCreated={() => {
              setShowForm(false);
              reload();
            }}
          />
        </div>
      )}

      {loading && <Spinner label="Loading jobs…" />}
      {error != null && <ErrorMessage error={error} />}

      {data && (
        <div className="grid gap-4">
          {data.content.length === 0 && (
            <Card>
              <p className="text-sm text-gray-500">No jobs yet.</p>
            </Card>
          )}
          {data.content.map((job) => (
            <Card key={job.id}>
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <h2 className="font-semibold text-gray-900">{job.title}</h2>
                    {job.status && (
                      <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500">
                        {job.status}
                      </span>
                    )}
                  </div>
                  {job.requiredSkills && job.requiredSkills.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {job.requiredSkills.slice(0, 6).map((s) => (
                        <Pill key={s.skillName}>{s.skillName}</Pill>
                      ))}
                    </div>
                  )}
                </div>
                <div className="flex flex-shrink-0 flex-wrap items-center gap-2 text-sm">
                  {job.status === 'DRAFT' && (
                    <button
                      className="btn-secondary"
                      disabled={busy === job.id}
                      onClick={() => publish(job)}
                    >
                      {busy === job.id ? 'Publishing…' : 'Publish'}
                    </button>
                  )}
                  <Link
                    className="text-brand-600 hover:underline"
                    to={`/recruiter/jobs/${job.id}/candidates`}
                  >
                    Candidates
                  </Link>
                  <Link
                    className="text-brand-600 hover:underline"
                    to={`/recruiter/jobs/${job.id}/pipeline`}
                  >
                    Pipeline
                  </Link>
                  <Link
                    className="text-brand-600 hover:underline"
                    to={`/recruiter/jobs/${job.id}/interview`}
                  >
                    Interview
                  </Link>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------

interface SkillRow {
  skillName: string;
  weight: number;
  required: boolean;
}

function CreateJobForm({
  educationLevels,
  onCreated,
}: {
  educationLevels: EducationLevel[];
  onCreated: () => void;
}) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [location, setLocation] = useState('');
  const [minExp, setMinExp] = useState('');
  const [maxExp, setMaxExp] = useState('');
  const [eduLevel, setEduLevel] = useState<EducationLevel | ''>('');
  const [skills, setSkills] = useState<SkillRow[]>([
    { skillName: '', weight: 1, required: true },
  ]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const addSkill = () =>
    setSkills((s) => [...s, { skillName: '', weight: 0.8, required: false }]);
  const removeSkill = (i: number) =>
    setSkills((s) => s.filter((_, idx) => idx !== i));
  const updateSkill = (i: number, patch: Partial<SkillRow>) =>
    setSkills((s) => s.map((row, idx) => (idx === i ? { ...row, ...patch } : row)));

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const requiredSkills: RequiredSkill[] = skills
        .filter((s) => s.skillName.trim())
        .map((s) => ({
          skillName: s.skillName.trim(),
          weight: s.weight,
          required: s.required,
        }));
      const payload: JobCreateRequest = {
        title: title.trim(),
        description: description.trim(),
        location: location.trim() || undefined,
        minExpYrs: minExp ? Number(minExp) : undefined,
        maxExpYrs: maxExp ? Number(maxExp) : undefined,
        educationLevel: eduLevel || undefined,
        requiredSkills,
      };
      await jobApi.create(payload);
      onCreated();
    } catch (err) {
      setError(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card>
      <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-gray-400">
        Create job (saved as DRAFT)
      </h2>
      <form onSubmit={onSubmit} className="space-y-4">
        {error != null && <ErrorMessage error={error} />}
        <div>
          <label className="label">Title</label>
          <input
            className="input"
            required
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
        </div>
        <div>
          <label className="label">Description</label>
          <textarea
            className="input min-h-[100px]"
            required
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label className="label">Location</label>
            <input
              className="input"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
            />
          </div>
          <div>
            <label className="label">Min exp (yrs)</label>
            <input
              type="number"
              min={0}
              className="input"
              value={minExp}
              onChange={(e) => setMinExp(e.target.value)}
            />
          </div>
          <div>
            <label className="label">Max exp (yrs)</label>
            <input
              type="number"
              min={0}
              className="input"
              value={maxExp}
              onChange={(e) => setMaxExp(e.target.value)}
            />
          </div>
          <div>
            <label className="label">Education</label>
            <select
              className="input"
              value={eduLevel}
              onChange={(e) =>
                setEduLevel(e.target.value as EducationLevel | '')
              }
            >
              <option value="">Any</option>
              {educationLevels.map((lvl) => (
                <option key={lvl} value={lvl}>
                  {lvl}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <label className="label mb-0">Required skills</label>
            <button
              type="button"
              className="text-sm text-brand-600 hover:underline"
              onClick={addSkill}
            >
              + Add skill
            </button>
          </div>
          <div className="space-y-2">
            {skills.map((row, i) => (
              <div key={i} className="flex flex-wrap items-center gap-2">
                <input
                  className="input flex-1"
                  placeholder="Skill name"
                  value={row.skillName}
                  onChange={(e) => updateSkill(i, { skillName: e.target.value })}
                />
                <input
                  type="number"
                  step={0.1}
                  min={0}
                  max={1}
                  className="input w-24"
                  title="Weight 0–1"
                  value={row.weight}
                  onChange={(e) =>
                    updateSkill(i, { weight: Number(e.target.value) })
                  }
                />
                <label className="flex items-center gap-1 text-sm text-gray-600">
                  <input
                    type="checkbox"
                    checked={row.required}
                    onChange={(e) =>
                      updateSkill(i, { required: e.target.checked })
                    }
                  />
                  required
                </label>
                {skills.length > 1 && (
                  <button
                    type="button"
                    className="text-sm text-rose-500 hover:underline"
                    onClick={() => removeSkill(i)}
                  >
                    Remove
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create job'}
        </button>
      </form>
    </Card>
  );
}
