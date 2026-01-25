import { Injectable, BadRequestException, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Media, MediaType } from './media.entity';
import * as fs from 'fs';

@Injectable()
export class MediaService {
  private readonly logger = new Logger(MediaService.name);

  constructor(
    @InjectRepository(Media)
    private mediaRepository: Repository<Media>,
  ) {
    // Ensure upload directory exists
    if (!fs.existsSync('./uploads')) {
      fs.mkdirSync('./uploads');
    }
  }

  async uploadFile(file: Express.Multer.File): Promise<Media> {
    if (!file) {
      throw new BadRequestException('File is required');
    }

    const type = file.mimetype.startsWith('video') ? MediaType.VIDEO : MediaType.IMAGE;
    
    // In production, build a full URL based on configured domain
    // For now, we assume static serving from /uploads
    const url = `/uploads/${file.filename}`;

    const media = this.mediaRepository.create({
      originalName: file.originalname,
      filename: file.filename,
      path: file.path,
      mimetype: file.mimetype,
      size: file.size,
      type: type,
      url: url,
    });

    return this.mediaRepository.save(media);
  }

  async findAll(): Promise<Media[]> {
    return this.mediaRepository.find({
      order: { createdAt: 'DESC' },
    });
  }

  async remove(id: string): Promise<void> {
    const media = await this.mediaRepository.findOneBy({ id });
    if (!media) {
      return;
    }

    // 1. 删除物理文件
    if (media.path && fs.existsSync(media.path)) {
      try {
        fs.unlinkSync(media.path);
      } catch (err) {
        this.logger.error(`Failed to delete physical file: ${media.path}`, (err as Error).stack);
        // 继续执行，防止因为文件系统问题导致数据库记录无法删除
      }
    }

    // 2. 删除数据库记录
    await this.mediaRepository.remove(media);
  }
}

