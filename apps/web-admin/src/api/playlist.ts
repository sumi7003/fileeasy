import request from './request';
import { Media } from './media';

export interface PlaylistItem {
  id: string;
  order: number;
  duration: number;
  media: Media;
}

export interface Playlist {
  id: string;
  name: string;
  description: string;
  startTime?: string;
  endTime?: string;
  daysOfWeek?: string;
  items: PlaylistItem[];
  createdAt: string;
}

export interface PlaylistFormData {
  name: string;
  description?: string;
  startTime?: string;
  endTime?: string;
  daysOfWeek?: string;
  items: {
    mediaId: string;
    duration: number;
    order: number;
  }[];
}

export const getPlaylists = () => {
  return request.get<any, Playlist[]>('/playlists');
};

export const getPlaylist = (id: string) => {
  return request.get<any, Playlist>(`/playlists/${id}`);
};

export const createPlaylist = (data: PlaylistFormData) => {
  return request.post<any, Playlist>('/playlists', data);
};

export const updatePlaylist = (id: string, data: Partial<PlaylistFormData>) => {
  return request.put<any, Playlist>(`/playlists/${id}`, data);
};

export const deletePlaylist = (id: string) => {
  return request.delete(`/playlists/${id}`);
};

export const assignPlaylists = (deviceId: string, playlistIds: string[]) => {
  return request.patch(`/devices/${deviceId}/playlist`, { playlistIds });
};
