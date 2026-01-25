import { Entity, Column, PrimaryGeneratedColumn, CreateDateColumn, UpdateDateColumn, ManyToMany, JoinTable } from 'typeorm';
import { Playlist } from '../playlist/playlist.entity';

export enum DeviceStatus {
  ONLINE = 'online',
  OFFLINE = 'offline',
  PENDING = 'pending',
}

@Entity('devices')
export class Device {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ unique: true })
  serialNumber: string;

  @Column({ nullable: true })
  name: string;

  @Column({ nullable: true })
  ipAddress: string;

  @Column({ nullable: true })
  version: string;

  @Column({
    type: 'varchar',
    default: DeviceStatus.PENDING,
  })
  status: DeviceStatus;

  @ManyToMany(() => Playlist)
  @JoinTable({
    name: 'device_playlists',
    joinColumn: { name: 'device_id', referencedColumnName: 'id' },
    inverseJoinColumn: { name: 'playlist_id', referencedColumnName: 'id' }
  })
  playlists: Playlist[];

  @Column({ type: 'datetime', nullable: true }) // Changed from timestamp to datetime for SQLite
  lastHeartbeat: Date;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
