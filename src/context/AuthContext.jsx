import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { loginWithCredentials } from '../services/authService'

const STORAGE_KEY = 'auth:isAuthenticated'

const AuthContext = createContext({
  isAuthenticated: false,
  login: async (_credentials) => {},
  logout: () => {},
})

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)

  useEffect(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved === 'true') {
        setIsAuthenticated(true)
      }
    } catch (_) {}
  }, [])

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, isAuthenticated ? 'true' : 'false')
    } catch (_) {}
  }, [isAuthenticated])

  const login = async ({ username, password }) => {
    await loginWithCredentials({ username, password })
    setIsAuthenticated(true)
  }
  const logout = () => setIsAuthenticated(false)

  const value = useMemo(() => ({ isAuthenticated, login, logout }), [isAuthenticated])

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}

export default AuthContext
