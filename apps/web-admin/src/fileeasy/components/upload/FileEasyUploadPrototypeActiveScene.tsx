import React from 'react';
import Button from '../shared/Button';
import type {
  FileEasyPrototypeStatusCopy,
  FileEasyPrototypeStatusToneMap,
  FileEasyPrototypeUploadTask,
} from '../../types/prototype';
import FileEasyUploadPrototypeTaskCard from './FileEasyUploadPrototypeTaskCard';

type FileEasyUploadPrototypeActiveSceneProps = {
  featureBadges: string[];
  statusCopy: FileEasyPrototypeStatusCopy;
  statusToneMap: FileEasyPrototypeStatusToneMap;
  tasks: FileEasyPrototypeUploadTask[];
  onJumpToAdmin: () => void;
  onJumpToAlerts: () => void;
  onJumpToApkHome: () => void;
  onSimulateUpload: () => void;
  onToggleTaskStatus: (taskId: string) => void;
};

const FileEasyUploadPrototypeActiveScene: React.FC<FileEasyUploadPrototypeActiveSceneProps> = ({
  featureBadges,
  statusCopy,
  statusToneMap,
  tasks,
  onJumpToAdmin,
  onJumpToAlerts,
  onJumpToApkHome,
  onSimulateUpload,
  onToggleTaskStatus,
}) => {
  return (
    <div className="fileeasy-device fileeasy-device--mobile">
      <div className="fileeasy-screen fileeasy-screen--web">
        <div className="fileeasy-web-header fileeasy-web-header--hero">
          <div>
            <div className="fileeasy-page-title">FileEasy 文件上传</div>
            <div className="fileeasy-subtitle">已连接到当前设备</div>
          </div>
          <Button variant="secondary" onClick={onJumpToApkHome}>
            返回设备页
          </Button>
        </div>

        <div className="fileeasy-chip-row">
          {featureBadges.map((badge) => (
            <span className="fileeasy-feature-chip" key={badge}>
              {badge}
            </span>
          ))}
        </div>

        <div className="fileeasy-upload-split">
          <div className="fileeasy-upload-card fileeasy-upload-card--prominent">
            <div className="fileeasy-card-heading">
              <strong>上传文件</strong>
              <span>把文件发送到当前设备</span>
            </div>
            <Button variant="primary" onClick={onSimulateUpload}>
              选择文件
            </Button>
            <div className="fileeasy-capability-list">
              <span>支持单文件 / 多文件上传</span>
              <span>单文件最大 4GB</span>
              <span>支持断点续传</span>
            </div>
            <div className="fileeasy-inline-actions">
              <Button variant="ghost" onClick={onJumpToAlerts}>
                查看异常态
              </Button>
              <Button variant="ghost" onClick={onJumpToAdmin}>
                去管理页
              </Button>
            </div>
          </div>

          <div className="fileeasy-upload-summary">
            <div className="fileeasy-card-heading">
              <strong>当前上传概览</strong>
              <span>{tasks.length} 个任务</span>
            </div>
            <div className="fileeasy-mini-grid">
              <div>
                <span>进行中</span>
                <strong>{tasks.filter((task) => task.status === 'uploading').length}</strong>
              </div>
              <div>
                <span>恢复中</span>
                <strong>{tasks.filter((task) => task.status === 'restoring').length}</strong>
              </div>
              <div>
                <span>待处理</span>
                <strong>{tasks.filter((task) => task.status === 'queued').length}</strong>
              </div>
              <div>
                <span>已完成</span>
                <strong>{tasks.filter((task) => task.status === 'done').length}</strong>
              </div>
            </div>
          </div>
        </div>

        <div className="fileeasy-section-caption">点击任务卡可轮播状态：上传中 → 失败 → 恢复 → 完成</div>

        <div className="fileeasy-panel">
          <div className="fileeasy-card-heading">
            <strong>上传任务</strong>
            <span>保持列表结构稳定，方便长时间观察</span>
          </div>
          <div className="fileeasy-stack">
            {tasks.map((task) => (
              <FileEasyUploadPrototypeTaskCard
                key={task.id}
                statusCopy={statusCopy}
                statusToneMap={statusToneMap}
                task={task}
                onClick={onToggleTaskStatus}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default FileEasyUploadPrototypeActiveScene;
