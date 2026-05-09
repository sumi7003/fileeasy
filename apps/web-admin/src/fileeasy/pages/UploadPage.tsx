import axios from 'axios';
import React, { useEffect, useRef, useState } from 'react';
import {
  cancelFileEasyUpload,
  completeFileEasyUpload,
  getFileEasyHomeSummary,
  getFileEasyUploadStatus,
  initFileEasyUpload,
  loginFileEasy,
  uploadFileEasyChunk,
} from '../../api/fileeasy';
import request from '../../api/request';
import Button from '../components/shared/Button';
import Input from '../components/shared/Input';
import ProgressBar from '../components/shared/ProgressBar';
import YiTransferBrand from '../components/shared/YiTransferBrand';
import type { StoredUploadTask, UploadTask, UploadTaskStatus } from '../types/upload';
import './upload-page.css';

const CHUNK_SIZE = 8 * 1024 * 1024;
const MAX_FILE_SIZE = 4 * 1024 * 1024 * 1024;
const SESSION_COOKIE = 'xplay_auth';
const TASK_STORAGE_KEY = 'fileeasy-upload-sessions-v1';
const REMEMBER_SESSION_TTL_SECONDS = 7 * 24 * 60 * 60;

const SUPPORTED_EXTENSIONS = [
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'ppt',
  'pptx',
  'txt',
  'jpg',
  'jpeg',
  'png',
  'gif',
  'webp',
  'mp4',
  'mov',
  'mp3',
  'wav',
  'm4a',
  'zip',
] as const;

const SUPPORTED_EXTENSION_BADGES = ['PDF', 'JPG', 'MP4', 'DOC', 'ZIP', '+ 更多'];
const FILE_PICKER_ACCEPT = '*/*';

type ServiceStatus = 'checking' | 'ready' | 'unavailable';
type UploadViewState =
  | 'network-unavailable'
  | 'auth-required'
  | 'file-selection'
  | 'uploading'
  | 'upload-complete';

type SpeedSample = {
  bytes: number;
  time: number;
};

const getCookieValue = (name: string) => {
  if (typeof document === 'undefined') return '';
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = document.cookie.match(new RegExp(`(?:^|; )${escaped}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : '';
};

const setSessionCookie = (token?: string, remember = true) => {
  if (typeof document === 'undefined') return;
  const cookieValue = `${SESSION_COOKIE}=${encodeURIComponent(token || 'admin-token')}; path=/; SameSite=Lax`;
  document.cookie = remember
    ? `${cookieValue}; max-age=${REMEMBER_SESSION_TTL_SECONDS}`
    : cookieValue;
};

const clearSessionCookie = () => {
  if (typeof document === 'undefined') return;
  document.cookie = `${SESSION_COOKIE}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax`;
};

const createTaskId = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `upload-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
};

const createFingerprint = (file: File) => `${file.name}:${file.size}:${file.lastModified}`;

const getFileExtension = (fileName: string) => {
  const segments = fileName.split('.');
  return segments.length > 1 ? segments.pop()?.toLowerCase() ?? '' : '';
};

const formatBytes = (bytes: number) => {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / 1024 ** exponent;
  return `${value >= 100 || exponent === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[exponent]}`;
};

const formatDuration = (milliseconds: number) => {
  if (!Number.isFinite(milliseconds) || milliseconds <= 0) return '0 秒';
  const totalSeconds = Math.max(1, Math.round(milliseconds / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes <= 0) return `${seconds} 秒`;
  return `${minutes} 分 ${seconds.toString().padStart(2, '0')} 秒`;
};

const formatRemainingTime = (seconds: number | null) => {
  if (!seconds || !Number.isFinite(seconds) || seconds <= 0) return '正在计算';
  if (seconds < 60) return `约 ${Math.ceil(seconds)} 秒`;
  const minutes = Math.floor(seconds / 60);
  const remainSeconds = Math.ceil(seconds % 60);
  return `约 ${minutes} 分 ${remainSeconds.toString().padStart(2, '0')} 秒`;
};

const getProgressFromChunks = (uploadedChunkCount: number, totalChunks: number) => {
  if (totalChunks <= 0) return 0;
  return Math.max(0, Math.min(100, Math.round((uploadedChunkCount / totalChunks) * 100)));
};

const mapServerStatus = (status?: string): UploadTaskStatus => {
  switch (status) {
    case 'completed':
    case 'done':
    case 'success':
    case 'finished':
      return 'completed';
    case 'uploading':
    case 'in_progress':
      return 'uploading';
    case 'resuming':
    case 'restoring':
      return 'resuming';
    case 'failed':
    case 'error':
      return 'failed';
    case 'queued':
    case 'pending':
    case 'created':
      return 'queued';
    case 'paused':
    default:
      return 'paused';
  }
};

const formatExpiryNote = (expiresAt?: string) => {
  if (!expiresAt) return '已保留上传进度，可继续上传。';
  const parsed = new Date(expiresAt);
  if (Number.isNaN(parsed.getTime())) return '已保留上传进度，可继续上传。';
  return `上传进度已保留至 ${parsed.toLocaleString('zh-CN', { hour12: false })}。`;
};

const getApiErrorMessage = (error: unknown, fallback: string) => {
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
      return '登录已失效，请重新输入访问密码。';
    }
    if (error.code === 'ERR_NETWORK' || !error.response) {
      return '当前无法连接到易传输服务，请确认手机与设备在同一局域网。';
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }

  return fallback;
};

const isUnauthorizedError = (error: unknown) =>
  axios.isAxiosError(error) && error.response?.status === 401;

const loadStoredTasks = (): UploadTask[] => {
  if (typeof window === 'undefined') return [];
  try {
    const raw = window.localStorage.getItem(TASK_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as StoredUploadTask[];
    if (!Array.isArray(parsed)) return [];
    return parsed.map((task) => ({
      ...task,
      status: task.status === 'completed' ? 'completed' : 'paused',
      note:
        task.status === 'completed'
          ? task.note || '上传已完成。'
          : task.needsFileReselect
            ? '重新选择同一个原文件后可继续上传。'
            : '已恢复上传记录，登录后可以继续上传。',
      needsFileReselect: true,
      file: undefined,
      lastUpdatedAt: task.lastUpdatedAt || Date.now(),
    }));
  } catch (error) {
    console.warn('Failed to restore FileEasy upload tasks', error);
    return [];
  }
};

const UploadPage: React.FC = () => {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const tasksRef = useRef<UploadTask[]>([]);
  const runningTasksRef = useRef<Set<string>>(new Set());
  const batchRunningRef = useRef(false);
  const speedSampleRef = useRef<SpeedSample | null>(null);

  const [serviceStatus, setServiceStatus] = useState<ServiceStatus>('checking');
  const [isOnline, setIsOnline] = useState(() =>
    typeof navigator === 'undefined' ? true : navigator.onLine,
  );
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getCookieValue(SESSION_COOKIE)));
  const [rememberSession, setRememberSession] = useState(true);
  const [passwordRequired, setPasswordRequired] = useState(true);
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loginError, setLoginError] = useState('');
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [tasks, setTasks] = useState<UploadTask[]>(() => loadStoredTasks());
  const [batchRunning, setBatchRunning] = useState(false);
  const [batchStartedAt, setBatchStartedAt] = useState<number | null>(null);
  const [batchCompletedAt, setBatchCompletedAt] = useState<number | null>(null);
  const [transferSpeed, setTransferSpeed] = useState(0);
  const [isDraggingFiles, setIsDraggingFiles] = useState(false);
  const [pageMessage, setPageMessage] = useState('');

  tasksRef.current = tasks;
  batchRunningRef.current = batchRunning;

  const connectionAddress =
    typeof window === 'undefined' ? '192.168.1.8:8080' : window.location.host || '192.168.1.8:8080';

  const updateTask = (taskId: string, updater: (task: UploadTask) => UploadTask) => {
    setTasks((current) => current.map((task) => (task.id === taskId ? updater(task) : task)));
  };

  const upsertTask = (nextTask: UploadTask) => {
    setTasks((current) => {
      const existingIndex = current.findIndex((task) => task.id === nextTask.id);
      if (existingIndex === -1) {
        return [...current, nextTask];
      }
      const nextTasks = [...current];
      nextTasks[existingIndex] = nextTask;
      return nextTasks;
    });
  };

  const removeTask = (taskId: string) => {
    setTasks((current) => current.filter((task) => task.id !== taskId));
  };

  const addImmediateErrorTask = (fileName: string, fileSize: number, message: string) => {
    upsertTask({
      id: createTaskId(),
      fingerprint: `${fileName}:${fileSize}:error`,
      fileName,
      fileSize,
      progress: 0,
      status: 'failed',
      note: message,
      errorMessage: message,
      totalChunks: 0,
      uploadedChunkCount: 0,
      resumable: false,
      needsFileReselect: false,
      lastUpdatedAt: Date.now(),
    });
  };

  const syncTaskFromStatus = async (task: UploadTask) => {
    if (!task.uploadId) return;
    try {
      const status = await getFileEasyUploadStatus(task.uploadId);
      const uploadedChunkCount = status.uploadedChunkIndexes?.length ?? status.uploadedChunks ?? 0;
      const totalChunks = status.totalChunks || task.totalChunks;
      updateTask(task.id, (current) => {
        const mappedStatus = mapServerStatus(status.status);
        const completed = mappedStatus === 'completed' || uploadedChunkCount >= totalChunks;
        return {
          ...current,
          progress: completed ? 100 : getProgressFromChunks(uploadedChunkCount, totalChunks),
          status: completed ? 'completed' : current.file ? 'paused' : 'paused',
          totalChunks,
          uploadedChunkCount,
          expiresAt: status.expiresAt,
          needsFileReselect: !current.file,
          note: completed
            ? '上传已完成。'
            : current.file
              ? `${formatExpiryNote(status.expiresAt)} 点击继续上传可从已完成进度接着传。`
              : '已恢复上传记录，重新选择同一个原文件后可继续上传。',
          lastUpdatedAt: Date.now(),
        };
      });
    } catch (error) {
      if (isUnauthorizedError(error)) {
        clearSessionCookie();
        setIsAuthenticated(false);
        setLoginError('登录已失效，请重新输入访问密码。');
        return;
      }
      updateTask(task.id, (current) => ({
        ...current,
        status: 'paused',
        note: '当前无法同步上传状态，网络恢复后可继续上传。',
        lastUpdatedAt: Date.now(),
      }));
    }
  };

  const refreshStoredStatuses = async () => {
    const snapshot = tasksRef.current.filter((task) => task.uploadId);
    await Promise.all(snapshot.map((task) => syncTaskFromStatus(task)));
  };

  const checkServiceAvailability = async () => {
    if (!navigator.onLine) {
      setServiceStatus('unavailable');
      return;
    }

    setServiceStatus('checking');
    try {
      await request.get('/ping');
      const summary = await getFileEasyHomeSummary();
      setServiceStatus('ready');
      setPasswordRequired(summary.passwordRequired);
      if (!summary.passwordRequired) {
        setSessionCookie('admin-token', true);
        setIsAuthenticated(true);
        setLoginError('');
      } else if (!getCookieValue(SESSION_COOKIE)) {
        setIsAuthenticated(false);
      }
      setPageMessage('');
    } catch (error) {
      setServiceStatus('unavailable');
      if (!pageMessage) {
        setPageMessage(getApiErrorMessage(error, '当前无法连接到易传输服务。'));
      }
    }
  };

  const startUpload = async (file: File, taskId: string) => {
    const task = tasksRef.current.find((item) => item.id === taskId);
    if (!task || runningTasksRef.current.has(taskId)) return;

    runningTasksRef.current.add(taskId);
    updateTask(taskId, (current) => ({
      ...current,
      status: current.uploadId ? 'resuming' : 'uploading',
      note: current.uploadId ? '正在继续上传...' : '正在上传...',
      errorMessage: undefined,
      needsFileReselect: false,
      lastUpdatedAt: Date.now(),
    }));

    try {
      let uploadId = task.uploadId;
      let totalChunks = task.totalChunks || Math.max(1, Math.ceil(file.size / CHUNK_SIZE));
      let uploadedChunkIndexes: number[] = [];
      let expiresAt = task.expiresAt;

      if (uploadId) {
        const status = await getFileEasyUploadStatus(uploadId);
        uploadedChunkIndexes = status.uploadedChunkIndexes || [];
        totalChunks = status.totalChunks || totalChunks;
        expiresAt = status.expiresAt;
      } else {
        const initResponse = await initFileEasyUpload({
          fileName: file.name,
          fileSize: file.size,
          chunkSize: CHUNK_SIZE,
          totalChunks,
          mimeType: file.type || undefined,
        });
        uploadId = initResponse.uploadId;
        uploadedChunkIndexes = initResponse.uploadedChunkIndexes || [];
        totalChunks = initResponse.totalChunks || totalChunks;
        expiresAt = initResponse.expiresAt;
      }

      updateTask(taskId, (current) => ({
        ...current,
        uploadId,
        totalChunks,
        uploadedChunkCount: uploadedChunkIndexes.length,
        progress: getProgressFromChunks(uploadedChunkIndexes.length, totalChunks),
        expiresAt,
        lastUpdatedAt: Date.now(),
      }));

      for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex += 1) {
        if (!batchRunningRef.current) {
          updateTask(taskId, (current) => ({
            ...current,
            status: 'paused',
            note: '上传已暂停，可稍后继续。',
            lastUpdatedAt: Date.now(),
          }));
          return;
        }

        if (uploadedChunkIndexes.includes(chunkIndex)) {
          continue;
        }

        const chunk = file.slice(chunkIndex * CHUNK_SIZE, Math.min(file.size, (chunkIndex + 1) * CHUNK_SIZE));
        await uploadFileEasyChunk({
          uploadId,
          chunkIndex,
          totalChunks,
          fileName: file.name,
          fileSize: file.size,
          chunk,
        });

        uploadedChunkIndexes = [...uploadedChunkIndexes, chunkIndex];
        updateTask(taskId, (current) => ({
          ...current,
          status: 'uploading',
          uploadedChunkCount: uploadedChunkIndexes.length,
          progress: getProgressFromChunks(uploadedChunkIndexes.length, totalChunks),
          note: uploadedChunkIndexes.length >= totalChunks ? '正在完成上传...' : '正在上传...',
          expiresAt,
          lastUpdatedAt: Date.now(),
        }));
      }

      const completeResponse = await completeFileEasyUpload({
        uploadId,
        fileName: file.name,
        fileSize: file.size,
        totalChunks,
      });

      updateTask(taskId, (current) => ({
        ...current,
        uploadId,
        status: 'completed',
        progress: 100,
        uploadedChunkCount: totalChunks,
        totalChunks,
        finalName:
          completeResponse.displayName ||
          completeResponse.fileName ||
          completeResponse.storedFilename ||
          current.finalName,
        note: '文件已成功发送到设备。',
        errorMessage: undefined,
        resumable: false,
        needsFileReselect: false,
        lastUpdatedAt: Date.now(),
      }));
    } catch (error) {
      if (isUnauthorizedError(error)) {
        clearSessionCookie();
        setIsAuthenticated(false);
        setLoginError('登录已失效，请重新输入访问密码。');
      }
      updateTask(taskId, (current) => ({
        ...current,
        status: current.uploadId ? 'paused' : 'failed',
        note: current.uploadId
          ? '上传已中断，稍后可从当前进度继续。'
          : getApiErrorMessage(error, '上传初始化失败，请重试。'),
        errorMessage: getApiErrorMessage(error, '上传失败，请稍后重试。'),
        needsFileReselect: !current.file,
        lastUpdatedAt: Date.now(),
      }));
      setPageMessage(getApiErrorMessage(error, '上传过程中出现异常。'));
    } finally {
      runningTasksRef.current.delete(taskId);
    }
  };

  const handleLogin = async () => {
    if (!password.trim()) {
      setLoginError('请输入访问密码。');
      return;
    }

    setIsLoggingIn(true);
    setLoginError('');
    try {
      const response = await loginFileEasy(password.trim());
      setSessionCookie(response.token, rememberSession);
      setIsAuthenticated(true);
      setPassword('');
      setPageMessage('');
      await refreshStoredStatuses();
    } catch (error) {
      setLoginError(getApiErrorMessage(error, '登录失败，请稍后重试。'));
    } finally {
      setIsLoggingIn(false);
    }
  };

  const handleFiles = (nextFiles: File[]) => {
    if (!nextFiles.length) return;

    const nextTasks: UploadTask[] = [];
    nextFiles.forEach((file) => {
      const extension = getFileExtension(file.name);
      if (!SUPPORTED_EXTENSIONS.includes(extension as (typeof SUPPORTED_EXTENSIONS)[number])) {
        addImmediateErrorTask(file.name, file.size, '当前文件类型暂不支持上传。');
        return;
      }

      if (file.size > MAX_FILE_SIZE) {
        addImmediateErrorTask(file.name, file.size, '单文件最大支持 4GB，请更换文件后重试。');
        return;
      }

      const fingerprint = createFingerprint(file);
      const existingTask = tasksRef.current.find((task) => task.fingerprint === fingerprint && task.status !== 'completed');
      if (existingTask) {
        updateTask(existingTask.id, (current) => ({
          ...current,
          file,
          fileName: file.name,
          fileSize: file.size,
          totalChunks: Math.max(1, Math.ceil(file.size / CHUNK_SIZE)),
          status:
            current.uploadId || current.uploadedChunkCount > 0
              ? 'paused'
              : current.status === 'failed'
                ? 'failed'
                : 'queued',
          errorMessage: current.uploadId ? undefined : current.errorMessage,
          needsFileReselect: false,
          note:
            current.uploadId || current.uploadedChunkCount > 0
              ? '已匹配到原文件，可继续上传。'
              : '文件已在列表中，可直接开始上传。',
          lastUpdatedAt: Date.now(),
        }));
        return;
      }

      nextTasks.push({
        id: createTaskId(),
        fingerprint,
        file,
        fileName: file.name,
        fileSize: file.size,
        progress: 0,
        status: 'queued',
        note: '已选择文件，点击开始上传。',
        totalChunks: Math.max(1, Math.ceil(file.size / CHUNK_SIZE)),
        uploadedChunkCount: 0,
        resumable: true,
        needsFileReselect: false,
        lastUpdatedAt: Date.now(),
      });
    });

    if (nextTasks.length) {
      setTasks((current) => [...current, ...nextTasks]);
      setPageMessage('');
    }
  };

  const handleFileInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    handleFiles(Array.from(event.target.files || []));
    event.target.value = '';
  };

  const handleRetry = (task: UploadTask) => {
    if (task.needsFileReselect || !task.file) {
      inputRef.current?.click();
      setPageMessage('重新选择同一个原文件后，可从已保留的进度继续上传。');
      return;
    }

    updateTask(task.id, (current) => ({
      ...current,
      status: 'paused',
      note: current.uploadId ? '已加入继续上传队列。' : '已加入上传队列。',
      errorMessage: undefined,
      lastUpdatedAt: Date.now(),
    }));
    setBatchCompletedAt(null);
    setBatchStartedAt((current) => current ?? Date.now());
    setBatchRunning(true);
  };

  const handleStartUpload = () => {
    const readyTasks = tasksRef.current.filter(
      (task) =>
        task.file &&
        !task.needsFileReselect &&
        (task.status === 'queued' || task.status === 'paused' || task.status === 'failed'),
    );
    if (!readyTasks.length) {
      setPageMessage('请先选择文件。');
      return;
    }

    setTasks((current) =>
      current.map((task) => {
        if (!task.file || task.needsFileReselect) return task;
        if (task.status === 'queued') {
          return {
            ...task,
            note: '等待开始上传...',
            errorMessage: undefined,
            lastUpdatedAt: Date.now(),
          };
        }
        return task;
      }),
    );

    setBatchStartedAt((current) => current ?? Date.now());
    setBatchCompletedAt(null);
    setTransferSpeed(0);
    speedSampleRef.current = null;
    setBatchRunning(true);
  };

  const handlePauseBatch = () => {
    setBatchRunning(false);
    setTasks((current) =>
      current.map((task) => {
        if (task.status === 'queued') {
          return {
            ...task,
            note: '上传已暂停，恢复后会从队列继续。',
            lastUpdatedAt: Date.now(),
          };
        }
        if (task.status === 'uploading' || task.status === 'resuming') {
          return {
            ...task,
            status: 'paused',
            note: '上传已暂停，可稍后继续。',
            lastUpdatedAt: Date.now(),
          };
        }
        return task;
      }),
    );
  };

  const handleRemoveTask = async (task: UploadTask) => {
    if (task.uploadId && task.status !== 'completed') {
      try {
        await cancelFileEasyUpload(task.uploadId);
      } catch (error) {
        console.warn('Failed to cancel upload task', error);
      }
    }
    removeTask(task.id);
  };

  const handleResetCompleted = () => {
    setTasks([]);
    setBatchRunning(false);
    setBatchStartedAt(null);
    setBatchCompletedAt(null);
    setTransferSpeed(0);
    speedSampleRef.current = null;
    setPageMessage('');
  };

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;

    const handleOnline = () => {
      setIsOnline(true);
      void checkServiceAvailability();
    };
    const handleOffline = () => {
      setIsOnline(false);
      setServiceStatus('unavailable');
      setBatchRunning(false);
      setTasks((current) =>
        current.map((task) => {
          if (task.status === 'uploading' || task.status === 'resuming') {
            return {
              ...task,
              status: 'paused',
              note: '网络中断，已保留当前进度，恢复连接后可继续上传。',
              lastUpdatedAt: Date.now(),
            };
          }
          return task;
        }),
      );
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    void checkServiceAvailability();

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;
    const timer = window.setInterval(() => {
      void checkServiceAvailability();
    }, 12000);
    return () => {
      window.clearInterval(timer);
    };
  }, [pageMessage]);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const serializableTasks = tasks
      .filter((task) => task.uploadId && task.status !== 'completed')
      .map<StoredUploadTask>(({ file: _file, ...task }) => task);
    window.localStorage.setItem(TASK_STORAGE_KEY, JSON.stringify(serializableTasks));
  }, [tasks]);

  useEffect(() => {
    if (!isAuthenticated) {
      setTasks((current) =>
        current.map((task) => {
          if (task.status === 'uploading' || task.status === 'resuming') {
            return {
              ...task,
              status: 'paused',
              note: '登录后可以继续当前上传任务。',
              lastUpdatedAt: Date.now(),
            };
          }
          return task;
        }),
      );
      return;
    }

    void refreshStoredStatuses();
  }, [isAuthenticated]);

  useEffect(() => {
    if (!batchRunning || !isAuthenticated || !isOnline || serviceStatus !== 'ready') return;
    if (runningTasksRef.current.size > 0) return;

    const nextTask = tasksRef.current.find(
      (task) =>
        task.file &&
        !task.needsFileReselect &&
        (task.status === 'queued' || task.status === 'paused' || task.status === 'failed'),
    );

    if (!nextTask) {
      setBatchRunning(false);
      return;
    }

    if (nextTask.file) {
      void startUpload(nextTask.file, nextTask.id);
    }
  }, [batchRunning, isAuthenticated, isOnline, serviceStatus, tasks]);

  useEffect(() => {
    const totalTrackedBytes = tasks.reduce((sum, task) => {
      if (task.status === 'failed' && !task.uploadId) return sum;
      return sum + task.fileSize;
    }, 0);
    const uploadedBytes = tasks.reduce((sum, task) => sum + task.fileSize * (task.progress / 100), 0);

    if (!batchRunning) {
      speedSampleRef.current = null;
      setTransferSpeed(0);
      return undefined;
    }

    const timer = window.setInterval(() => {
      const now = Date.now();
      const previousSample = speedSampleRef.current;
      if (!previousSample) {
        speedSampleRef.current = { bytes: uploadedBytes, time: now };
        return;
      }

      const elapsedSeconds = (now - previousSample.time) / 1000;
      const byteDelta = uploadedBytes - previousSample.bytes;
      if (elapsedSeconds > 0 && byteDelta >= 0) {
        setTransferSpeed(byteDelta / elapsedSeconds);
      }
      speedSampleRef.current = { bytes: uploadedBytes, time: now };
    }, 1000);

    if (totalTrackedBytes > 0 && uploadedBytes >= totalTrackedBytes) {
      setTransferSpeed(0);
    }

    return () => {
      window.clearInterval(timer);
    };
  }, [batchRunning, tasks]);

  useEffect(() => {
    if (!tasks.length) {
      setBatchRunning(false);
      setBatchStartedAt(null);
      setBatchCompletedAt(null);
      setTransferSpeed(0);
      return;
    }

    if (tasks.every((task) => task.status === 'completed')) {
      if (!batchCompletedAt) {
        setBatchCompletedAt(Date.now());
      }
      setBatchRunning(false);
      setTransferSpeed(0);
      return;
    }

    if (batchCompletedAt) {
      setBatchCompletedAt(null);
    }
  }, [batchCompletedAt, tasks]);

  const totalFiles = tasks.length;
  const readyToStartCount = tasks.filter((task) => task.status === 'queued' && task.file).length;
  const completedTasks = tasks.filter((task) => task.status === 'completed');
  const totalTrackedSize = tasks.reduce((sum, task) => {
    if (task.status === 'failed' && !task.uploadId) return sum;
    return sum + task.fileSize;
  }, 0);
  const completedSize = completedTasks.reduce((sum, task) => sum + task.fileSize, 0);
  const overallUploadedBytes = tasks.reduce((sum, task) => sum + task.fileSize * (task.progress / 100), 0);
  const overallProgress =
    totalTrackedSize > 0 ? Math.max(0, Math.min(100, Math.round((overallUploadedBytes / totalTrackedSize) * 100))) : 0;
  const remainingSeconds =
    transferSpeed > 0 && totalTrackedSize > overallUploadedBytes
      ? (totalTrackedSize - overallUploadedBytes) / transferSpeed
      : null;
  const uploadDuration =
    batchStartedAt && (batchCompletedAt || batchRunning)
      ? (batchCompletedAt || Date.now()) - batchStartedAt
      : 0;

  const hasStartedUpload = tasks.some(
    (task) =>
      Boolean(task.uploadId) ||
      task.status === 'uploading' ||
      task.status === 'resuming' ||
      task.status === 'paused' ||
      task.status === 'completed',
  );

  let viewState: UploadViewState = 'file-selection';
  if (!isOnline || serviceStatus === 'unavailable') {
    viewState = 'network-unavailable';
  } else if (!isAuthenticated) {
    viewState = 'auth-required';
  } else if (tasks.length > 0 && tasks.every((task) => task.status === 'completed')) {
    viewState = 'upload-complete';
  } else if (hasStartedUpload) {
    viewState = 'uploading';
  }

  return (
    <div className="fileeasy-upload-page">
      <div className="fileeasy-upload-shell">
        <header className="fileeasy-flow-header">
          <YiTransferBrand caption="局域网扫码传文件" />
          <div className="fileeasy-flow-header__status">
            <span className={`fileeasy-connection-pill ${serviceStatus === 'ready' ? 'ready' : 'muted'}`}>
              <span className="fileeasy-connection-pill__dot" />
              {serviceStatus === 'ready' ? '已连接' : serviceStatus === 'checking' ? '检测中' : '未连接'}
            </span>
          </div>
        </header>

        {pageMessage ? <div className="fileeasy-flow-banner">{pageMessage}</div> : null}

        <input
          ref={inputRef}
          accept={FILE_PICKER_ACCEPT}
          className="fileeasy-hidden-input"
          multiple
          type="file"
          onChange={handleFileInputChange}
        />

        {viewState === 'network-unavailable' ? (
          <section className="fileeasy-flow-card fileeasy-flow-card--centered">
            <div className="fileeasy-hero-icon fileeasy-hero-icon--danger">╳</div>
            <div className="fileeasy-flow-copy">
              <h1>无法连接到易传输服务</h1>
              <p>请确认你的手机已连接到与设备相同的 Wi-Fi 网络或热点。</p>
            </div>

            <div className="fileeasy-network-card">
              <span>当前网络</span>
              <strong>{serviceStatus === 'checking' ? '正在检测局域网服务' : '未检测到局域网服务'}</strong>
              <small>需要连接</small>
              <code>{connectionAddress}</code>
            </div>

            <Button
              block
              size="lg"
              variant="primary"
              onClick={() => {
                setPageMessage('');
                void checkServiceAvailability();
              }}
            >
              重新检测
            </Button>
          </section>
        ) : null}

        {viewState === 'auth-required' ? (
          <section className="fileeasy-flow-card">
            <div className="fileeasy-flow-copy">
              <h1>输入访问密码</h1>
              <p>连接到 {connectionAddress}</p>
            </div>

            <div className="fileeasy-network-inline">
              <span className="fileeasy-connection-pill ready">
                <span className="fileeasy-connection-pill__dot" />
                {connectionAddress}
              </span>
            </div>

            <label className="fileeasy-field">
              <span>访问密码</span>
              <div className="fileeasy-password-field">
                <Input
                  aria-invalid={Boolean(loginError)}
                  placeholder="请输入访问密码"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(event) => {
                    setPassword(event.target.value);
                    if (loginError) setLoginError('');
                  }}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault();
                      void handleLogin();
                    }
                  }}
                />
                <button
                  className="fileeasy-password-toggle"
                  type="button"
                  onClick={() => setShowPassword((current) => !current)}
                >
                  {showPassword ? '隐藏' : '显示'}
                </button>
              </div>
            </label>

            {loginError ? <div className="fileeasy-error-text">{loginError}</div> : null}

            <Button block size="lg" variant="primary" onClick={() => void handleLogin()} disabled={isLoggingIn}>
              {isLoggingIn ? '登录中...' : '登录'}
            </Button>

            <label className="fileeasy-remember-row">
              <input
                checked={rememberSession}
                type="checkbox"
                onChange={(event) => setRememberSession(event.target.checked)}
              />
              <span>7 天内记住登录状态</span>
            </label>
          </section>
        ) : null}

        {viewState === 'file-selection' ? (
          <section className="fileeasy-flow-card">
            <div className="fileeasy-flow-copy">
              <h1>上传文件</h1>
              <p>选择要发送到设备的文件</p>
            </div>

            <button
              className={`fileeasy-dropzone ${isDraggingFiles ? 'is-dragging' : ''}`}
              type="button"
              onClick={() => inputRef.current?.click()}
              onDragEnter={(event) => {
                event.preventDefault();
                setIsDraggingFiles(true);
              }}
              onDragLeave={(event) => {
                event.preventDefault();
                if (event.currentTarget.contains(event.relatedTarget as Node | null)) return;
                setIsDraggingFiles(false);
              }}
              onDragOver={(event) => {
                event.preventDefault();
                setIsDraggingFiles(true);
              }}
              onDrop={(event) => {
                event.preventDefault();
                setIsDraggingFiles(false);
                handleFiles(Array.from(event.dataTransfer.files || []));
              }}
            >
              <div className="fileeasy-dropzone__icon">⇪</div>
              <strong>点击选择文件</strong>
              <span>支持文件、图片、视频混合多选</span>
              <small>单文件最大 4 GB</small>
              <div className="fileeasy-format-badges">
                {SUPPORTED_EXTENSION_BADGES.map((badge) => (
                  <span key={badge}>{badge}</span>
                ))}
              </div>
            </button>

            <div className="fileeasy-selection-caption">
              已选择 {totalFiles} 个文件
              {readyToStartCount > 0 ? `，其中 ${readyToStartCount} 个可立即开始上传` : ''}
            </div>

            <div className="fileeasy-selection-content">
              {tasks.length ? (
                <div className="fileeasy-selected-list">
                  {tasks.map((task) => (
                    <article key={task.id} className={`fileeasy-selected-item ${task.status}`}>
                      <div className="fileeasy-selected-item__main">
                        <div className={`fileeasy-file-tag ${task.status}`}>
                          {getFileExtension(task.fileName).toUpperCase() || 'FILE'}
                        </div>
                        <div>
                          <strong>{task.finalName || task.fileName}</strong>
                          <span>{formatBytes(task.fileSize)}</span>
                          <small>{task.note}</small>
                        </div>
                      </div>
                      <div className="fileeasy-selected-item__actions">
                        {(task.status === 'paused' || task.status === 'failed') && task.uploadId ? (
                          <button type="button" onClick={() => handleRetry(task)}>
                            继续
                          </button>
                        ) : null}
                        <button type="button" onClick={() => void handleRemoveTask(task)}>
                          移除
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="fileeasy-empty-hint">选择文件后，列表会显示在这里。</div>
              )}
            </div>

            <div className="fileeasy-selection-footer">
              <div className="fileeasy-selection-footer__summary">
                <strong>{totalFiles} 个已选</strong>
                <span>{readyToStartCount > 0 ? `${readyToStartCount} 个可立即上传` : '先选择文件后再开始上传'}</span>
              </div>
              <Button block size="lg" variant="primary" onClick={handleStartUpload} disabled={!readyToStartCount}>
                开始上传 {readyToStartCount > 0 ? `${readyToStartCount} 个` : ''}
              </Button>
            </div>
          </section>
        ) : null}

        {viewState === 'uploading' ? (
          <section className="fileeasy-flow-card">
            <div className="fileeasy-flow-card__header">
              <div className="fileeasy-flow-copy">
                <h1>上传中...</h1>
              </div>
              <button className="fileeasy-inline-link" type="button" onClick={handlePauseBatch}>
                {batchRunning ? '暂停' : '继续'}
              </button>
            </div>

            <div className="fileeasy-progress-summary">
              <div className="fileeasy-progress-ring">
                <div className="fileeasy-progress-ring__value">{overallProgress}%</div>
                <span>整体进度</span>
              </div>

              <div className="fileeasy-progress-summary__stats">
                <div>
                  <span>预计剩余</span>
                  <strong>{formatRemainingTime(remainingSeconds)}</strong>
                </div>
                <div>
                  <span>速度</span>
                  <strong>{transferSpeed > 0 ? `${formatBytes(transferSpeed)}/s` : '等待中'}</strong>
                </div>
                <div>
                  <span>文件</span>
                  <strong>{tasks.length} 个</strong>
                </div>
              </div>
            </div>

            <div className="fileeasy-upload-list">
              {tasks.map((task) => (
                <article key={task.id} className={`fileeasy-upload-item ${task.status}`}>
                  <div className="fileeasy-upload-item__head">
                    <div className={`fileeasy-file-tag ${task.status}`}>{getFileExtension(task.fileName).toUpperCase() || 'FILE'}</div>
                    <div className="fileeasy-upload-item__meta">
                      <strong>{task.finalName || task.fileName}</strong>
                      <span>
                        {formatBytes(task.fileSize)} / {task.status === 'completed' ? formatBytes(task.fileSize) : formatBytes((task.fileSize * task.progress) / 100)}
                      </span>
                    </div>
                    <div className="fileeasy-upload-item__status">
                      {task.status === 'completed'
                        ? '完成'
                        : task.status === 'uploading'
                          ? `${task.progress}%`
                          : task.status === 'resuming'
                            ? '恢复中'
                            : task.status === 'paused'
                              ? '已暂停'
                              : task.status === 'failed'
                                ? '失败'
                                : '排队中'}
                    </div>
                  </div>

                  <ProgressBar
                    tone={task.status === 'completed' ? 'success' : task.status === 'failed' ? 'danger' : 'default'}
                    value={task.progress}
                  />

                  <div className="fileeasy-upload-item__footer">
                    <small>{task.note}</small>
                    <div className="fileeasy-upload-item__actions">
                      {(task.status === 'paused' || task.status === 'failed') ? (
                        <button type="button" onClick={() => handleRetry(task)}>
                          {task.uploadId ? '继续上传' : '重新上传'}
                        </button>
                      ) : null}
                      <button type="button" onClick={() => void handleRemoveTask(task)}>
                        移除
                      </button>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          </section>
        ) : null}

        {viewState === 'upload-complete' ? (
          <section className="fileeasy-flow-card fileeasy-flow-card--centered">
            <div className="fileeasy-hero-icon fileeasy-hero-icon--success">✓</div>
            <div className="fileeasy-flow-copy">
              <h1>全部上传完成</h1>
              <p>文件已成功发送到设备，可在管理页查看。</p>
            </div>

            <div className="fileeasy-result-card">
              <div>
                <span>文件数量</span>
                <strong>{completedTasks.length} 个文件</strong>
              </div>
              <div>
                <span>总大小</span>
                <strong>{formatBytes(completedSize)}</strong>
              </div>
              <div>
                <span>用时</span>
                <strong>{formatDuration(uploadDuration)}</strong>
              </div>
              <div>
                <span>发送到</span>
                <strong>{connectionAddress}</strong>
              </div>
            </div>

            <Button
              block
              size="lg"
              variant="primary"
              onClick={() => {
                handleResetCompleted();
                window.setTimeout(() => {
                  inputRef.current?.click();
                }, 40);
              }}
            >
              继续上传文件
            </Button>
          </section>
        ) : null}
      </div>
    </div>
  );
};

export default UploadPage;
