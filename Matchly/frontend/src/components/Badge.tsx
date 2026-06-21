import type { ReactNode } from 'react';
import type { Stage } from '../types';

const STAGE_TONE: Record<Stage, string> = {
  APPLIED: 'bg-gray-100 text-gray-700',
  SCREENING: 'bg-sky-100 text-sky-700',
  SHORTLISTED: 'bg-indigo-100 text-indigo-700',
  INTERVIEW: 'bg-violet-100 text-violet-700',
  OFFER: 'bg-emerald-100 text-emerald-700',
  REJECTED: 'bg-rose-100 text-rose-700',
};

export function StageBadge({ stage }: { stage: Stage }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STAGE_TONE[stage]}`}
    >
      {stage}
    </span>
  );
}

export function Pill({ children }: { children: ReactNode }) {
  return (
    <span className="inline-flex items-center rounded-full bg-brand-50 px-2.5 py-0.5 text-xs font-medium text-brand-700">
      {children}
    </span>
  );
}
