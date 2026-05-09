import type { FileItem, FolderKey } from '../types/file';

export const useFolderFilter = () => {
  return {
    folder: 'all' as FolderKey,
    files: [] as FileItem[],
  };
};
