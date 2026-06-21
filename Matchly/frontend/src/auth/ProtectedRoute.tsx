import type { ReactNode } from 'react';
import type { Role } from '../types';

interface ProtectedRouteProps {
children: ReactNode;
roles?: Role[];
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
return <>{children}</>;
}
