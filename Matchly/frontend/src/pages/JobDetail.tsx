import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Card } from '../components/Card';

export function JobDetail() {
  const { id } = useParams();
  const [applied, setApplied] = useState(false);

  const jobs = [
    {
      id: '1',
      title: 'Senior Python Developer',
      location: 'Remote',
      status: 'OPEN',
      description:
        'Build scalable AI-powered recruitment systems using FastAPI, PostgreSQL, AWS, and modern cloud-native architectures.',
      requiredSkills: ['Python', 'FastAPI', 'AWS'],
    },
    {
      id: '2',
      title: 'Machine Learning Engineer',
      location: 'Bangalore',
      status: 'OPEN',
      description:
        'Develop candidate matching algorithms using NLP, embeddings, vector databases, and LLMs.',
      requiredSkills: ['PyTorch', 'NLP', 'LLM'],
    },
    {
      id: '3',
      title: 'Frontend React Developer',
      location: 'Remote',
      status: 'OPEN',
      description:
        'Build modern recruiter dashboards using React, TypeScript, and Tailwind CSS.',
      requiredSkills: ['React', 'TypeScript', 'Tailwind'],
    },
    {
      id: '4',
      title: 'Data Engineer',
      location: 'Mumbai',
      status: 'OPEN',
      description:
        'Design ETL pipelines, optimize data warehouses, and work with cloud platforms.',
      requiredSkills: ['Python', 'SQL', 'AWS'],
    },
    {
      id: '5',
      title: 'AI Product Manager',
      location: 'Remote',
      status: 'OPEN',
      description:
        'Lead AI-powered hiring solutions and collaborate closely with engineering teams.',
      requiredSkills: ['Product Management', 'AI', 'Analytics'],
    },
  ];

  const job = jobs.find((j) => j.id === id);

  if (!job) {
    return (
      <div className="max-w-3xl">
        <h1 className="text-2xl font-semibold">Job Not Found</h1>
        <Link to="/jobs" className="text-brand-600 hover:underline">
          Back to Jobs
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-3xl">
      <Link to="/jobs" className="text-sm text-brand-600 hover:underline">
        ← Back to jobs
      </Link>

      <div className="mt-4">
        <h1 className="text-3xl font-bold text-gray-900">
          {job.title}
        </h1>

        <div className="mt-2 flex gap-3 text-sm text-gray-500">
          <span>{job.location}</span>
          <span>{job.status}</span>
        </div>

        <div className="mt-4">
          <button
            className="btn-primary"
            onClick={() => setApplied(true)}
            disabled={applied}
          >
            {applied ? 'Application Submitted ✓' : 'Apply Now'}
          </button>

          {applied && (
            <p className="mt-2 text-sm text-green-600">
              Your application has been submitted successfully.
            </p>
          )}
        </div>
      </div>

      <Card className="mt-6">
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-gray-400">
          Description
        </h2>

        <p className="text-sm text-gray-700">
          {job.description}
        </p>
      </Card>

      <Card className="mt-4">
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-400">
          Required Skills
        </h2>

        <div className="flex flex-wrap gap-2">
          {job.requiredSkills.map((skill) => (
            <span
              key={skill}
              className="rounded-full bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700"
            >
              {skill}
            </span>
          ))}
        </div>
      </Card>
    </div>
  );
}