import { useState } from 'react';
import { fileEasyAuthService, getFileEasyAuthErrorMessage } from '../services/auth';
import type { LoginPayload } from '../types/auth';

export const useAuthState = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(fileEasyAuthService.isAuthenticated());
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const login = async (payload: LoginPayload) => {
    setIsSubmitting(true);
    setError('');
    try {
      await fileEasyAuthService.login(payload);
      setIsAuthenticated(true);
      return true;
    } catch (nextError) {
      setError(getFileEasyAuthErrorMessage(nextError));
      return false;
    } finally {
      setIsSubmitting(false);
    }
  };

  const logout = async () => {
    setIsSubmitting(true);
    setError('');
    try {
      await fileEasyAuthService.logout();
    } finally {
      setIsAuthenticated(false);
      setIsSubmitting(false);
    }
  };

  const clearSession = () => {
    fileEasyAuthService.clearLocalSession();
    setIsAuthenticated(false);
  };

  return {
    isAuthenticated,
    isSubmitting,
    error,
    setError,
    login,
    logout,
    clearSession,
  };
};
