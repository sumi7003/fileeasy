import request from './request';

export interface FileEasyLoginResponse {
  status?: string;
  token?: string;
  expiresAt?: string;
}

export interface FileEasyUploadInitPayload {
  fileName: string;
  fileSize: number;
  chunkSize: number;
  totalChunks: number;
  mimeType?: string;
}

export interface FileEasyUploadInitResponse {
  uploadId: string;
  chunkSize?: number;
  totalChunks?: number;
  uploadedChunks?: number;
  uploadedChunkIndexes?: number[];
  status?: string;
  expiresAt?: string;
  createdAt?: string;
}

export interface FileEasyUploadStatusResponse {
  uploadId?: string;
  totalChunks: number;
  uploadedChunks?: number;
  uploadedChunkIndexes: number[];
  missingChunkIndexes?: number[];
  status?: string;
  expiresAt?: string;
  createdAt?: string;
}

export interface FileEasyUploadCompletePayload {
  uploadId: string;
  fileName: string;
  fileSize: number;
  totalChunks: number;
}

export interface FileEasyUploadCompleteResponse {
  id?: string;
  fileId?: string;
  fileName?: string;
  displayName?: string;
  storedFilename?: string;
  message?: string;
}

export interface FileEasyUploadChunkPayload {
  uploadId: string;
  chunkIndex: number;
  totalChunks: number;
  fileName: string;
  fileSize: number;
  chunk: Blob;
}

export interface FileEasyManagedFileResponse {
  id: string;
  originalName?: string;
  displayName?: string;
  storedFilename?: string;
  filename?: string;
  url?: string;
  type?: string;
  category?: string;
  extension?: string;
  mimeType?: string;
  previewSupported?: boolean;
  size: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface FileEasyHomeUploadTaskResponse {
  createdAt: number;
  fileName: string;
  progress: number;
  status: string;
  totalChunks: number;
  updatedAt: number;
  uploadId: string;
  uploadedChunks: number;
}

export interface FileEasyHomeRecentFileResponse {
  category: string;
  createdAt: number;
  fileName: string;
  id: string;
  size: number;
}

export interface FileEasyHomeSummaryResponse {
  activeUploads: FileEasyHomeUploadTaskResponse[];
  passwordRequired: boolean;
  recentFiles: FileEasyHomeRecentFileResponse[];
  uploadUrl: string;
}

export const loginFileEasy = (password: string) => {
  return request.post<any, FileEasyLoginResponse>('/auth/login', {
    username: 'admin',
    password,
  });
};

export const logoutFileEasy = () => {
  return request.post('/auth/logout');
};

export const initFileEasyUpload = (payload: FileEasyUploadInitPayload) => {
  return request.post<any, FileEasyUploadInitResponse>('/upload/init', payload);
};

export const getFileEasyUploadStatus = (uploadId: string) => {
  return request.get<any, FileEasyUploadStatusResponse>(`/upload/status/${uploadId}`);
};

export const cancelFileEasyUpload = (uploadId: string) => {
  return request.delete(`/upload/status/${uploadId}`);
};

export const uploadFileEasyChunk = (
  payload: FileEasyUploadChunkPayload,
  onProgress?: (percent: number) => void,
) => {
  const formData = new FormData();
  formData.append('uploadId', payload.uploadId);
  formData.append('chunkIndex', String(payload.chunkIndex));
  formData.append('totalChunks', String(payload.totalChunks));
  formData.append('fileName', payload.fileName);
  formData.append('fileSize', String(payload.fileSize));
  formData.append('chunk', payload.chunk, payload.fileName);

  return request.post('/upload/chunk', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (event) => {
      if (!event.total) return;
      onProgress?.(Math.round((event.loaded / event.total) * 100));
    },
  });
};

export const completeFileEasyUpload = (payload: FileEasyUploadCompletePayload) => {
  return request.post<any, FileEasyUploadCompleteResponse>('/upload/complete', payload);
};

export const listManagedFileEasyFiles = () => {
  return request.get<any, FileEasyManagedFileResponse[]>('/files');
};

export const renameManagedFileEasyFile = (id: string, baseName: string) => {
  return request.patch<any, FileEasyManagedFileResponse>(`/files/${id}`, {
    baseName,
  });
};

export const deleteManagedFileEasyFile = (id: string) => {
  return request.delete(`/files/${id}`);
};

export const batchDeleteManagedFileEasyFiles = (ids: string[]) => {
  return request.post('/files/batch-delete', { ids });
};

export const getFileEasyHomeSummary = () => {
  return request.get<any, FileEasyHomeSummaryResponse>('/home/summary');
};
