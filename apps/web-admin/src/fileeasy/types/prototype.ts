import type { FolderKey } from './file';

export type FileEasyPrototypeStage = 'APK 端' | '扫码端' | '管理端';

export type FileEasyPrototypeSceneId =
  | 'apk-home'
  | 'upload-login'
  | 'upload-active'
  | 'upload-alerts'
  | 'admin-list'
  | 'admin-batch';

export type FileEasyPrototypeHint = {
  label: string;
  detail: string;
};

export type FileEasyPrototypeUploadStatus =
  | 'queued'
  | 'uploading'
  | 'restoring'
  | 'done'
  | 'failed';

export type FileEasyPrototypeStatusTone =
  | 'neutral'
  | 'info'
  | 'success'
  | 'warning'
  | 'danger';

export type FileEasyPrototypeStatusCopyItem = {
  className: string;
  label: string;
  note?: string;
};

export type FileEasyPrototypeStatusCopy = Record<
  FileEasyPrototypeUploadStatus,
  FileEasyPrototypeStatusCopyItem
>;

export type FileEasyPrototypeStatusToneMap = Record<
  FileEasyPrototypeUploadStatus,
  FileEasyPrototypeStatusTone
>;

export type FileEasyPrototypeDemoFileKind = 'PDF' | '视频' | '图片' | '音频' | 'ZIP';

export type FileEasyPrototypeUploadTask = {
  id: string;
  name: string;
  size: string;
  progress: number;
  status: FileEasyPrototypeUploadStatus;
  note?: string;
};

export type FileEasyPrototypeDemoFile = {
  id: string;
  name: string;
  kind: FileEasyPrototypeDemoFileKind;
  folder: FolderKey;
  size: string;
  time: string;
  previewable: boolean;
};

export type FileEasyPrototypeFolderOption = {
  key: FolderKey;
  label: string;
  hint: string;
};

export type FileEasyPrototypeStorageKeys = {
  onboardingDone: string;
  password: string;
};

export type FileEasyPrototypeSceneMeta = {
  eyebrow: string;
  id: FileEasyPrototypeSceneId;
  interactionHints: FileEasyPrototypeHint[];
  label: string;
  stage: FileEasyPrototypeStage;
  summary: string;
};
