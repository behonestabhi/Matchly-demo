import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { matchingApi } from '../api/client';
import { useAsync } from '../hooks/useAsync';
import { Card } from '../components/Card';
import { Pill } from '../components/Badge';
import { ScoreBar, ScoreChip } from '../components/ScoreBar';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';
import type { MatchScore } from '../types';

export function RankedCandidates() {
  const { jobId = '' } = useParams();
  const { data, loading, error } = useAsync(
    () => matchingApi.rankedForJob(jobId, 20, 0),
    [jobId],
  );

  const [openId, setOpenId] = useState<string | null>(null);
  const [score, setScore] = useState<MatchScore | null>(null);
  const [scoreLoading, setScoreLoading] = useState(false);
  const [scoreError, setScoreError] = useState<unknown>(null);

  const toggleBreakdown = async (candidateId: string) => {
    if (openId === candidateId) {
      setOpenId(null);
      return;
    }
    setOpenId(candidateId);
    setScore(null);
    setScoreError(null);
    setScoreLoading(true);
    try {
      const s = await matchingApi.score(candidateId, jobId);
      setScore(s);
    } catch (err) {
      setScoreError(err);
    } finally {
      setScoreLoading(false);
    }
  };

  return (
    <div>
      <Link
        to={`/jobs/${jobId}`}
        className="text-sm text-brand-600 hover:underline"
      >
        ← Back to job
      </Link>
      <h1 className="mt-2 text-2xl font-semibold text-gray-900">
        Ranked candidates
      </h1>
      <p className="mb-6 text-sm text-gray-500">
        AI-ranked matches for this job. Click a candidate for the score
        breakdown.
      </p>

      {loading && <Spinner label="Ranking candidates…" />}
      {error != null && <ErrorMessage error={error} />}

      {data && (
        <div className="grid gap-3">
          {data.content.length === 0 && (
            <Card>
              <p className="text-sm text-gray-500">
                No ranked candidates yet. Publish the job and let matching run.
              </p>
            </Card>
          )}
          {data.content.map((c, i) => {
            const isOpen = openId === c.candidateId;
            return (
              <Card key={c.candidateId}>
                <button
                  className="flex w-full items-center justify-between gap-4 text-left"
                  onClick={() => toggleBreakdown(c.candidateId)}
                >
                  <div className="flex items-center gap-3">
                    <span className="grid h-8 w-8 place-items-center rounded-full bg-gray-100 text-sm font-semibold text-gray-600">
                      {i + 1}
                    </span>
                    <div>
                      <p className="font-medium text-gray-900">
                        {c.name || c.candidateId}
                      </p>
                      {c.topSkills && c.topSkills.length > 0 && (
                        <div className="mt-1 flex flex-wrap gap-1.5">
                          {c.topSkills.slice(0, 6).map((s) => (
                            <Pill key={s}>{s}</Pill>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <ScoreChip value={c.finalScore} />
                    <span className="text-gray-400">{isOpen ? '▲' : '▼'}</span>
                  </div>
                </button>

                {isOpen && (
                  <div className="mt-4 border-t border-gray-100 pt-4">
                    {scoreLoading && <Spinner label="Loading breakdown…" />}
                    {scoreError != null && <ErrorMessage error={scoreError} />}
                    {score && (
                      <div className="space-y-3">
                        <div className="grid gap-3 sm:grid-cols-2">
                          <ScoreBar
                            label="Semantic"
                            value={score.breakdown.semantic}
                          />
                          <ScoreBar
                            label="Skill overlap"
                            value={score.breakdown.skillOverlap}
                          />
                          <ScoreBar
                            label="Experience fit"
                            value={score.breakdown.experienceFit}
                          />
                          <ScoreBar
                            label="Education fit"
                            value={score.breakdown.educationFit}
                          />
                        </div>
                        <div className="flex items-center justify-between text-xs text-gray-400">
                          <span>
                            Final score:{' '}
                            <span className="font-medium text-gray-700">
                              {Math.round(score.finalScore * 100)}%
                            </span>
                          </span>
                          {score.modelVersion && (
                            <span>model {score.modelVersion}</span>
                          )}
                        </div>
                        <div className="flex gap-3 text-sm">
                          <Link
                            to={`/recruiter/jobs/${jobId}/interview?candidateId=${c.candidateId}`}
                            className="text-brand-600 hover:underline"
                          >
                            Generate interview questions →
                          </Link>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
