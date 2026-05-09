import React from 'react';
import ProgressBar from '../shared/ProgressBar';
import StatusPill from '../shared/StatusPill';
import type {
  FileEasyPrototypeStatusCopy,
  FileEasyPrototypeStatusToneMap,
  FileEasyPrototypeUploadTask,
} from '../../types/prototype';

type FileEasyUploadPrototypeTaskCardProps = {
  statusCopy: FileEasyPrototypeStatusCopy;
  statusToneMap: FileEasyPrototypeStatusToneMap;
  task: FileEasyPrototypeUploadTask;
  onClick: (taskId: string) => void;
};

const FileEasyUploadPrototypeTaskCard: React.FC<FileEasyUploadPrototypeTaskCardProps> = ({
  statusCopy,
  statusToneMap,
  task,
  onClick,
}) => {
  const copy = statusCopy[task.status];

  return (
    <button
      className={`fileeasy-task-card ${copy.className}`}
      type="button"
      onClick={() => onClick(task.id)}
    >
      <div className="fileeasy-task-card__top">
        <strong>{task.name}</strong>
        <StatusPill className={`fileeasy-status-pill ${copy.className}`} tone={statusToneMap[task.status]}>
          {task.status === 'uploading' ? `${task.progress}%` : copy.label}
        </StatusPill>
      </div>
      <ProgressBar
        className="fileeasy-progress"
        fillClassName="fileeasy-progress__fill"
        tone={task.status === 'done' ? 'success' : task.status === 'failed' ? 'danger' : 'default'}
        value={task.progress}
      />
      <div className="fileeasy-task-card__meta">
        <span>{task.size}</span>
        <span>{task.note ?? copy.note ?? copy.label}</span>
      </div>
    </button>
  );
};

export default FileEasyUploadPrototypeTaskCard;
