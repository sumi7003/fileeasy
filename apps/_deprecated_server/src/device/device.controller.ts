import { Controller, Get, Post, Body, Param, Put, Patch } from '@nestjs/common';
import { DeviceService, RegisterDeviceDto } from './device.service';

@Controller('devices')
export class DeviceController {
  constructor(private readonly deviceService: DeviceService) {}

  @Get()
  findAll() {
    return this.deviceService.findAll();
  }

  @Post('register')
  register(@Body() dto: RegisterDeviceDto) {
    return this.deviceService.register(dto);
  }

  @Put(':id/heartbeat')
  heartbeat(@Param('id') id: string) {
    return this.deviceService.heartbeat(id);
  }

  @Patch(':id/playlist')
  assignPlaylist(
    @Param('id') id: string,
    @Body('playlistIds') playlistIds: string[],
  ) {
    return this.deviceService.assignPlaylists(id, playlistIds);
  }
}
