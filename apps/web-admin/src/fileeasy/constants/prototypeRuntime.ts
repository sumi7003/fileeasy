import type {
  FileEasyPrototypeStatusCopy,
  FileEasyPrototypeStatusToneMap,
  FileEasyPrototypeStorageKeys,
} from '../types/prototype';

export const FILEEASY_PROTOTYPE_STATUS_COPY: FileEasyPrototypeStatusCopy = {
  queued: { label: '等待上传', className: 'queued' },
  uploading: { label: '上传中', className: 'uploading' },
  restoring: { label: '正在恢复', className: 'restoring', note: '正在恢复上传任务' },
  done: { label: '上传完成', className: 'done' },
  failed: { label: '上传失败', className: 'failed', note: '设备空间不足，请联系管理员清理' },
};

export const FILEEASY_PROTOTYPE_UPLOAD_STATUS_TONE_MAP: FileEasyPrototypeStatusToneMap = {
  queued: 'neutral',
  uploading: 'info',
  restoring: 'warning',
  done: 'success',
  failed: 'danger',
};

export const FILEEASY_PROTOTYPE_DEFAULT_PASSWORD = '123456';

export const FILEEASY_PROTOTYPE_DEMO_STORAGE_KEYS: FileEasyPrototypeStorageKeys = {
  onboardingDone: 'fileeasy-demo-onboarding-done',
  password: 'fileeasy-demo-password',
};

export const FILEEASY_PROTOTYPE_APK_PAGE_STORAGE_KEYS: FileEasyPrototypeStorageKeys = {
  onboardingDone: 'fileeasy-apk-page-onboarding-done',
  password: 'fileeasy-apk-page-password',
};

export const readFileEasyPrototypeOnboardingDone = (
  storageKeys: FileEasyPrototypeStorageKeys,
): boolean => {
  if (typeof window === 'undefined') return false;
  return window.localStorage.getItem(storageKeys.onboardingDone) === '1';
};

export const readFileEasyPrototypePassword = (
  storageKeys: FileEasyPrototypeStorageKeys,
): string => {
  if (typeof window === 'undefined') return FILEEASY_PROTOTYPE_DEFAULT_PASSWORD;
  return (
    window.localStorage.getItem(storageKeys.password) ??
    FILEEASY_PROTOTYPE_DEFAULT_PASSWORD
  );
};

export const writeFileEasyPrototypeOnboardingDone = (
  storageKeys: FileEasyPrototypeStorageKeys,
  done: boolean,
): void => {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(storageKeys.onboardingDone, done ? '1' : '0');
};

export const writeFileEasyPrototypePassword = (
  storageKeys: FileEasyPrototypeStorageKeys,
  password: string,
): void => {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(storageKeys.password, password);
};
