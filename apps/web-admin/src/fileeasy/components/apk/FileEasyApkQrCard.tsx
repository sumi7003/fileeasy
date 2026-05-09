import React from 'react';
import Button from '../shared/Button';
import type { ApkMode, HomeUploadTask, ServiceStage } from '../../types/apk';

type FileEasyApkQrCardProps = {
  activeUploads: HomeUploadTask[];
  apkMode: ApkMode;
  isHomeSummaryLoading: boolean;
  serviceStage: ServiceStage;
  uploadUrl?: string;
  onOpenNetworkDialog: () => void;
  onOpenUploadEntry: () => void;
  onRecoverNetwork: () => void;
  onSetReady: () => void;
  onStepForward: () => void;
};

const uploadStatusLabelMap: Record<string, string> = {
  initialized: '等待上传',
  ready: '等待确认',
  uploading: '上传中',
  paused: '已暂停',
  resuming: '继续上传',
  completed: '已完成',
};

const getUploadStatusLabel = (status: string) => uploadStatusLabelMap[status] || '上传中';

const FileEasyApkQrCard: React.FC<FileEasyApkQrCardProps> = ({
  activeUploads,
  apkMode,
  isHomeSummaryLoading,
  serviceStage,
  uploadUrl,
  onOpenNetworkDialog,
  onOpenUploadEntry,
  onRecoverNetwork,
  onSetReady,
  onStepForward,
}) => {
  if (apkMode === 'network-missing') {
    return (
      <div className="fileeasy-qr-card fileeasy-qr-card--warning">
        <div className="fileeasy-qr-card__label">扫码上传</div>
        <div className="fileeasy-empty-hero">当前无法生成上传二维码</div>
        <div className="fileeasy-muted">请确认设备已连接 Wi-Fi 或热点后再展示上传地址</div>
        <div className="fileeasy-inline-actions">
          <Button variant="ghost" onClick={onOpenNetworkDialog}>
            查看处理建议
          </Button>
          <Button variant="primary" onClick={onRecoverNetwork}>
            模拟网络恢复
          </Button>
        </div>
      </div>
    );
  }

  if (serviceStage !== 'ready') {
    return (
      <div className="fileeasy-qr-card fileeasy-qr-card--pending">
        <div className="fileeasy-qr-card__label">扫码上传</div>
        <div className="fileeasy-empty-hero">
          {serviceStage === 'booting' ? '正在启动本地服务' : '前台服务已运行，等待生成可访问地址'}
        </div>
        <div className="fileeasy-muted">
          {serviceStage === 'booting'
            ? '请稍候，首页资源和服务状态正在初始化'
            : '检测到可用局域网地址后，将自动展示二维码和上传地址'}
        </div>
        <div className="fileeasy-inline-actions">
          <Button variant="ghost" onClick={onStepForward}>
            推进到下一状态
          </Button>
          <Button variant="primary" onClick={onSetReady}>
            直接进入可访问态
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="fileeasy-qr-card">
      <div className="fileeasy-qr-card__layout">
        <div className="fileeasy-qr-card__main">
          <div className="fileeasy-qr-card__label">扫码上传</div>
          <button className="fileeasy-qr" onClick={onOpenUploadEntry} type="button">
            <span>QR</span>
          </button>
        </div>
        <div className="fileeasy-qr-card__side">
          <div className="fileeasy-qr-card__label">上传页地址</div>
          <div className="fileeasy-address">{uploadUrl || 'http://192.168.1.23:3000/'}</div>
          <div className="fileeasy-mini-note">扫码后直接选择文件开始上传。</div>
          <div className="fileeasy-upload-flow-hint">
            <span>1 扫码</span>
            <span>2 选择文件</span>
            <span>3 开始上传</span>
          </div>
        </div>
      </div>

      <div className="fileeasy-home-upload-state">
          <div className="fileeasy-home-upload-state__header">
            <strong>上传页状态</strong>
          <span>{activeUploads.length > 0 ? `${activeUploads.length} 个文件正在上传` : '等待开始上传'}</span>
        </div>
        {activeUploads.length > 0 ? (
          <div className="fileeasy-home-upload-state__list">
            {activeUploads.map((task) => (
              <div className="fileeasy-home-upload-task" key={task.uploadId}>
                <div className="fileeasy-home-upload-task__top">
                  <strong>{task.fileName}</strong>
                  <span>{getUploadStatusLabel(task.status)}</span>
                </div>
                <div className="fileeasy-home-upload-task__bar">
                  <div className="fileeasy-home-upload-task__fill" style={{ width: `${task.progress}%` }} />
                </div>
                <div className="fileeasy-home-upload-task__meta">
                  <span>{task.progress}%</span>
                  <span>
                    {task.uploadedChunks}/{task.totalChunks} 个分片
                  </span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="fileeasy-home-upload-state__empty">
            {isHomeSummaryLoading ? '正在同步上传状态...' : '访客开始上传后，这里会显示文件名、进度和当前状态'}
          </div>
        )}
      </div>
    </div>
  );
};

export default FileEasyApkQrCard;
