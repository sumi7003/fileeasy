import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { ServeStaticModule } from '@nestjs/serve-static';
import { join } from 'path';
import { DeviceModule } from './device/device.module';
import { MediaModule } from './media/media.module';
import { PlaylistModule } from './playlist/playlist.module';
import { UpdateModule } from './update/update.module';
import { Device } from './device/device.entity';
import { Media } from './media/media.entity';
import { Playlist, PlaylistItem } from './playlist/playlist.entity';
import { AppUpdate } from './update/update.entity';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    ServeStaticModule.forRoot({
      rootPath: join(__dirname, '..', '..', 'uploads'),
      serveRoot: '/uploads',
    }),
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (configService: ConfigService) => {
        // TEMPORARY: Force SQLite for local testing without Docker
        // Change this back to 'postgres' logic for production
        return {
          type: 'sqlite',
          database: 'xplay.db',
          entities: [Device, Media, Playlist, PlaylistItem, AppUpdate],
          synchronize: true, // Auto create tables
        };
      },
      inject: [ConfigService],
    }),
    DeviceModule,
    MediaModule,
    PlaylistModule,
    UpdateModule,
  ],
})
export class AppModule {}
