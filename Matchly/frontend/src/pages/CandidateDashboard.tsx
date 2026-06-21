import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card } from '../components/Card';
import { ProfileSection } from './candidate/ProfileSection';
import { ResumeSection } from './candidate/ResumeSection';
import { ApplicationsSection } from './candidate/ApplicationsSection';
import { SkillGapSection } from './candidate/SkillGapSection';

type Tab = 'profile' | 'resume' | 'applications' | 'skillgap';

const TABS: { id: Tab; label: string }[] = [
  { id: 'profile', label: 'Profile' },
  { id: 'resume', label: 'Resume' },
  { id: 'applications', label: 'My applications' },
  { id: 'skillgap', label: 'Skill gap' },
];

export function CandidateDashboard() {
  const [searchParams] = useSearchParams();
  const initialJob = searchParams.get('skillGapJob');
  const [tab, setTab] = useState<Tab>(initialJob ? 'skillgap' : 'profile');

  return (
    <div>
      <h1 className="mb-1 text-2xl font-semibold text-gray-900">
        Candidate dashboard
      </h1>
      <p className="mb-6 text-sm text-gray-500">
        Manage your profile, resume and applications.
      </p>

      <div className="mb-6 flex flex-wrap gap-1 border-b border-gray-200">
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium transition ${
              tab === t.id
                ? 'border-brand-600 text-brand-700'
                : 'border-transparent text-gray-500 hover:text-gray-800'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'profile' && <ProfileSection />}
      {tab === 'resume' && <ResumeSection />}
      {tab === 'applications' && <ApplicationsSection />}
      {tab === 'skillgap' && <SkillGapSection initialJobId={initialJob} />}

      {tab !== 'profile' &&
        tab !== 'resume' &&
        tab !== 'applications' &&
        tab !== 'skillgap' && (
          <Card>
            <p className="text-sm text-gray-500">Nothing here.</p>
          </Card>
        )}
    </div>
  );
}
