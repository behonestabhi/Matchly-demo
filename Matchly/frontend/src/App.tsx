import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Jobs } from './pages/Jobs';
import { JobDetail } from './pages/JobDetail';
import { CandidateDashboard } from './pages/CandidateDashboard';
import { RecruiterDashboard } from './pages/RecruiterDashboard';
import { PipelineBoard } from './pages/PipelineBoard';
import { RankedCandidates } from './pages/RankedCandidates';
import { InterviewGenerator } from './pages/InterviewGenerator';
import { Analytics } from './pages/Analytics';
import { NotFound } from './pages/NotFound';

const RECRUITER_ROLES = ['RECRUITER', 'HIRING_MANAGER', 'ADMIN'] as const;

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<Layout />}>
        <Route index element={<Navigate to="/jobs" replace />} />

        {/* Jobs — any authenticated user */}
        <Route
          path="/jobs"
          element={
            <ProtectedRoute>
              <Jobs />
            </ProtectedRoute>
          }
        />
        <Route
          path="/jobs/:id"
          element={
            <ProtectedRoute>
              <JobDetail />
            </ProtectedRoute>
          }
        />

        {/* Candidate */}
        <Route
          path="/candidate"
          element={
            <ProtectedRoute roles={['CANDIDATE']}>
              <CandidateDashboard />
            </ProtectedRoute>
          }
        />

        {/* Recruiter / hiring manager */}
        <Route
          path="/recruiter"
          element={
            <ProtectedRoute roles={[...RECRUITER_ROLES]}>
              <RecruiterDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/recruiter/jobs/:jobId/pipeline"
          element={
            <ProtectedRoute roles={[...RECRUITER_ROLES]}>
              <PipelineBoard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/recruiter/jobs/:jobId/candidates"
          element={
            <ProtectedRoute roles={[...RECRUITER_ROLES]}>
              <RankedCandidates />
            </ProtectedRoute>
          }
        />
        <Route
          path="/recruiter/jobs/:jobId/interview"
          element={
            <ProtectedRoute roles={[...RECRUITER_ROLES]}>
              <InterviewGenerator />
            </ProtectedRoute>
          }
        />

        {/* Analytics */}
        <Route
          path="/analytics"
          element={
            <ProtectedRoute roles={[...RECRUITER_ROLES]}>
              <Analytics />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
