import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { DeviceController } from './device.controller';
import { DeviceService } from './device.service';
import { Device } from './device.entity';
import { Playlist } from '../playlist/playlist.entity'; // Add this
import { PlaylistModule } from '../playlist/playlist.module'; // Import PlaylistModule
import { UpdateModule } from '../update/update.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([Device, Playlist]), // Add Playlist here
    PlaylistModule,
    UpdateModule,
  ],
  controllers: [DeviceController],
  providers: [DeviceService],
  exports: [DeviceService],
})
export class DeviceModule {}
