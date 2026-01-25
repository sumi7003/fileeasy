import { Controller, Post, Get, Body, UseInterceptors, UploadedFile, BadRequestException, Res } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { UpdateService } from './update.service';
import { diskStorage } from 'multer';
import { extname, join } from 'path';

@Controller('update')
export class UpdateController {
  constructor(private readonly updateService: UpdateService) {}

  @Post('upload')
  @UseInterceptors(FileInterceptor('file', {
    storage: diskStorage({
      destination: './uploads/apk',
      filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, `${file.fieldname}-${uniqueSuffix}${extname(file.originalname)}`);
      },
    }),
    limits: {
      fileSize: 100 * 1024 * 1024, // 100MB limit
    }
  }))
  async uploadApk(
    @UploadedFile() file: Express.Multer.File,
    @Body('versionCode') versionCode?: string,
    @Body('versionName') versionName?: string,
  ) {
    if (!file) {
      throw new BadRequestException('File is missing');
    }

    const latest = await this.updateService.getLatestUpdate();
    
    // 如果没有提供版本号，则自动增加。
    // 起始值设大一点（比如 1000），避免低于用户现有的版本。
    const nextVersionCode = versionCode ? parseInt(versionCode) : Math.max(latest?.versionCode || 0, 1000) + 1;
    const nextVersionName = versionName || `1.0.${nextVersionCode}`;

    return this.updateService.createUpdate(file, nextVersionCode, nextVersionName);
  }

  @Get('check')
  async checkUpdate() {
    const latest = await this.updateService.getLatestUpdate();
    if (!latest) {
      return { hasUpdate: false, versionCode: 0, versionName: '0.0.0' };
    }
    return {
      hasUpdate: true,
      versionCode: latest.versionCode,
      versionName: latest.versionName,
    };
  }

  @Get('download')
  async download(@Res() res: any) {
    const latest = await this.updateService.getLatestUpdate();
    if (!latest) {
      throw new BadRequestException('No update available');
    }
    // Static path is /uploads/apk/filename
    return res.redirect(`/uploads/apk/${latest.filename}`);
  }
}
