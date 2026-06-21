import { useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { interviewApi } from '../api/client';
import { Card } from '../components/Card';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';
import type {
  Difficulty,
  InterviewQuestion,
  InterviewSet,
  QuestionCategory,
} from '../types';

const POLL_MS = 2500;
const MAX_POLLS = 40; // ~100s

const CATEGORIES: QuestionCategory[] = ['TECHNICAL', 'BEHAVIORAL', 'SCENARIO'];

const CATEGORY_TONE: Record<QuestionCategory, string> = {
  TECHNICAL: 'bg-sky-50 text-sky-700 border-sky-200',
  BEHAVIORAL: 'bg-violet-50 text-violet-700 border-violet-200',
  SCENARIO: 'bg-amber-50 text-amber-700 border-amber-200',
};

const DIFF_TONE: Record<Difficulty, string> = {
  EASY: 'bg-emerald-100 text-emerald-700',
  MEDIUM: 'bg-amber-100 text-amber-700',
  HARD: 'bg-rose-100 text-rose-700',
};

export function InterviewGenerator() {
  const { jobId = '' } = useParams();
  const [searchParams] = useSearchParams();
  const [candidateId, setCandidateId] = useState(
    searchParams.get('candidateId') ?? '',
  );

  const [set, setSet] = useState<InterviewSet | null>(null);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const pollRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(
    () => () => {
      if (pollRef.current) clearTimeout(pollRef.current);
    },
    [],
  );

  const startPolling = (id: string) => {
    let polls = 0;
    const tick = async () => {
      try {
        const result = await interviewApi.get(id);
        setSet(result);
        if (result.status === 'READY' || result.status === 'FAILED') {
          setGenerating(false);
          return;
        }
      } catch (err) {
        setError(err);
        setGenerating(false);
        return;
      }
      polls += 1;
      if (polls >= MAX_POLLS) {
        setGenerating(false);
        return;
      }
      pollRef.current = setTimeout(tick, POLL_MS);
    };
    pollRef.current = setTimeout(tick, POLL_MS);
  };

  const generate = async () => {
    if (!candidateId.trim()) return;
    setGenerating(true);
    setError(null);
    setSet(null);
    try {
      const created = await interviewApi.generate({
        candidateId: candidateId.trim(),
        jobId,
      });
      setSet(created);
      if (created.status === 'READY' || created.status === 'FAILED') {
        setGenerating(false);
      } else {
        startPolling(created.id);
      }
    } catch (err) {
      setError(err);
      setGenerating(false);
    }
  };

  const questions = set?.questions ?? [];
  const grouped = (cat: QuestionCategory): InterviewQuestion[] =>
    questions.filter((q) => q.category === cat);

  return (
    <div>
      <Link
        to={`/jobs/${jobId}`}
        className="text-sm text-brand-600 hover:underline"
      >
        ← Back to job
      </Link>
      <h1 className="mt-2 text-2xl font-semibold text-gray-900">
        Interview questions
      </h1>
      <p className="mb-6 text-sm text-gray-500">
        Generate AI tailored questions for a candidate and this job.
      </p>

      <Card className="mb-6">
        <div className="flex flex-wrap items-end gap-3">
          <div className="min-w-[240px] flex-1">
            <label className="label">Candidate ID</label>
            <input
              className="input"
              placeholder="candidate-uuid"
              value={candidateId}
              onChange={(e) => setCandidateId(e.target.value)}
            />
          </div>
          <button
            className="btn-primary"
            disabled={generating || !candidateId.trim()}
            onClick={generate}
          >
            {generating ? 'Generating…' : 'Generate'}
          </button>
        </div>
      </Card>

      {error != null && <ErrorMessage error={error} />}

      {set && set.status !== 'READY' && (
        <Card>
          <div className="flex items-center gap-3">
            <span
              className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                set.status === 'FAILED'
                  ? 'bg-rose-100 text-rose-700'
                  : 'bg-amber-100 text-amber-700'
              }`}
            >
              {set.status}
            </span>
            {set.status === 'GENERATING' && <Spinner label="Generating…" />}
            {set.status === 'FAILED' && (
              <span className="text-sm text-rose-600">
                Generation failed. Try again.
              </span>
            )}
          </div>
        </Card>
      )}

      {set?.status === 'READY' && (
        <div className="space-y-6">
          {set.llmModel && (
            <p className="text-xs text-gray-400">Model: {set.llmModel}</p>
          )}
          {CATEGORIES.map((cat) => {
            const items = grouped(cat);
            if (items.length === 0) return null;
            return (
              <section key={cat}>
                <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-500">
                  {cat}
                </h2>
                <div className="space-y-3">
                  {items.map((q, i) => (
                    <Card
                      key={`${cat}-${i}`}
                      className={`border ${CATEGORY_TONE[cat]}`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <p className="font-medium text-gray-900">{q.question}</p>
                        {q.difficulty && (
                          <span
                            className={`flex-shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${DIFF_TONE[q.difficulty]}`}
                          >
                            {q.difficulty}
                          </span>
                        )}
                      </div>
                      {q.suggestedAnswer && (
                        <details className="mt-2 text-sm text-gray-600">
                          <summary className="cursor-pointer font-medium text-gray-500">
                            Suggested answer
                          </summary>
                          <p className="mt-1 whitespace-pre-line">
                            {q.suggestedAnswer}
                          </p>
                        </details>
                      )}
                    </Card>
                  ))}
                </div>
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}
