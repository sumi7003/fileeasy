import React from 'react';
import Button from '../shared/Button';
import DialogShell from '../shared/DialogShell';
import Input from '../shared/Input';

type FileEasyAdminPrototypeRenameDialogProps = {
  extension: string;
  value: string;
  onClose: () => void;
  onSave: () => void;
};

const FileEasyAdminPrototypeRenameDialog: React.FC<FileEasyAdminPrototypeRenameDialogProps> = ({
  extension,
  value,
  onClose,
  onSave,
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
          <Button variant="primary" onClick={onSave}>
            保存
          </Button>
        </>
      }
      onClose={onClose}
      panelClassName="fileeasy-modal"
      title="重命名文件"
    >
      <div className="fileeasy-rename-row">
        <Input value={value} onChange={() => undefined} readOnly />
        <span className="fileeasy-extension">{extension}</span>
      </div>
    </DialogShell>
  );
};

export default FileEasyAdminPrototypeRenameDialog;
