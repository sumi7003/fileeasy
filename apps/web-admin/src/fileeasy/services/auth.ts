import axios from 'axios';
import { loginFileEasy, logoutFileEasy } from '../../api/fileeasy';
import type { AuthSession, LoginPayload, LoginResponse } from '../types/auth';

const SESSION_COOKIE = 'xplay_auth';
const SESSION_TTL_SECONDS = 7 * 24 * 60 * 60;

const getCookieValue = (name: string) => {
  if (typeof document === 'undefined') return '';
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = document.cookie.match(new RegExp(`(?:^|; )${escaped}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : '';
};

const setSessionCookie = (token?: string) => {
  if (typeof document === 'undefined') return;
  document.cookie = `${SESSION_COOKIE}=${encodeURIComponent(
    token || 'admin-token',
  )}; path=/; max-age=${SESSION_TTL_SECONDS}; SameSite=Lax`;
};

const clearSessionCookie = () => {
  if (typeof document === 'undefined') return;
  document.cookie = `${SESSION_COOKIE}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax`;
};

export const getFileEasyAuthErrorMessage = (error: unknown) => {
  if (axios.isAxiosError(error)) {
    const responseData = error.response?.data as
      | { message?: string; error?: string; detail?: string }
      | undefined;
    if (typeof responseData?.message === 'string' && responseData.message.trim()) {
      return responseData.message;
    }
    if (typeof responseData?.error === 'string' && responseData.error.trim()) {
      return responseData.error;
    }
    if (typeof responseData?.detail === 'string' && responseData.detail.trim()) {
      return responseData.detail;
    }
    if (error.response?.status === 401) {
      return '密码错误，请重新输入。';
    }
    if (error.code === 'ERR_NETWORK' || !error.response) {
      return '当前无法连接到设备，请确认局域网服务已开启。';
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }

  return '登录失败，请稍后重试。';
};

export const fileEasyAuthService = {
  isAuthenticated: () => Boolean(getCookieValue(SESSION_COOKIE)),
  getSession: (): AuthSession | null => {
    const token = getCookieValue(SESSION_COOKIE);
    return token ? { token } : null;
  },
  login: async (payload: LoginPayload): Promise<LoginResponse> => {
    const response = await loginFileEasy(payload.password);
    setSessionCookie(response.token);
    return {
      token: response.token ?? 'admin-token',
      expiresAt: response.expiresAt,
    };
  },
  logout: async (): Promise<void> => {
    try {
      await logoutFileEasy();
    } finally {
      clearSessionCookie();
    }
  },
  clearLocalSession: () => {
    clearSessionCookie();
  },
};
