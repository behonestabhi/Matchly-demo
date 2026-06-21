import { useEffect, useRef, useState } from 'react';
import { candidateApi } from '../../api/client';
import { Card } from '../../components/Card';
import { Pill } from '../../components/Badge';
import { Spinner } from '../../components/Spinner';
import { ErrorMessage } from '../../components/ErrorMessage';
import type { ParseStatus, Resume } from '../../types';

const POLL_MS = 2500;
const MAX_POLLS = 24; // ~1 minute

function statusTone(status: ParseStatus): string {
  switch (status) {
    case 'PARSED':
      return 'bg-emerald-100 text-emerald-700';
    case 'FAILED':
      return 'bg-rose-100 text-rose-700';
    default:
      return 'bg-amber-100 text-amber-700';
  }
}

export function ResumeSection() {
  const [file, setFile] = useState<File | null>(null);
  const [resumeId, setResumeId] = useState<string | null>(null);
  const [resume, setResume] = useState<Resume | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [polling, setPolling] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Poll the resume parse status until terminal or timeout.
  useEffect(() => {
    if (!resumeId) return;
    if (resume && (resume.parseStatus === 'PARSED' || resume.parseStatus === 'FAILED')) {
      setPolling(false);
      return;
    }

    let polls = 0;
    setPolling(true);
    let timer: ReturnType<typeof setTimeout>;

    const tick = async () => {
      try {
        const r = await candidateApi.getResume(resumeId);
        setResume(r);
        if (r.parseStatus === 'PARSED' || r.parseStatus === 'FAILED') {
          setPolling(false);
          return;
        }
      } catch (err) {
        setError(err);
        setPolling(false);
        return;
      }
      polls += 1;
      if (polls >= MAX_POLLS) {
        setPolling(false);
        return;
      }
      timer = setTimeout(tick, POLL_MS);
    };

    timer = setTimeout(tick, POLL_MS);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resumeId, resume?.parseStatus]);

  const onUpload = async () => {
    if (!file) return;
    setUploading(true);
    setError(null);
    setResume(null);
    setResumeId(null);
    try {
      const res = await candidateApi.uploadResume(file);
      setResumeId(res.resumeId);
      setResume({ id: res.resumeId, parseStatus: res.parseStatus });
    } catch (err) {
      setError(err);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <Card>
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-400">
          Upload resume
        </h2>
        <p className="mb-4 text-sm text-gray-500">
          PDF or DOCX. Parsing runs asynchronously — status updates below.
        </p>

        {error != null && (
          <div className="mb-3">
            <ErrorMessage error={error} />
          </div>
        )}

        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.doc,.docx"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          className="block w-full text-sm text-gray-600 file:mr-3 file:rounded-md file:border-0 file:bg-brand-50 file:px-4 file:py-2 file:text-sm file:font-medium file:text-brand-700 hover:file:bg-brand-100"
        />

        <button
          className="btn-primary mt-4"
          disabled={!file || uploading}
          onClick={onUpload}
        >
          {uploading ? 'Uploading…' : 'Upload & parse'}
        </button>

        {resume && (
          <div className="mt-5 flex items-center gap-3">
            <span
              className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${statusTone(
                resume.parseStatus,
              )}`}
            >
              {resume.parseStatus}
            </span>
            {polling && <Spinner label="Parsing…" />}
          </div>
        )}
      </Card>

      <Card>
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-400">
          Parsed result
        </h2>
        {!resume && (
          <p className="text-sm text-gray-400">No resume uploaded yet.</p>
        )}
        {resume?.parseStatus === 'FAILED' && (
          <p className="text-sm text-rose-600">
            Parsing failed. Try re-uploading the file.
          </p>
        )}
        {resume?.parseStatus === 'PARSED' && (
          <div className="space-y-4 text-sm">
            {resume.candidate && (
              <div>
                <p className="font-medium text-gray-900">
                  {resume.candidate.fullName}
                </p>
                <p className="text-gray-500">{resume.candidate.email}</p>
                {resume.candidate.totalExpYrs != null && (
                  <p className="text-gray-500">
                    {resume.candidate.totalExpYrs} yrs experience
                  </p>
                )}
              </div>
            )}
            {resume.skills && resume.skills.length > 0 && (
              <div>
                <p className="mb-1 text-xs font-semibold uppercase text-gray-400">
                  Skills
                </p>
                <div className="flex flex-wrap gap-1.5">
                  {resume.skills.map((s) => (
                    <Pill key={s}>{s}</Pill>
                  ))}
                </div>
              </div>
            )}
            {resume.experiences && resume.experiences.length > 0 && (
              <div>
                <p className="mb-1 text-xs font-semibold uppercase text-gray-400">
                  Experience
                </p>
                <ul className="space-y-1">
                  {resume.experiences.map((exp, i) => (
                    <li key={i} className="text-gray-700">
                      <span className="font-medium">{exp.title}</span>
                      {exp.company && <span> · {exp.company}</span>}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {resume.education && resume.education.length > 0 && (
              <div>
                <p className="mb-1 text-xs font-semibold uppercase text-gray-400">
                  Education
                </p>
                <ul className="space-y-1">
                  {resume.education.map((ed, i) => (
                    <li key={i} className="text-gray-700">
                      {ed.degree} {ed.field && `in ${ed.field}`}
                      {ed.institution && ` — ${ed.institution}`}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </Card>
    </div>
  );
}
