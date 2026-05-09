import React from 'react';
import type { FileItem } from '../../types/file';
import Button from '../shared/Button';
import DialogShell from '../shared/DialogShell';

export type FileEasyAdminDeleteDialogState =
  | {
      mode: 'single';
      file: FileItem;
    }
  | {
      mode: 'batch';
      files: FileItem[];
    };

type FileEasyAdminDeleteDialogProps = {
  dialog: FileEasyAdminDeleteDialogState;
  isDeleting: boolean;
  onClose: () => void;
  onConfirm: () => void;
};

const FileEasyAdminDeleteDialog: React.FC<FileEasyAdminDeleteDialogProps> = ({
  dialog,
  isDeleting,
  onClose,
  onConfirm,
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
          <Button className="fileeasy-admin-button danger" disabled={isDeleting} variant="danger" onClick={onConfirm}>
            {isDeleting ? '删除中...' : '确认删除'}
          </Button>
        </>
      }
      footerClassName="fileeasy-admin-modal-footer"
      headerClassName="fileeasy-admin-modal-header"
      onClose={onClose}
      panelClassName="fileeasy-admin-modal"
      title={dialog.mode === 'single' ? '确认删除' : '确认批量删除'}
    >
      <p className="fileeasy-admin-danger-copy">删除后无法恢复</p>
      <strong className="fileeasy-admin-danger-copy">
        {dialog.mode === 'single'
          ? `是否删除“${dialog.file.name}”`
          : `是否删除已选 ${dialog.files.length} 个文件`}
      </strong>
    </DialogShell>
  );
};

export default FileEasyAdminDeleteDialog;
