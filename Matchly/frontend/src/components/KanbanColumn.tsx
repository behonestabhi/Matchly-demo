import type { Application, Stage } from '../types';

interface KanbanColumnProps {
  stage: Stage;
  applications: Application[];
  busy: boolean;
  onMove: (app: Application, toStage: Stage) => void;
  nextStages: Stage[];
}

export function KanbanColumn({
  stage,
  applications,
  busy,
  onMove,
  nextStages,
}: KanbanColumnProps) {
  return (
    <div className="flex w-64 flex-shrink-0 flex-col rounded-xl bg-gray-100 p-3">
      <div className="mb-3 flex items-center justify-between px-1">
        <h3 className="text-sm font-semibold text-gray-700">{stage}</h3>
        <span className="rounded-full bg-white px-2 py-0.5 text-xs font-medium text-gray-500">
          {applications.length}
        </span>
      </div>

      <div className="flex flex-col gap-2">
        {applications.length === 0 && (
          <p className="px-1 py-4 text-center text-xs text-gray-400">Empty</p>
        )}
        {applications.map((app) => (
          <div
            key={app.id}
            className="rounded-lg border border-gray-200 bg-white p-3 shadow-sm"
          >
            <p className="text-sm font-medium text-gray-900">
              {app.candidate?.name || app.candidate?.id || 'Candidate'}
            </p>
            <p className="mt-0.5 truncate text-xs text-gray-500">
              {app.job?.title}
            </p>
            {nextStages.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1">
                {nextStages.map((next) => (
                  <button
                    key={next}
                    disabled={busy}
                    onClick={() => onMove(app, next)}
                    className="rounded border border-gray-200 px-1.5 py-0.5 text-[11px] text-gray-600 hover:bg-brand-50 hover:text-brand-700 disabled:opacity-50"
                  >
                    → {next}
                  </button>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
