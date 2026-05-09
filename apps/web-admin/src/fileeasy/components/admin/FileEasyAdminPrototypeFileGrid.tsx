import React from 'react';

type DemoFile = {
  folder: string;
  id: string;
  kind: string;
  name: string;
  size: string;
  time: string;
  previewable?: boolean;
};

type FileEasyAdminPrototypeFileGridProps<T extends DemoFile> = {
  emptyState: React.ReactNode;
  fileKindGlyphs: Record<string, string>;
  files: T[];
  selectedFolderLabel: string;
  onRenderActions: (file: T) => React.ReactNode;
};

const FileEasyAdminPrototypeFileGrid = <T extends DemoFile>({
  emptyState,
  fileKindGlyphs,
  files,
  selectedFolderLabel,
  onRenderActions,
}: FileEasyAdminPrototypeFileGridProps<T>) => {
  return (
    <div className="fileeasy-file-pane">
      <div className="fileeasy-pane-header">
        <div>
          <div className="fileeasy-pane-header__path">文件管理 / {selectedFolderLabel}</div>
          <strong>{selectedFolderLabel}</strong>
        </div>
        <span>{files.length} 个文件</span>
      </div>

      <div className="fileeasy-file-grid">
        {files.length === 0
          ? emptyState
          : files.map((file) => (
              <div className={`fileeasy-file-card fileeasy-file-card--${file.folder}`} key={file.id}>
                <div className="fileeasy-file-thumb">
                  <span>{fileKindGlyphs[file.kind]}</span>
                </div>
                <div className="fileeasy-file-card__title-row">
                  <strong>{file.name}</strong>
                  <span className="fileeasy-kind-pill">{file.kind}</span>
                </div>
                <div className="fileeasy-file-card__meta">
                  <span>{file.size}</span>
                  <span>{file.time}</span>
                </div>
                {onRenderActions(file)}
              </div>
            ))}
      </div>
    </div>
  );
};

export default FileEasyAdminPrototypeFileGrid;
