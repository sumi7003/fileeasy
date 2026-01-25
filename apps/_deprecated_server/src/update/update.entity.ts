import { Entity, Column, PrimaryGeneratedColumn, CreateDateColumn } from 'typeorm';

@Entity('app_updates')
export class AppUpdate {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column()
  versionCode: number;

  @Column()
  versionName: string;

  @Column()
  filename: string;

  @Column()
  path: string;

  @CreateDateColumn()
  createdAt: Date;
}
