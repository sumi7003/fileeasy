import type { UploadTask } from '../types/upload';

export const useUploadTasks = () => {
  return {
    tasks: [] as UploadTask[],
  };
};
