import React from 'react';
import Button from '../shared/Button';

type FileEasyAdminPrototypeFileActionsProps = {
  fileName: string;
  previewable: boolean;
  onDelete: () => void;
  onDownload: () => void;
  onPreview: () => void;
  onRename: () => void;
};

const FileEasyAdminPrototypeFileActions: React.FC<FileEasyAdminPrototypeFileActionsProps> = ({
  fileName,
  previewable,
  onDelete,
  onDownload,
  onPreview,
  onRename,
}) => {
  return (
    <div className="fileeasy-file-card__actions">
      <Button variant="ghost" onClick={onPreview}>
        预览
      </Button>
      <Button variant="ghost" onClick={onDownload}>
        下载
      </Button>
      <Button variant="ghost" onClick={onRename}>
        重命名
      </Button>
      <Button className="danger-text" variant="ghost" onClick={onDelete}>
        删除
      </Button>
      {!previewable ? <span className="fileeasy-admin-inline-note">当前类型不支持在线预览</span> : null}
      <span className="sr-only">{fileName}</span>
    </div>
  );
};

export default FileEasyAdminPrototypeFileActions;
