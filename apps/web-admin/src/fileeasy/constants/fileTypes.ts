import type { FileKind, FolderKey } from '../types/file';

export const FILEEASY_SUPPORTED_EXTENSIONS = [
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

export const FILEEASY_FOLDER_LABELS: Record<FolderKey, string> = {
  all: '全部文件',
  document: '文档',
  video: '视频',
  image: '图片',
  audio: '音频',
  archive: '压缩包',
};

export const FILEEASY_KIND_TO_FOLDER: Record<FileKind, FolderKey> = {
  PDF: 'document',
  文档: 'document',
  视频: 'video',
  图片: 'image',
  音频: 'audio',
  ZIP: 'archive',
};
