interface ScoreBarProps {
  label: string;
  /** value in [0, 1] */
  value: number;
}

function pct(value: number): number {
  return Math.max(0, Math.min(100, Math.round(value * 100)));
}

function colorFor(value: number): string {
  if (value >= 0.75) return 'bg-emerald-500';
  if (value >= 0.5) return 'bg-amber-500';
  return 'bg-rose-500';
}

export function ScoreBar({ label, value }: ScoreBarProps) {
  const percent = pct(value);
  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-sm">
        <span className="text-gray-600">{label}</span>
        <span className="font-medium text-gray-900">{percent}%</span>
      </div>
      <div className="h-2.5 w-full overflow-hidden rounded-full bg-gray-100">
        <div
          className={`h-full rounded-full ${colorFor(value)}`}
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}

/** A larger circular-feel score chip for the headline final score. */
export function ScoreChip({ value }: { value: number }) {
  const percent = pct(value);
  const tone =
    value >= 0.75
      ? 'bg-emerald-100 text-emerald-700'
      : value >= 0.5
        ? 'bg-amber-100 text-amber-700'
        : 'bg-rose-100 text-rose-700';
  return (
    <span
      className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-semibold ${tone}`}
    >
      {percent}% match
    </span>
  );
}
