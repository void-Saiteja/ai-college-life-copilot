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
        setUser(null);
        setLoading(false);
        return;
      }

      try {
        const res = await api.get('/auth/me');
        if (res.data.success) {
          setUser(res.data.user);
        } else {
          setUser(null);
          setToken(null);
          localStorage.removeItem('copilot_token');
        }
      } catch (err) {
        setUser(null);
        setToken(null);
        localStorage.removeItem('copilot_token');
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
