import type { UploadInitPayload, UploadInitResponse } from '../types/upload';

export const fileEasyUploadService = {
  initUpload: async (_payload: UploadInitPayload): Promise<UploadInitResponse> => {
    return { uploadId: '' };
  },
};
