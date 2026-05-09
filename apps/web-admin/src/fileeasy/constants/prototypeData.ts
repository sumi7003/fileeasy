import type { FolderKey } from '../types/file';
import type {
  FileEasyPrototypeDemoFile,
  FileEasyPrototypeDemoFileKind,
  FileEasyPrototypeFolderOption,
  FileEasyPrototypeUploadTask,
} from '../types/prototype';

export const FILEEASY_PROTOTYPE_DEMO_FILES: FileEasyPrototypeDemoFile[] = [
  {
    id: 'f1',
    name: '项目方案.pdf',
    kind: 'PDF',
    folder: 'document',
    size: '12.4 MB',
    time: '2026-05-07 14:20',
    previewable: true,
  },
  {
    id: 'f2',
    name: '宣传视频.mp4',
    kind: '视频',
    folder: 'video',
    size: '256 MB',
    time: '2026-05-07 12:10',
    previewable: true,
  },
  {
    id: 'f3',
    name: '门店海报.webp',
    kind: '图片',
    folder: 'image',
    size: '3.2 MB',
    time: '2026-05-07 10:15',
    previewable: true,
  },
  {
    id: 'f5',
    name: '门店广播.m4a',
    kind: '音频',
    folder: 'audio',
    size: '18.6 MB',
    time: '2026-05-07 09:32',
    previewable: true,
  },
  {
    id: 'f4',
    name: '合同归档.zip',
    kind: 'ZIP',
    folder: 'archive',
    size: '88 MB',
    time: '2026-05-06 18:42',
    previewable: false,
  },
];

export const FILEEASY_PROTOTYPE_FOLDER_OPTIONS: FileEasyPrototypeFolderOption[] = [
  { key: 'all', label: '全部文件', hint: '查看所有类型' },
  { key: 'document', label: '文档', hint: 'PDF / Word / Excel / PPT / TXT' },
  { key: 'video', label: '视频', hint: 'MP4 / MOV' },
  { key: 'image', label: '图片', hint: 'JPG / PNG / WEBP / GIF' },
  { key: 'audio', label: '音频', hint: 'MP3 / WAV / M4A' },
  { key: 'archive', label: '压缩包', hint: 'ZIP' },
];

export const FILEEASY_PROTOTYPE_INITIAL_TASKS: FileEasyPrototypeUploadTask[] = [
  { id: 'u1', name: '项目方案.pdf', size: '48 MB', progress: 24, status: 'uploading' },
  { id: 'u2', name: '合同归档.zip', size: '1.2 GB', progress: 0, status: 'queued' },
  {
    id: 'u3',
    name: '宣传视频.mp4',
    size: '860 MB',
    progress: 68,
    status: 'restoring',
    note: '正在恢复上传任务',
  },
];

export const FILEEASY_PROTOTYPE_UPLOAD_FEATURE_BADGES: string[] = [
  '单文件 4GB',
  '多文件上传',
  '断点续传',
];

export const FILEEASY_PROTOTYPE_FOLDER_GLYPHS: Record<FolderKey, string> = {
  all: '全',
  document: '文',
  video: '视',
  image: '图',
  audio: '音',
  archive: '压',
};

export const FILEEASY_PROTOTYPE_FILE_KIND_GLYPHS: Record<
  FileEasyPrototypeDemoFileKind,
  string
> = {
  PDF: 'PDF',
  视频: 'VID',
  图片: 'IMG',
  音频: 'AUD',
  ZIP: 'ZIP',
};
