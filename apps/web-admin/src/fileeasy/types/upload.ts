export type UploadTaskStatus =
  | 'queued'
  | 'uploading'
  | 'paused'
  | 'resuming'
  | 'failed'
  | 'completed';

export type UploadTask = {
  id: string;
  fingerprint: string;
  uploadId?: string;
  file?: File;
  fileName: string;
  fileSize: number;
  progress: number;
  status: UploadTaskStatus;
  note: string;
  errorMessage?: string;
  totalChunks: number;
  uploadedChunkCount: number;
  expiresAt?: string;
  finalName?: string;
  resumable: boolean;
  needsFileReselect: boolean;
  lastUpdatedAt: number;
};

export type StoredUploadTask = Omit<UploadTask, 'file'>;

export type UploadInitPayload = {
  fileName: string;
  fileSize: number;
  chunkSize: number;
  totalChunks: number;
  mimeType?: string;
};

export type UploadInitResponse = {
  uploadId: string;
  chunkSize?: number;
  totalChunks?: number;
  uploadedChunkIndexes?: number[];
  status?: string;
};
