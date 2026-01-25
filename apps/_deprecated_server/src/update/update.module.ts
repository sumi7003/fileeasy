import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UpdateController } from './update.controller';
import { UpdateService } from './update.service';
import { AppUpdate } from './update.entity';

@Module({
  imports: [TypeOrmModule.forFeature([AppUpdate])],
  controllers: [UpdateController],
  providers: [UpdateService],
  exports: [UpdateService],
})
export class UpdateModule {}
