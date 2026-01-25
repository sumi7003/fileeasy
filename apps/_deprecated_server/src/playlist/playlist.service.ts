import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In } from 'typeorm';
import { Playlist, PlaylistItem } from './playlist.entity';
import { Media } from '../media/media.entity';

export interface CreatePlaylistDto {
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

export interface UpdatePlaylistDto {
  name?: string;
  description?: string;
  startTime?: string;
  endTime?: string;
  daysOfWeek?: string;
  items?: {
    mediaId: string;
    duration: number;
    order: number;
  }[];
}

@Injectable()
export class PlaylistService {
  constructor(
    @InjectRepository(Playlist)
    private playlistRepository: Repository<Playlist>,
    @InjectRepository(PlaylistItem)
    private itemRepository: Repository<PlaylistItem>,
    @InjectRepository(Media)
    private mediaRepository: Repository<Media>,
  ) {}

  async findAll(): Promise<Playlist[]> {
    return this.playlistRepository.find({
      relations: ['items', 'items.media'],
      order: { createdAt: 'DESC' },
    });
  }

  async findOne(id: string): Promise<Playlist> {
    const playlist = await this.playlistRepository.findOne({
      where: { id },
      relations: ['items', 'items.media'],
      order: { items: { order: 'ASC' } } as any, // Sort items by order
    });

    if (!playlist) {
      throw new NotFoundException(`Playlist ${id} not found`);
    }
    return playlist;
  }

  async create(dto: CreatePlaylistDto): Promise<Playlist> {
    const playlist = this.playlistRepository.create({
      name: dto.name,
      description: dto.description,
      startTime: dto.startTime,
      endTime: dto.endTime,
      daysOfWeek: dto.daysOfWeek,
    });

    const savedPlaylist = await this.playlistRepository.save(playlist);

    // Create items
    if (dto.items && dto.items.length > 0) {
      const items: PlaylistItem[] = [];
      for (const itemDto of dto.items) {
        const media = await this.mediaRepository.findOneBy({ id: itemDto.mediaId });
        if (media) {
          const item = this.itemRepository.create({
            order: itemDto.order,
            duration: itemDto.duration,
            playlist: savedPlaylist,
            media: media,
          });
          items.push(item);
        }
      }
      await this.itemRepository.save(items);
    }

    return this.findOne(savedPlaylist.id);
  }

  async update(id: string, dto: UpdatePlaylistDto): Promise<Playlist> {
    const playlist = await this.playlistRepository.findOne({
      where: { id },
      relations: ['items'],
    });

    if (!playlist) {
      throw new NotFoundException(`Playlist ${id} not found`);
    }

    // Update basic fields
    if (dto.name !== undefined) playlist.name = dto.name;
    if (dto.description !== undefined) playlist.description = dto.description;
    if (dto.startTime !== undefined) playlist.startTime = dto.startTime;
    if (dto.endTime !== undefined) playlist.endTime = dto.endTime;
    if (dto.daysOfWeek !== undefined) playlist.daysOfWeek = dto.daysOfWeek;

    await this.playlistRepository.save(playlist);

    // Update items if provided
    if (dto.items !== undefined) {
      // Delete existing items
      await this.itemRepository.delete({ playlist: { id } });

      // Create new items
      if (dto.items.length > 0) {
        const items: PlaylistItem[] = [];
        for (const itemDto of dto.items) {
          const media = await this.mediaRepository.findOneBy({ id: itemDto.mediaId });
          if (media) {
            const item = this.itemRepository.create({
              order: itemDto.order,
              duration: itemDto.duration,
              playlist: playlist,
              media: media,
            });
            items.push(item);
          }
        }
        await this.itemRepository.save(items);
      }
    }

    return this.findOne(id);
  }

  async delete(id: string): Promise<void> {
    await this.playlistRepository.delete(id);
  }
}

