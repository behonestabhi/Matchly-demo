import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import type { Role } from '../types';

interface NavItem {
  to: string;
  label: string;
  /** Roles allowed to see this item; undefined = any authenticated user. */
  roles?: Role[];
}

const ITEMS: NavItem[] = [
  { to: '/jobs', label: 'Jobs' },
  { to: '/candidate', label: 'My Dashboard', roles: ['CANDIDATE'] },
  {
    to: '/recruiter',
    label: 'Recruiter',
    roles: ['RECRUITER', 'HIRING_MANAGER', 'ADMIN'],
  },
  {
    to: '/analytics',
    label: 'Analytics',
    roles: ['RECRUITER', 'HIRING_MANAGER', 'ADMIN'],
  },
];

export function Nav() {
  const { user, hasRole, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const visible = ITEMS.filter((i) => !i.roles || hasRole(...i.roles));

  return (
    <header className="sticky top-0 z-20 border-b border-gray-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-6xl items-center gap-6 px-4">
        <Link to="/" className="flex items-center gap-2 font-semibold text-gray-900">
          <span className="grid h-7 w-7 place-items-center rounded-md bg-brand-600 text-sm font-bold text-white">
            M
          </span>
          Matchly
        </Link>

        <nav className="flex flex-1 items-center gap-1">
          {visible.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `rounded-md px-3 py-1.5 text-sm font-medium transition ${
                  isActive
                    ? 'bg-brand-50 text-brand-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        {user ? (
          <div className="flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <p className="text-sm font-medium text-gray-900">
                {user.fullName || user.email}
              </p>
              <p className="text-xs text-gray-400">{user.roles.join(', ')}</p>
            </div>
            <button
              onClick={handleLogout}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Log out
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Link
              to="/login"
              className="rounded-md px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-100"
            >
              Log in
            </Link>
            <Link
              to="/register"
              className="rounded-md bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700"
            >
              Sign up
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}
