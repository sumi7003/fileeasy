import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PlaylistController } from './playlist.controller';
import { PlaylistService } from './playlist.service';
import { PlaylistGateway } from './playlist.gateway';
import { Playlist, PlaylistItem } from './playlist.entity';
import { MediaModule } from '../media/media.module';
import { Media } from '../media/media.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([Playlist, PlaylistItem, Media]),
    MediaModule,
  ],
  controllers: [PlaylistController],
  providers: [PlaylistService, PlaylistGateway],
  exports: [PlaylistService, PlaylistGateway],
})
export class PlaylistModule {}

