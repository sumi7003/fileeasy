import { Controller, Get, Post, Put, Body, Param, Delete } from '@nestjs/common';
import { PlaylistService, CreatePlaylistDto, UpdatePlaylistDto } from './playlist.service';
import { PlaylistGateway } from './playlist.gateway';

@Controller('playlists')
export class PlaylistController {
  constructor(
    private readonly playlistService: PlaylistService,
    private readonly playlistGateway: PlaylistGateway,
  ) {}

  @Get()
  findAll() {
    return this.playlistService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.playlistService.findOne(id);
  }

  @Post()
  async create(@Body() dto: CreatePlaylistDto) {
    const playlist = await this.playlistService.create(dto);
    // 通知所有客户端播放列表已创建
    this.playlistGateway.notifyPlaylistChange(playlist.id, 'created', playlist);
    return playlist;
  }

  @Put(':id')
  async update(@Param('id') id: string, @Body() dto: UpdatePlaylistDto) {
    const playlist = await this.playlistService.update(id, dto);
    // 通知所有客户端播放列表已更新
    this.playlistGateway.notifyPlaylistChange(id, 'updated', playlist);
    return playlist;
  }

  @Delete(':id')
  async delete(@Param('id') id: string) {
    await this.playlistService.delete(id);
    // 通知所有客户端播放列表已删除
    this.playlistGateway.notifyPlaylistChange(id, 'deleted');
    return { success: true };
  }
}

