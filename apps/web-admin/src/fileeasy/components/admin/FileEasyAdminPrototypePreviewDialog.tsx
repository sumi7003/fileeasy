import React from 'react';
import type { FileKind } from '../../types/file';
import DialogShell from '../shared/DialogShell';

type FileEasyAdminPrototypePreviewDialogProps = {
  fileKind: FileKind;
  title: string;
  onClose: () => void;
};

const FileEasyAdminPrototypePreviewDialog: React.FC<FileEasyAdminPrototypePreviewDialogProps> = ({
  fileKind,
  title,
  onClose,
}) => {
  return (
    <DialogShell
      backdropClassName="fileeasy-modal-backdrop"
      bodyClassName=""
      onClose={onClose}
      panelClassName="fileeasy-modal"
      title={title}
    >
      <div className={`fileeasy-preview fileeasy-preview--${fileKind}`}>
        {fileKind === 'PDF' ? <div className="fileeasy-preview__sheet">PDF 阅读区域</div> : null}
        {fileKind === '视频' ? <div className="fileeasy-preview__sheet">视频播放器区域</div> : null}
        {fileKind === '图片' ? <div className="fileeasy-preview__sheet">图片预览区域</div> : null}
        {fileKind === '音频' ? <div className="fileeasy-preview__sheet">音频播放条区域</div> : null}
        {fileKind === 'ZIP' ? <div className="fileeasy-preview__sheet">当前类型不支持预览</div> : null}
        {fileKind === '文档' ? <div className="fileeasy-preview__sheet">文档阅读区域</div> : null}
      </div>
    </DialogShell>
  );
};

export default FileEasyAdminPrototypePreviewDialog;
