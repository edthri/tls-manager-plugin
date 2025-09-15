import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from './context/ProtectedRoute'
import { useAuth } from './context/AuthContext'

function LoginPlaceholder() {
  return <div>Login Page</div>
}

function SslManagementPlaceholder() {
  return <div>SSL management</div>
}

export default function App() {
  const { isAuthenticated } = useAuth()

  return (
    <BrowserRouter basename="/dashboard">
      <Routes>
        <Route
          path="/login"
          element={
            isAuthenticated ? <Navigate to="/ssl" replace /> : <LoginPlaceholder />
          }
        />
        <Route
          path="/ssl"
          element={
            <ProtectedRoute>
              <SslManagementPlaceholder />
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
