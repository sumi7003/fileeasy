import request from './request';

export interface TransferFile {
  id: string;
  originalName: string;
  size: number;
  downloadCount: number;
  expiresAt: number;
  createdAt: number;
  uploaderIp?: string;
  shareUrl?: string;
  remainingDays: number;
}

export interface TransferLog {
  id: string;
  action: string;
  ip?: string;
  clientRemark?: string;
  result?: string;
  time: number;
}

export interface TransferStorageStatus {
  usedMB: number;
  freeMB: number;
  warn: boolean;
  blocked: boolean;
}

export const uploadTransferFile = (file: File, onProgress?: (percent: number) => void) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post<any, TransferFile>('/transfer/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (evt) => {
      if (!evt.total) return;
      const percent = Math.round((evt.loaded / evt.total) * 100);
      onProgress?.(percent);
    },
  });
};

export const getTransferFiles = () => {
  return request.get<any, TransferFile[]>('/transfer/files');
};

export const deleteTransferFile = (id: string) => {
  return request.delete(`/transfer/files/${id}`);
};

export const getTransferShare = (id: string) => {
  return request.get<any, { shareUrl: string; qrContent: string }>(`/transfer/files/${id}/share`);
};

export const getTransferLogs = (id: string) => {
  return request.get<any, TransferLog[]>(`/transfer/files/${id}/logs`);
};

export const getTransferQrUrl = (id: string) => `/api/v1/transfer/files/${id}/qr`;

export const getTransferStorage = () => {
  return request.get<any, TransferStorageStatus>('/transfer/storage');
};
