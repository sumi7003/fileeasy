import React from 'react';
import type { ApkMode, HomeSummary, ServiceStage, ServiceStep } from '../../types/apk';
import Button from '../shared/Button';
import StatusPill from '../shared/StatusPill';
import FileEasyApkQrCard from './FileEasyApkQrCard';
import YiTransferBrand from '../shared/YiTransferBrand';

type FileEasyApkHomeSceneProps = {
  apkMode: ApkMode;
  apkPassword: string;
  homeSummary: HomeSummary | null;
  isHomeSummaryLoading: boolean;
  serviceStage: ServiceStage;
  serviceSteps: ServiceStep[];
  onCompleteOnboarding: () => void;
  onCycleServiceStage: () => void;
  onJumpToAdmin: () => void;
  onJumpToUpload: () => void;
  onOpenInfoDialog: () => void;
  onOpenNetworkDialog: () => void;
  onOpenPasswordDialog: () => void;
  onSelectMode: (mode: ApkMode) => void;
  onSelectServiceStage: (stage: ServiceStage) => void;
  onSetReady: () => void;
};

const formatBytes = (bytes: number) => {
  if (!Number.isFinite(bytes)) return '-';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let value = bytes;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value.toFixed(value >= 100 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
};

const categoryLabelMap: Record<string, string> = {
  archive: '压缩包',
  audio: '音频',
  document: '文档',
  image: '图片',
  video: '视频',
};

const formatRelativeTime = (timestamp: number) => {
  if (!timestamp) return '刚刚';
  const delta = Date.now() - timestamp;
  if (delta < 60_000) return '刚刚';
  const minutes = Math.floor(delta / 60_000);
  if (minutes < 60) return `${minutes} 分钟前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} 小时前`;
  const days = Math.floor(hours / 24);
  return `${days} 天前`;
};

const transferIllustration = new URL('../../assets/yichuanshu-home-transfer.png', import.meta.url).href;

const FileEasyApkHomeScene: React.FC<FileEasyApkHomeSceneProps> = ({
  apkMode,
  apkPassword,
  homeSummary,
  isHomeSummaryLoading,
  serviceStage,
  serviceSteps,
  onCompleteOnboarding,
  onCycleServiceStage,
  onJumpToAdmin,
  onJumpToUpload,
  onOpenInfoDialog,
  onOpenNetworkDialog,
  onOpenPasswordDialog,
  onSelectMode,
  onSetReady,
}) => {
  const apkStatusLabel =
    apkMode === 'network-missing'
      ? '等待网络'
      : apkMode === 'first-install'
        ? '首次安装'
        : serviceStage === 'booting'
          ? '服务启动中'
          : serviceStage === 'foreground'
            ? '服务运行中'
            : '可扫码上传';
  const showIllustration = serviceStage === 'ready' && !homeSummary?.activeUploads.length && !homeSummary?.recentFiles.length;

  return (
    <div className="fileeasy-device fileeasy-device--android">
      <div className="fileeasy-screen">
        <div className="fileeasy-screen__header fileeasy-screen__header--hero">
          <YiTransferBrand caption="扫码后把文件上传到这台设备" className="fileeasy-brand-lockup" />
          <StatusPill
            className={`fileeasy-status-pill ${apkMode === 'network-missing' ? 'warning' : apkMode === 'first-install' ? 'setup' : ''}`}
            tone={
              apkMode === 'network-missing'
                ? 'warning'
                : apkMode === 'first-install'
                  ? 'info'
                  : serviceStage === 'ready'
                    ? 'success'
                    : 'neutral'
            }
          >
            {apkStatusLabel}
          </StatusPill>
        </div>

        <section className="fileeasy-guide-panel">
          <div className="fileeasy-guide-panel__title">上传就三步</div>
          <div className="fileeasy-guide-panel__subtitle">只保留扫码访客最关心的操作，不做多余说明。</div>
          <div className="fileeasy-guide-steps">
            {serviceSteps.map((step, index) => (
              <div className="fileeasy-guide-step" key={step.title}>
                <div className="fileeasy-guide-step__index">{index + 1}</div>
                <div>
                  <strong>{step.title}</strong>
                  <span>{step.detail}</span>
                </div>
              </div>
            ))}
          </div>
        </section>

        {apkMode === 'first-install' ? (
          <div className="fileeasy-guide-inline">
            <span>先连 Wi-Fi 或热点，再给访客扫码</span>
            <span>访客扫码后可直接上传文件</span>
          </div>
        ) : null}

        <FileEasyApkQrCard
          activeUploads={homeSummary?.activeUploads ?? []}
          apkMode={apkMode}
          isHomeSummaryLoading={isHomeSummaryLoading}
          serviceStage={serviceStage}
          uploadUrl={homeSummary?.uploadUrl}
          onOpenNetworkDialog={onOpenNetworkDialog}
          onOpenUploadEntry={onJumpToUpload}
          onRecoverNetwork={() => onSelectMode('normal')}
          onSetReady={onSetReady}
          onStepForward={onCycleServiceStage}
        />

        <section className="fileeasy-home-files">
          <div className="fileeasy-home-files__header">
            <div>
              <strong>最近收到</strong>
              <span>上传完成后，最近文件会直接出现在这里。</span>
            </div>
            <Button size="sm" variant="ghost" onClick={onJumpToAdmin}>
              管理全部文件
            </Button>
          </div>
          {homeSummary?.recentFiles.length ? (
            <div className="fileeasy-home-files__list">
              {homeSummary.recentFiles.slice(0, 3).map((file) => (
                <div className="fileeasy-home-file" key={file.id}>
                  <div className="fileeasy-home-file__top">
                    <strong>{file.fileName}</strong>
                    <span>{formatRelativeTime(file.createdAt)}</span>
                  </div>
                  <div className="fileeasy-home-file__meta">
                    <span>{categoryLabelMap[file.category] || '文件'}</span>
                    <span>{formatBytes(file.size)}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="fileeasy-home-files__empty">
              {isHomeSummaryLoading ? '正在读取首页摘要...' : '上传完成后，最近文件会显示在这里'}
            </div>
          )}
        </section>

        {showIllustration ? (
          <section className="fileeasy-home-illustration">
            <div className="fileeasy-home-illustration__access">
              电脑访问网页：{homeSummary?.uploadUrl || 'http://192.168.1.23:3000/'}
            </div>
            <img className="fileeasy-home-illustration__image" src={transferIllustration} alt="扫码选择文件上传示意图" />
          </section>
        ) : null}

        <div className="fileeasy-home-footer-actions">
          <Button variant="ghost" onClick={onOpenInfoDialog}>
            查看说明
          </Button>
          {apkMode === 'first-install' ? (
            <Button variant="secondary" onClick={onCompleteOnboarding}>
              完成首次安装
            </Button>
          ) : null}
        </div>
      </div>
    </div>
  );
};

export default FileEasyApkHomeScene;
