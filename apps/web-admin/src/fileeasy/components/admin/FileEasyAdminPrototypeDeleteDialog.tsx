import React from 'react';
import Button from '../shared/Button';
import DialogShell from '../shared/DialogShell';

type FileEasyAdminPrototypeDeleteDialogProps = {
  description: string;
  title: string;
  onClose: () => void;
  onConfirm: () => void;
};

const FileEasyAdminPrototypeDeleteDialog: React.FC<FileEasyAdminPrototypeDeleteDialogProps> = ({
  description,
  title,
  onClose,
  onConfirm,
}) => {
  return (
    <DialogShell
      backdropClassName="fileeasy-modal-backdrop"
      compact
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            取消
          </Button>
          <Button variant="danger" onClick={onConfirm}>
            确认删除
          </Button>
        </>
      }
      onClose={onClose}
      panelClassName="fileeasy-modal"
      title={title}
    >
      <div className="fileeasy-danger-copy">
        <p>删除后无法恢复</p>
        <strong>{description}</strong>
      </div>
    </DialogShell>
  );
};

export default FileEasyAdminPrototypeDeleteDialog;
