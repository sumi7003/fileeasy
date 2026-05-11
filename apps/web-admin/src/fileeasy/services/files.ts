import {
  batchDeleteManagedFileEasyFiles,
  deleteManagedFileEasyFile,
  listManagedFileEasyFiles,
  renameManagedFileEasyFile,
  type FileEasyManagedFileResponse,
} from '../../api/fileeasy';
import type { FileItem, FileKind, FolderKey } from '../types/file';

type RenameOptions = {
  file: FileItem;
  nextBaseName: string;
};

type RenameOutcome = {
  finalName: string;
};

const previewMimeTypes: Record<string, string> = {
  pdf: 'application/pdf',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  gif: 'image/gif',
  webp: 'image/webp',
  mp4: 'video/mp4',
  mov: 'video/quicktime',
  mp3: 'audio/mpeg',
  wav: 'audio/wav',
  m4a: 'audio/mp4',
};

const extractExtension = (fileName: string) => {
  const segments = fileName.split('.');
  return segments.length > 1 ? segments.pop()!.toLowerCase() : '';
};

const splitFileName = (fileName: string) => {
  const extension = extractExtension(fileName);
  if (!extension) {
    return { baseName: fileName, extension: '' };
  }
  return {
    baseName: fileName.slice(0, -(extension.length + 1)),
    extension,
  };
};

const inferFolder = (type: string, extension: string): FolderKey => {
  if (extension === 'pdf') return 'document';
  if (type === 'image') return 'image';
  if (type === 'video') return 'video';
  if (type === 'audio') return 'audio';
  if (type === 'archive') return 'archive';
  return 'document';
};

const inferKind = (folder: FolderKey, extension: string): FileKind => {
  if (extension === 'pdf') return 'PDF';
  if (extension === 'apk') return 'APK';
  if (folder === 'image') return '图片';
  if (folder === 'video') return '视频';
  if (folder === 'audio') return '音频';
  if (folder === 'archive') return 'ZIP';
  return '文档';
};

const isPreviewable = (folder: FolderKey, extension: string) =>
  extension === 'pdf' || folder === 'image' || folder === 'video' || folder === 'audio';

const formatBytes = (bytes: number) => {
  if (!Number.isFinite(bytes)) return '-';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let value = bytes;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  const decimals = value >= 100 || unitIndex === 0 ? 0 : 1;
  return `${value.toFixed(decimals)} ${units[unitIndex]}`;
};

const formatTimestamp = (value?: string) => {
  if (!value) return '-';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  const year = parsed.getFullYear();
  const month = `${parsed.getMonth() + 1}`.padStart(2, '0');
  const date = `${parsed.getDate()}`.padStart(2, '0');
  const hours = `${parsed.getHours()}`.padStart(2, '0');
  const minutes = `${parsed.getMinutes()}`.padStart(2, '0');
  return `${year}-${month}-${date} ${hours}:${minutes}`;
};

const normalizeType = (record: FileEasyManagedFileResponse, extension: string) => {
  const rawType = (record.category || record.type || '').toLowerCase();
  if (rawType === 'image' || rawType === 'video' || rawType === 'audio' || rawType === 'archive') {
    return rawType;
  }
  if (rawType === 'document') return rawType;
  if (extension === 'zip' || extension === 'apk') return 'archive';
  if (extension === 'pdf') return 'document';
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(extension)) return 'image';
  if (['mp4', 'mov'].includes(extension)) return 'video';
  if (['mp3', 'wav', 'm4a'].includes(extension)) return 'audio';
  return 'document';
};

const toFileItem = (record: FileEasyManagedFileResponse): FileItem => {
  const name =
    record.displayName ||
    record.originalName ||
    record.storedFilename ||
    record.filename ||
    '未命名文件';
  const { baseName, extension } = splitFileName(name);
  const normalizedType = normalizeType(
    record,
    record.extension?.toLowerCase() || extractExtension(name) || extractExtension(record.filename || ''),
  );
  const folder = inferFolder(normalizedType, extension);

  return {
    id: record.id,
    name,
    baseName,
    extension,
    kind: inferKind(folder, extension),
    folder,
    size: formatBytes(record.size),
    sizeBytes: record.size,
    time: formatTimestamp(record.createdAt),
    previewable: record.previewSupported ?? isPreviewable(folder, extension),
    previewUrl: `/api/v1/files/${record.id}/preview`,
    downloadUrl: `/api/v1/files/${record.id}/download`,
    createdAt: record.createdAt || '',
    source: 'files-api',
  };
};

export const fileEasyFilesService = {
  listFiles: async (): Promise<FileItem[]> => {
    const records = await listManagedFileEasyFiles();
    return records.map((record) => toFileItem(record));
  },
  createPreviewUrl: async (file: FileItem): Promise<string> => {
    if (file.folder !== 'audio' && file.extension !== 'pdf') {
      return file.previewUrl;
    }

    const response = await fetch(file.previewUrl, {
      credentials: 'same-origin',
    });
    if (!response.ok) {
      throw new Error(response.status === 401 ? '登录已失效，请重新输入密码。' : '预览加载失败，请稍后重试。');
    }

    const bytes = await response.arrayBuffer();
    const mimeType = previewMimeTypes[file.extension] || 'application/octet-stream';
    const blob = new Blob([bytes], { type: mimeType });
    return URL.createObjectURL(blob);
  },
  renameFile: async ({ file, nextBaseName }: RenameOptions): Promise<RenameOutcome> => {
    const response = await renameManagedFileEasyFile(file.id, nextBaseName);
    return {
      finalName:
        response.displayName ||
        response.originalName ||
        (file.extension ? `${nextBaseName}.${file.extension}` : nextBaseName),
    };
  },
  deleteFile: async (file: FileItem) => {
    await deleteManagedFileEasyFile(file.id);
  },
  batchDeleteFiles: async (files: FileItem[]) => {
    if (files.length === 0) return;
    await batchDeleteManagedFileEasyFiles(files.map((file) => file.id));
  },
};

export type { RenameOutcome };
