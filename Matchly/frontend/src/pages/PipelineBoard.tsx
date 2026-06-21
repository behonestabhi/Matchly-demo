import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { applicationApi } from '../api/client';
import { useAsync } from '../hooks/useAsync';
import { KanbanColumn } from '../components/KanbanColumn';
import { Spinner } from '../components/Spinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { STAGES, type Application, type Stage } from '../types';

/** Suggest the most relevant next moves for a card from its current stage. */
function nextStagesFor(stage: Stage): Stage[] {
  const idx = STAGES.indexOf(stage);
  const forward = STAGES.slice(idx + 1, idx + 2).filter(
    (s) => s !== 'REJECTED',
  );
  const options = new Set<Stage>(forward);
  if (stage !== 'REJECTED') options.add('REJECTED');
  return Array.from(options);
}

export function PipelineBoard() {
  const { jobId = '' } = useParams();
  const { data, loading, error, reload } = useAsync(
    () => applicationApi.byJob(jobId),
    [jobId],
  );
  const [busy, setBusy] = useState(false);
  const [moveError, setMoveError] = useState<unknown>(null);

  const onMove = async (app: Application, toStage: Stage) => {
    setBusy(true);
    setMoveError(null);
    try {
      await applicationApi.changeStage(app.id, { toStage });
      reload();
    } catch (err) {
      setMoveError(err);
    } finally {
      setBusy(false);
    }
  };

  const apps = data ?? [];
  const byStage = (stage: Stage) =>
    apps.filter((a) => a.currentStage === stage);

  return (
    <div>
      <Link
        to={`/jobs/${jobId}`}
        className="text-sm text-brand-600 hover:underline"
      >
        ← Back to job
      </Link>
      <h1 className="mt-2 text-2xl font-semibold text-gray-900">
        Pipeline board
      </h1>
      <p className="mb-6 text-sm text-gray-500">
        Move candidates through stages. Updates persist via the ATS service.
      </p>

      {loading && <Spinner label="Loading pipeline…" />}
      {error != null && <ErrorMessage error={error} />}
      {moveError != null && (
        <div className="mb-4">
          <ErrorMessage error={moveError} />
        </div>
      )}

      {data && (
        <div className="flex gap-4 overflow-x-auto pb-4">
          {STAGES.map((stage) => (
            <KanbanColumn
              key={stage}
              stage={stage}
              applications={byStage(stage)}
              busy={busy}
              onMove={onMove}
              nextStages={nextStagesFor(stage)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
