import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import MainLayout from './layouts/MainLayout';
import ProtectedRoute from './components/ProtectedRoute';

import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import StudentDashboard from './pages/StudentDashboard';
import AdminDashboard from './pages/AdminDashboard';
import AICopilotPage from './pages/AICopilotPage';
import StudyPlannerPage from './pages/StudyPlannerPage';
import AttendancePage from './pages/AttendancePage';
import AssignmentsPage from './pages/AssignmentsPage';
import ExamsPage from './pages/ExamsPage';
import AIQuizPage from './pages/AIQuizPage';
import DocumentsPage from './pages/DocumentsPage';
import CareerPage from './pages/CareerPage';
import NotificationsPage from './pages/NotificationsPage';
import ProfilePage from './pages/ProfilePage';
import SchedulePage from './pages/SchedulePage';
import NotesPage from './pages/NotesPage';
import BudgetPage from './pages/BudgetPage';

export default function App() {
  return (
    <Router>
      <AuthProvider>
        <MainLayout>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            <Route 
              path="/dashboard" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <StudentDashboard />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/admin" 
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <AdminDashboard />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/copilot" 
              element={
                <ProtectedRoute>
                  <AICopilotPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/planner" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <StudyPlannerPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/attendance" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <AttendancePage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/assignments" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <AssignmentsPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/exams" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <ExamsPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/quiz" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <AIQuizPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/documents" 
              element={
                <ProtectedRoute>
                  <DocumentsPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/career" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <CareerPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/notifications" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <NotificationsPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/schedule" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <SchedulePage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/notes" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <NotesPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/budget" 
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <BudgetPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/profile" 
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              } 
            />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </MainLayout>
      </AuthProvider>
    </Router>
  );
}
