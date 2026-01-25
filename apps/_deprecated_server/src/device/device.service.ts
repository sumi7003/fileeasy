import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In } from 'typeorm';
import { Device, DeviceStatus } from './device.entity';
import { PlaylistService } from '../playlist/playlist.service';
import { Playlist } from '../playlist/playlist.entity';
import { UpdateService } from '../update/update.service';

export interface RegisterDeviceDto {
  serialNumber: string;
  name?: string;
  version?: string;
  ipAddress?: string;
}

export interface HeartbeatResponse {
  status: string;
  timestamp: Date;
  playlistIds: string[];
  currentPlaylistId: string | null; // 为了兼容旧版 APK
  updateInfo?: {
    versionCode: number;
    versionName: string;
    hasUpdate: boolean;
  };
}

@Injectable()
export class DeviceService {
  private readonly logger = new Logger(DeviceService.name);

  constructor(
    @InjectRepository(Device)
    private deviceRepository: Repository<Device>,
    @InjectRepository(Playlist)
    private playlistRepository: Repository<Playlist>,
    private updateService: UpdateService,
  ) {}

  async findAll(): Promise<Device[]> {
    return this.deviceRepository.find({
      order: { lastHeartbeat: 'DESC' },
      relations: ['playlists'], // Include playlists info
    });
  }

  async findOne(id: string): Promise<Device | null> {
    return this.deviceRepository.findOne({ 
      where: { id },
      relations: ['playlists'] 
    });
  }

  async register(dto: RegisterDeviceDto): Promise<Device> {
    this.logger.log(`Registering device: ${dto.serialNumber}`);
    
    let device = await this.deviceRepository.findOne({ 
      where: { serialNumber: dto.serialNumber },
      relations: ['playlists']
    });

    if (!device) {
      // 查找第一个可用的清单
      const firstPlaylist = await this.playlistRepository.findOne({ where: {} });
      
      device = this.deviceRepository.create({
        serialNumber: dto.serialNumber,
        name: dto.name || `Device-${dto.serialNumber.substring(0, 6)}`,
        status: DeviceStatus.ONLINE,
        playlists: firstPlaylist ? [firstPlaylist] : [], // 自动关联第一个清单
      });
    }

    // Update dynamic info
    if (dto.version) device.version = dto.version;
    if (dto.ipAddress) device.ipAddress = dto.ipAddress;
    
    device.lastHeartbeat = new Date();
    device.status = DeviceStatus.ONLINE;

    return this.deviceRepository.save(device);
  }

  async heartbeat(id: string): Promise<HeartbeatResponse> {
    const device = await this.deviceRepository.findOne({ 
      where: { id },
      relations: ['playlists']
    });
    
    if (!device) {
      throw new NotFoundException(`Device ${id} not found`);
    }

    // ✅ 强力修复：如果设备没有任何清单，直接分配所有已存在的清单
    if (!device.playlists || device.playlists.length === 0) {
      const allPlaylists = await this.playlistRepository.find();
      if (allPlaylists.length > 0) {
        device.playlists = allPlaylists;
        await this.deviceRepository.save(device);
        this.logger.log(`Force-assigned ${allPlaylists.length} playlists to device: ${device.name}`);
      }
    }

    await this.deviceRepository.update(id, {
      lastHeartbeat: new Date(),
      status: DeviceStatus.ONLINE,
    });

    const now = new Date();
    const currentTime = now.toTimeString().split(' ')[0]; // HH:mm:ss
    const currentDay = now.getDay() === 0 ? 7 : now.getDay(); // 1-7 (Mon-Sun)

    const activePlaylists = device.playlists.filter(p => {
      // If no schedule set, it's always active
      if (!p.startTime || !p.endTime) return true;

      // Check days of week
      const days = p.daysOfWeek ? p.daysOfWeek.split(',').map(Number) : [1, 2, 3, 4, 5, 6, 7];
      if (!days.includes(currentDay)) {
        this.logger.debug(`Playlist ${p.name} filtered out by day: today=${currentDay}, allowed=${days}`);
        return false;
      }

      // Check time range
      const isActive = currentTime >= p.startTime && currentTime <= p.endTime;
      if (!isActive) {
        this.logger.debug(`Playlist ${p.name} filtered out by time: current=${currentTime}, range=${p.startTime}-${p.endTime}`);
      }
      return isActive;
    });

    const playlistIds = activePlaylists.map(p => p.id);
    this.logger.log(`Heartbeat for device ${device.name} (${id}). Returning playlists: ${playlistIds}`);

    const latestUpdate = await this.updateService.getLatestUpdate();
    const updateInfo = latestUpdate ? {
      versionCode: latestUpdate.versionCode,
      versionName: latestUpdate.versionName,
      hasUpdate: true,
    } : undefined;

    return {
      status: 'ok',
      timestamp: new Date(),
      playlistIds: playlistIds,
      currentPlaylistId: playlistIds.length > 0 ? playlistIds[0] : null,
      updateInfo,
    };
  }

  async assignPlaylists(deviceId: string, playlistIds: string[]): Promise<Device> {
    const device = await this.deviceRepository.findOne({ 
      where: { id: deviceId },
      relations: ['playlists']
    });
    
    if (!device) {
      throw new NotFoundException(`Device ${deviceId} not found`);
    }

    // Find all playlists
    const playlists = await this.playlistRepository.findBy({
      id: In(playlistIds)
    });
    
    device.playlists = playlists;
    return this.deviceRepository.save(device);
  }
}
