import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../components/Card';
import { Pill } from '../components/Badge';

export function Jobs() {
  const [q, setQ] = useState('');
  const [location, setLocation] = useState('');

  const jobs = [
    {
      id: 1,
      title: 'Senior Python Developer',
      location: 'Remote',
      description:
        'Build scalable AI-powered recruitment systems using FastAPI, PostgreSQL, and AWS.',
      status: 'OPEN',
      requiredSkills: [
        { skillName: 'Python' },
        { skillName: 'FastAPI' },
        { skillName: 'AWS' },
      ],
    },
    {
      id: 2,
      title: 'Machine Learning Engineer',
      location: 'Bangalore',
      description:
        'Develop candidate matching algorithms using NLP, embeddings, and LLMs.',
      status: 'OPEN',
      requiredSkills: [
        { skillName: 'PyTorch' },
        { skillName: 'LLM' },
        { skillName: 'NLP' },
      ],
    },
    {
      id: 3,
      title: 'Frontend React Developer',
      location: 'Remote',
      description:
        'Build modern recruiter dashboards using React, TypeScript, and Tailwind.',
      status: 'OPEN',
      requiredSkills: [
        { skillName: 'React' },
        { skillName: 'TypeScript' },
        { skillName: 'Tailwind' },
      ],
    },
    {
      id: 4,
      title: 'Data Engineer',
      location: 'Mumbai',
      description:
        'Design ETL pipelines, optimize data warehouses, and work with cloud platforms.',
      status: 'OPEN',
      requiredSkills: [
        { skillName: 'Python' },
        { skillName: 'SQL' },
        { skillName: 'AWS' },
      ],
    },
    {
      id: 5,
      title: 'AI Product Manager',
      location: 'Remote',
      description:
        'Lead AI-powered hiring solutions and collaborate with engineering teams.',
      status: 'OPEN',
      requiredSkills: [
        { skillName: 'Product Management' },
        { skillName: 'AI' },
        { skillName: 'Analytics' },
      ],
    },
  ];

  const [filteredJobs, setFilteredJobs] = useState(jobs);

  const onSearch = (e: FormEvent) => {
    e.preventDefault();

    const filtered = jobs.filter(
      (job) =>
        job.title.toLowerCase().includes(q.toLowerCase()) &&
        (location === '' ||
          job.location.toLowerCase().includes(location.toLowerCase()))
    );

    setFilteredJobs(filtered);
  };

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Open roles</h1>
          <p className="mt-1 text-sm text-gray-500">
            Search and apply to jobs across the platform.
          </p>
        </div>
      </div>

      <form
        onSubmit={onSearch}
        className="mb-6 flex flex-wrap items-end gap-3 rounded-xl border border-gray-200 bg-white p-4"
      >
        <div className="min-w-[200px] flex-1">
          <label className="label" htmlFor="q">
            Keyword
          </label>
          <input
            id="q"
            className="input"
            placeholder="e.g. Backend Engineer"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>

        <div className="min-w-[160px]">
          <label className="label" htmlFor="loc">
            Location
          </label>
          <input
            id="loc"
            className="input"
            placeholder="Remote, NYC…"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
          />
        </div>

        <button type="submit" className="btn-primary">
          Search
        </button>
      </form>

      {filteredJobs.length === 0 ? (
        <Card>
          <p className="text-sm text-gray-500">No jobs found.</p>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {filteredJobs.map((job) => (
            <Link key={job.id} to={`/jobs/${job.id}`} className="group">
              <Card className="h-full transition group-hover:border-brand-300 group-hover:shadow-md">
                <div className="flex items-start justify-between gap-2">
                  <h2 className="font-semibold text-gray-900 group-hover:text-brand-700">
                    {job.title}
                  </h2>

                  <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500">
                    {job.status}
                  </span>
                </div>

                <p className="mt-1 text-sm text-gray-500">{job.location}</p>

                <p className="mt-2 line-clamp-2 text-sm text-gray-600">
                  {job.description}
                </p>

                <div className="mt-3 flex flex-wrap gap-1.5">
                  {job.requiredSkills.map((s) => (
                    <Pill key={s.skillName}>{s.skillName}</Pill>
                  ))}
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}