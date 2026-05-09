export type FileKind = 'PDF' | '视频' | '图片' | '音频' | 'ZIP' | '文档';

export type FolderKey = 'all' | 'document' | 'video' | 'image' | 'audio' | 'archive';

export type FileItem = {
  id: string;
  name: string;
  baseName: string;
  extension: string;
  kind: FileKind;
  folder: FolderKey;
  size: string;
  sizeBytes: number;
  time: string;
  previewable: boolean;
  previewUrl: string;
  downloadUrl: string;
  createdAt: string;
  source: 'files-api' | 'media-api';
};
