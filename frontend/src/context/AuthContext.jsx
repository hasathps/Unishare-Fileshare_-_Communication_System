import React, { createContext, useContext, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import api, { API_BASE_URL } from '../services/api';

const AuthContext = createContext({
  user: null,
  loading: true,
  login: async () => {},
  logout: async () => {},
});

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchCurrentUser = async () => {
      try {
        const { data } = await api.get('/api/auth/me');
        setUser(data);
      } catch (error) {
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    fetchCurrentUser();
  }, []);

  const login = async (email, password) => {
    try {
      const { data } = await api.post('/api/auth/login', { email, password });
      toast.success(`Welcome back ${data.displayName || data.email}!`);
      const current = await api.get('/api/auth/me');
      setUser(current.data);
    } catch (error) {
      const message =
        error.response?.data?.error ||
        error.message ||
        'Unable to log in. Please try again.';
      toast.error(message);
      throw error;
    }
  };

  const logout = async () => {
    try {
      await api.post('/api/auth/logout');
      setUser(null);
      toast.success('Signed out successfully.');
    } catch (error) {
      toast.error('Failed to log out. Please try again.');
      throw error;
    }
  };

  const value = { user, loading, login, logout, apiBaseUrl: API_BASE_URL };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

