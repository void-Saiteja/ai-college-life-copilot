import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('copilot_token') || null);
  const [loading, setLoading] = useState(true);

  // Check auth state on mount
  useEffect(() => {
    async function checkAuth() {
      if (!token) {
        // Default seed demo student user for instant seamless experience
        setUser({
          id: 'u-student-1',
          name: 'Alex Mercer',
          email: 'alex@student.edu',
          role: 'STUDENT',
          studentId: 'std-1',
          semester: 4,
          department: 'Computer Science & Engineering'
        });
        setLoading(false);
        return;
      }

      try {
        const res = await api.get('/auth/me');
        if (res.data.success) {
          setUser(res.data.user);
        }
      } catch (err) {
        console.warn('Auth session check notice: Using active session.');
      } finally {
        setLoading(false);
      }
    }

    checkAuth();
  }, [token]);

  const login = async (email, password) => {
    try {
      const res = await api.post('/auth/login', { email, password });
      if (res.data.success) {
        const newToken = res.data.token;
        const loggedUser = res.data.user;
        localStorage.setItem('copilot_token', newToken);
        setToken(newToken);
        setUser(loggedUser);
        return { success: true, user: loggedUser };
      }
    } catch (err) {
      return {
        success: false,
        error: err.response?.data?.error || 'Login failed. Please check your credentials.'
      };
    }
  };

  const register = async (userData) => {
    try {
      const res = await api.post('/auth/register', userData);
      if (res.data.success) {
        const newToken = res.data.token;
        const newUser = res.data.user;
        localStorage.setItem('copilot_token', newToken);
        setToken(newToken);
        setUser(newUser);
        return { success: true, user: newUser };
      }
    } catch (err) {
      return {
        success: false,
        error: err.response?.data?.error || 'Registration failed.'
      };
    }
  };

  const logout = () => {
    localStorage.removeItem('copilot_token');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, logout, setUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
