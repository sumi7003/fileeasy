export type ApkMode = 'first-install' | 'normal' | 'network-missing';

export type ApkDialog = 'none' | 'welcome' | 'password' | 'network';

export type ServiceStage = 'booting' | 'foreground' | 'ready';

export type ServiceStep = {
  detail: string;
  key: ServiceStage;
  title: string;
};

export type HomeUploadTask = {
  createdAt: number;
  fileName: string;
  progress: number;
  status: string;
  totalChunks: number;
  updatedAt: number;
  uploadId: string;
  uploadedChunks: number;
};

export type HomeRecentFile = {
  category: string;
  createdAt: number;
  fileName: string;
  id: string;
  size: number;
};

export type HomeSummary = {
  activeUploads: HomeUploadTask[];
  recentFiles: HomeRecentFile[];
  uploadUrl: string;
};
