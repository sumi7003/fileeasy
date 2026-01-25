import request from './request';

export interface UpdateInfo {
  versionCode: number;
  versionName: string;
  hasUpdate: boolean;
}

export const uploadApk = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post<any, any>('/update/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const getUpdateInfo = () => {
  return request.get<any, UpdateInfo>('/update/check');
};
