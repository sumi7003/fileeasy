import { Entity, Column, PrimaryGeneratedColumn, CreateDateColumn, UpdateDateColumn, OneToMany, ManyToOne } from 'typeorm';
import { Media } from '../media/media.entity';

@Entity('playlists')
export class Playlist {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  name: string;

  @Column({ nullable: true })
  description: string;

  @Column({ nullable: true })
  startTime: string; // HH:mm:ss

  @Column({ nullable: true })
  endTime: string; // HH:mm:ss

  @Column({ nullable: true, default: '1,2,3,4,5,6,7' })
  daysOfWeek: string; // 1,2,3,4,5,6,7

  @OneToMany(() => PlaylistItem, item => item.playlist, { cascade: true })
  items: PlaylistItem[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}

@Entity('playlist_items')
export class PlaylistItem {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  order: number;

  @Column({ default: 10 }) // duration in seconds
  duration: number;

  @ManyToOne(() => Playlist, playlist => playlist.items, { onDelete: 'CASCADE' })
  playlist: Playlist;

  @ManyToOne(() => Media, { eager: true, onDelete: 'CASCADE' }) // 删除媒体时自动移除播放列表项
  media: Media;
}

