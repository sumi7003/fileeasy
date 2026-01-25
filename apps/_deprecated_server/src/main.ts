/**
 * ⚠️ ⚠️ ⚠️ 此文件已废弃 - DEPRECATED ⚠️ ⚠️ ⚠️
 * 
 * 独立NestJS服务器模式已不再使用
 * 
 * 当前架构：Android Host Mode (一主多从)
 * - 主Pad运行内嵌Ktor服务器 (LocalServerService.kt)
 * - 从Pad连接到主Pad进行播放
 * 
 * 废弃原因：
 * 1. 用户已有多台高性能Android Pad，不需要额外服务器
 * 2. Android Host Mode架构更简单、成本为零
 * 3. 维护双服务端架构成本高、容易出问题
 * 
 * 如需重新启用此模式，请查看文档：
 * docs/DEPRECATED_NESTJS_SERVER.md
 * 
 * 最后更新：2026-01-16
 * 维护者：开发团队
 */

/*
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { Logger } from '@nestjs/common';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  
  // Enable CORS for Web Admin and Android (if needed for API)
  app.enableCors({
    origin: '*', // Allow all origins for dev/testing
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE',
  });
  
  // Global Prefix
  app.setGlobalPrefix('api/v1');

  const port = 3000;
  // Listen on all interfaces (0.0.0.0) so external devices/emulator can access
  await app.listen(port, '0.0.0.0');
  Logger.log(`Server running on http://localhost:${port}`, 'Bootstrap');
}
bootstrap();
*/

// ⚠️ 此服务器代码已注释，不会执行
console.error('❌ NestJS服务器已废弃，请使用Android Host Mode');
console.error('📖 详见文档: docs/DEPRECATED_NESTJS_SERVER.md');
process.exit(1);
