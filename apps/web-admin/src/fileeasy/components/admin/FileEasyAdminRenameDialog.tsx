import React from 'react';
import type { FileItem } from '../../types/file';
import Button from '../shared/Button';
import DialogShell from '../shared/DialogShell';
import Input from '../shared/Input';

type FileEasyAdminRenameDialogProps = {
  error: string;
  file: FileItem;
  isSubmitting: boolean;
  value: string;
  onChange: (value: string) => void;
  onClose: () => void;
  onSubmit: () => void;
};

const FileEasyAdminRenameDialog: React.FC<FileEasyAdminRenameDialogProps> = ({
  error,
  file,
  isSubmitting,
  value,
  onChange,
  onClose,
  onSubmit,
}) => {
  return (
    <DialogShell
      backdropClassName="fileeasy-admin-modal-backdrop"
      bodyClassName="fileeasy-admin-modal-body"
      compact
      footer={
        <>
          <Button className="fileeasy-admin-button ghost" variant="ghost" onClick={onClose}>
            取消
          </Button>
          <Button className="fileeasy-admin-button primary" disabled={isSubmitting} variant="primary" onClick={onSubmit}>
            {isSubmitting ? '保存中...' : '保存'}
          </Button>
        </>
      }
      footerClassName="fileeasy-admin-modal-footer"
      headerClassName="fileeasy-admin-modal-header"
      onClose={onClose}
      panelClassName="fileeasy-admin-modal"
      title="重命名文件"
    >
      <label className="fileeasy-admin-field">
        <span>主文件名</span>
        <div className="fileeasy-admin-rename-row">
          <Input value={value} onChange={(event) => onChange(event.target.value)} />
          {file.extension ? <span className="fileeasy-admin-extension">.{file.extension}</span> : null}
        </div>
      </label>
      <p className="fileeasy-admin-helper">这里只允许修改主文件名，扩展名会保持锁定，不允许变更。</p>
      {error ? <div className="fileeasy-admin-feedback error">{error}</div> : null}
    </DialogShell>
  );
};

export default FileEasyAdminRenameDialog;
