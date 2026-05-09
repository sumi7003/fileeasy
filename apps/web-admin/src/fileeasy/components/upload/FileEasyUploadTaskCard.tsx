import React from 'react';
import type { UploadTask } from '../../types/upload';
import Button from '../shared/Button';
import ProgressBar from '../shared/ProgressBar';
import StatusPill from '../shared/StatusPill';

type FileEasyUploadTaskCardProps = {
  formatBytes: (bytes: number) => string;
  statusLabelMap: Record<UploadTask['status'], string>;
  statusToneMap: Record<UploadTask['status'], 'neutral' | 'info' | 'success' | 'warning' | 'danger'>;
  task: UploadTask;
  onRemove: (task: UploadTask) => void;
  onRetry: (task: UploadTask) => void;
};

const FileEasyUploadTaskCard: React.FC<FileEasyUploadTaskCardProps> = ({
  formatBytes,
  statusLabelMap,
  statusToneMap,
  task,
  onRemove,
  onRetry,
}) => {
  return (
    <article className={`fileeasy-task-card ${task.status}`}>
      <div className="fileeasy-task-card__top">
        <div>
          <strong>{task.finalName || task.fileName}</strong>
          <span>{formatBytes(task.fileSize)}</span>
        </div>
        <StatusPill className={`fileeasy-status-pill ${task.status}`} tone={statusToneMap[task.status]}>
          {statusLabelMap[task.status]}
        </StatusPill>
      </div>

      <ProgressBar
        className="fileeasy-progress"
        fillClassName="fileeasy-progress__fill"
        tone={task.status === 'completed' ? 'success' : task.status === 'failed' ? 'danger' : 'default'}
        value={task.progress}
      />

      <div className="fileeasy-task-card__meta">
        <span>{task.progress}%</span>
        <span>
          {task.uploadedChunkCount > 0 || task.totalChunks > 0
            ? `${task.uploadedChunkCount}/${task.totalChunks} 个分片`
            : '等待开始'}
        </span>
      </div>

      <p className="fileeasy-task-card__note">{task.note}</p>

      {task.errorMessage ? <div className="fileeasy-feedback error soft">{task.errorMessage}</div> : null}

      <div className="fileeasy-task-actions">
        {(task.status === 'paused' || task.status === 'failed') && task.file ? (
          <Button className="fileeasy-button secondary" variant="secondary" onClick={() => onRetry(task)}>
            {task.uploadId ? '继续上传' : '重新上传'}
          </Button>
        ) : null}

        {task.needsFileReselect ? <span className="fileeasy-task-tip">重新选择同一个原文件后可继续上传</span> : null}

        <Button className="fileeasy-button ghost" variant="ghost" onClick={() => onRemove(task)}>
          {task.status === 'completed' ? '清除记录' : '移除任务'}
        </Button>
      </div>
    </article>
  );
};

export default FileEasyUploadTaskCard;
