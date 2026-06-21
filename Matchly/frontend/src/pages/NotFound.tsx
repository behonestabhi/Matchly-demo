import { Link } from 'react-router-dom';

export function NotFound() {
  return (
    <div className="mx-auto max-w-lg py-20 text-center">
      <p className="text-5xl font-bold text-brand-600">404</p>
      <h1 className="mt-4 text-2xl font-semibold text-gray-900">
        Page not found
      </h1>
      <p className="mt-2 text-gray-500">
        The page you’re looking for doesn’t exist.
      </p>
      <Link to="/jobs" className="btn-primary mt-6">
        Go to jobs
      </Link>
    </div>
  );
}
