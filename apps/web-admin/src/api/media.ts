import request from './request';

export interface Media {
  id: string;
  originalName: string;
  filename: string;
  url: string;
  type: 'image' | 'video';
  size: number;
  createdAt: string;
}

export const getMedia = () => {
  return request.get<any, Media[]>('/media');
};

export const uploadMedia = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post<any, Media>('/media/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const deleteMedia = (id: string) => {
  return request.delete(`/media/${id}`);
};
