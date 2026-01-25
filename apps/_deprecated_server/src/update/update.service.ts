import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { AppUpdate } from './update.entity';

@Injectable()
export class UpdateService {
  private readonly logger = new Logger(UpdateService.name);

  constructor(
    @InjectRepository(AppUpdate)
    private updateRepository: Repository<AppUpdate>,
  ) {}

  async getLatestUpdate(): Promise<AppUpdate | null> {
    return this.updateRepository.findOne({
      where: {},
      order: { createdAt: 'DESC' },
    });
  }

  async createUpdate(file: Express.Multer.File, versionCode: number, versionName: string): Promise<AppUpdate> {
    const update = this.updateRepository.create({
      versionCode,
      versionName,
      filename: file.filename,
      path: file.path,
    });
    return this.updateRepository.save(update);
  }

  // Alias for backward compatibility if needed, or just use getLatestUpdate
  async findLatest(): Promise<AppUpdate | null> {
    return this.getLatestUpdate();
  }
}
