type FileEasyApkStorageKeys = {
  onboardingDone: string;
  password: string;
};

export const FILEEASY_APK_DEFAULT_PASSWORD = '123456';

export const FILEEASY_APK_PAGE_STORAGE_KEYS: FileEasyApkStorageKeys = {
  onboardingDone: 'fileeasy-apk-page-onboarding-done',
  password: 'fileeasy-apk-page-password',
};

export const readFileEasyApkOnboardingDone = (
  storageKeys: FileEasyApkStorageKeys = FILEEASY_APK_PAGE_STORAGE_KEYS,
): boolean => {
  if (typeof window === 'undefined') return false;
  return window.localStorage.getItem(storageKeys.onboardingDone) === '1';
};

export const readFileEasyApkPassword = (
  storageKeys: FileEasyApkStorageKeys = FILEEASY_APK_PAGE_STORAGE_KEYS,
): string => {
  if (typeof window === 'undefined') return FILEEASY_APK_DEFAULT_PASSWORD;
  return window.localStorage.getItem(storageKeys.password) ?? FILEEASY_APK_DEFAULT_PASSWORD;
};

export const writeFileEasyApkOnboardingDone = (
  storageKeys: FileEasyApkStorageKeys = FILEEASY_APK_PAGE_STORAGE_KEYS,
  done: boolean,
): void => {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(storageKeys.onboardingDone, done ? '1' : '0');
};

export const writeFileEasyApkPassword = (
  storageKeys: FileEasyApkStorageKeys = FILEEASY_APK_PAGE_STORAGE_KEYS,
  password: string,
): void => {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(storageKeys.password, password);
};
