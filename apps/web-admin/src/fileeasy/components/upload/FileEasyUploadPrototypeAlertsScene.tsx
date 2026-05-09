import React from 'react';
import Button from '../shared/Button';
import type {
  FileEasyPrototypeStatusCopy,
  FileEasyPrototypeStatusToneMap,
  FileEasyPrototypeUploadTask,
} from '../../types/prototype';
import FileEasyUploadPrototypeTaskCard from './FileEasyUploadPrototypeTaskCard';

type AlertMode = 'none' | 'space' | 'network' | 'expired';

type FileEasyUploadPrototypeAlertsSceneProps = {
  alertMode: AlertMode;
  statusCopy: FileEasyPrototypeStatusCopy;
  statusToneMap: FileEasyPrototypeStatusToneMap;
  tasks: FileEasyPrototypeUploadTask[];
  onAlertModeChange: (mode: Exclude<AlertMode, 'none'>) => void;
  onToggleTaskStatus: (taskId: string) => void;
};

const FileEasyUploadPrototypeAlertsScene: React.FC<FileEasyUploadPrototypeAlertsSceneProps> = ({
  alertMode,
  statusCopy,
  statusToneMap,
  tasks,
  onAlertModeChange,
  onToggleTaskStatus,
}) => {
  const renderAlertNotice = () => {
    if (alertMode === 'space') {
      return (
        <div className="fileeasy-alert-card warning">
          <strong>设备空间不足，请联系管理员清理</strong>
          <span>当前无法发起新的上传</span>
        </div>
      );
    }

    if (alertMode === 'network') {
      return (
        <div className="fileeasy-alert-card neutral">
          <strong>网络中断，正在等待恢复</strong>
          <span>现有进度会被保留，恢复后继续上传</span>
        </div>
      );
    }

    if (alertMode === 'expired') {
      return (
        <div className="fileeasy-alert-card danger">
          <strong>上传任务已过期，请重新上传</strong>
          <span>超过 24 小时的未完成任务已失效</span>
        </div>
      );
    }

    return null;
  };

  return (
    <div className="fileeasy-device fileeasy-device--mobile">
      <div className="fileeasy-screen fileeasy-screen--web">
        <div className="fileeasy-web-header">
          <div>
            <div className="fileeasy-page-title">FileEasy 文件上传</div>
            <div className="fileeasy-subtitle">异常反馈示意</div>
          </div>
        </div>

        <div className="fileeasy-inline-actions">
          <Button variant={alertMode === 'space' ? 'primary' : 'ghost'} onClick={() => onAlertModeChange('space')}>
            空间不足
          </Button>
          <Button variant={alertMode === 'network' ? 'primary' : 'ghost'} onClick={() => onAlertModeChange('network')}>
            网络中断
          </Button>
          <Button variant={alertMode === 'expired' ? 'primary' : 'ghost'} onClick={() => onAlertModeChange('expired')}>
            续传过期
          </Button>
        </div>

        {renderAlertNotice()}

        <div className={`fileeasy-upload-card ${alertMode === 'space' ? 'fileeasy-upload-card--disabled' : ''}`}>
          <Button variant="primary" disabled={alertMode === 'space'}>
            选择文件
          </Button>
          <div className="fileeasy-capability-list">
            <span>支持单文件 / 多文件上传</span>
            <span>单文件最大 4GB</span>
            <span>支持断点续传</span>
          </div>
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
  );
};

export default FileEasyUploadPrototypeAlertsScene;
