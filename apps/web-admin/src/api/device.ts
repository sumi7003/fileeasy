import request from './request';

import { Playlist } from './playlist';

export interface Device {
  id: string;
  serialNumber: string;
  name: string;
  status: 'online' | 'offline' | 'pending';
  lastHeartbeat: string;
  ipAddress?: string;
  version?: string;
  createdAt: string;
  playlists?: Playlist[];
}

export const getDevices = () => {
  return request.get<any, Device[]>('/devices');
};

