import React from 'react';
import type { UploadTask } from '../../types/upload';
import EmptyState from '../shared/EmptyState';
import Button from '../shared/Button';
import FileEasyUploadTaskCard from './FileEasyUploadTaskCard';

type FileEasyUploadTasksSectionProps = {
  completedCount: number;
  formatBytes: (bytes: number) => string;
  isAuthenticated: boolean;
  onClearCompleted: () => void;
  statusLabelMap: Record<UploadTask['status'], string>;
  statusToneMap: Record<UploadTask['status'], 'neutral' | 'info' | 'success' | 'warning' | 'danger'>;
  tasks: UploadTask[];
  onRemove: (task: UploadTask) => void;
  onRetry: (task: UploadTask) => void;
};

const FileEasyUploadTasksSection: React.FC<FileEasyUploadTasksSectionProps> = ({
  completedCount,
  formatBytes,
  isAuthenticated,
  onClearCompleted,
  statusLabelMap,
  statusToneMap,
  tasks,
  onRemove,
  onRetry,
}) => {
  return (
    <section className="fileeasy-card fileeasy-card--tasks">
      <div className="fileeasy-card__header">
        <div>
          <div className="fileeasy-step-card__marker">第三步</div>
          <h2>上传状态</h2>
        </div>
        <div className="fileeasy-tasks-toolbar">
          <span className="fileeasy-card__hint">{tasks.length} 个任务</span>
          <Button
            className="fileeasy-button ghost"
            disabled={completedCount === 0}
            size="sm"
            variant="ghost"
            onClick={onClearCompleted}
          >
            清除已完成
          </Button>
        </div>
      </div>

      {!tasks.length ? (
        <EmptyState
          className="fileeasy-empty-state"
          description={
            isAuthenticated ? '选择文件后显示' : '输入密码后显示'
          }
          title={isAuthenticated ? '暂无任务' : '等待上传'}
        />
      ) : (
        <div className="fileeasy-task-list">
          {tasks.map((task) => (
            <FileEasyUploadTaskCard
              key={task.id}
              formatBytes={formatBytes}
              statusLabelMap={statusLabelMap}
              statusToneMap={statusToneMap}
              task={task}
              onRemove={onRemove}
              onRetry={onRetry}
            />
          ))}
        </div>
      )}
    </section>
  );
};

export default FileEasyUploadTasksSection;
