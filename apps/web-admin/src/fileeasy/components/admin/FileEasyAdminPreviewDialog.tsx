import React from 'react';
import type { FileItem, FolderKey } from '../../types/file';
import DialogShell from '../shared/DialogShell';

type FileEasyAdminPreviewDialogProps = {
  content: React.ReactNode;
  previewFile: FileItem;
  previewTitles: Partial<Record<FolderKey, string>>;
  onClose: () => void;
};

const FileEasyAdminPreviewDialog: React.FC<FileEasyAdminPreviewDialogProps> = ({
  content,
  previewFile,
  previewTitles,
  onClose,
}) => {
  return (
    <DialogShell
      backdropClassName="fileeasy-admin-modal-backdrop"
      bodyClassName="fileeasy-admin-modal-body"
      headerClassName="fileeasy-admin-modal-header"
      onClose={onClose}
      panelClassName="fileeasy-admin-modal preview"
      title={previewTitles[previewFile.folder] || '文件预览'}
    >
      {content}
    </DialogShell>
  );
};

export default FileEasyAdminPreviewDialog;
