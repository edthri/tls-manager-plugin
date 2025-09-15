import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from './context/ProtectedRoute'
import { useAuth } from './context/AuthContext'
import DashboardLayout from './layout/DashboardLayout'
import Login from './pages/Login'
import SslManagement from './pages/SslManagement'

export default function App() {
  const { isAuthenticated } = useAuth()

  return (
    <BrowserRouter basename="/dashboard">
      <Routes>
        <Route
          path="/login"
          element={
            isAuthenticated ? <Navigate to="/ssl" replace /> : <Login />
          }
        />
        <Route
          path="/ssl"
          element={
            <ProtectedRoute>
              <DashboardLayout>
                <SslManagement />
              </DashboardLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/"
          element={<Navigate to={isAuthenticated ? '/ssl' : '/login'} replace />}
        />
        <Route
          path="*"
          element={<Navigate to={isAuthenticated ? '/ssl' : '/login'} replace />}
        />
      </Routes>
    </BrowserRouter>
  )
}
