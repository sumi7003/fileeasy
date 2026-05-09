import React from 'react';
import Button from '../shared/Button';
import type { FolderKey } from '../../types/file';
import FileEasyAdminPrototypeFolderSidebar from './FileEasyAdminPrototypeFolderSidebar';

type FolderOption = {
  hint: string;
  key: FolderKey;
  label: string;
};

type DemoFile = {
  folder: FolderKey;
  id: string;
  kind: string;
  name: string;
  size: string;
  time: string;
};

type FileEasyAdminPrototypeBatchSceneProps = {
  emptyState: React.ReactNode;
  files: DemoFile[];
  folderGlyphs: Record<FolderKey, string>;
  folderOptions: FolderOption[];
  folderStats: Record<FolderKey, number>;
  selectedFileIds: string[];
  selectedFolder: FolderKey;
  selectedFolderLabel: string;
  onDeleteSelected: () => void;
  onExitBatch: () => void;
  onOpenFolder: (folder: FolderKey) => void;
  onToggleSelection: (fileId: string) => void;
};

const FileEasyAdminPrototypeBatchScene: React.FC<FileEasyAdminPrototypeBatchSceneProps> = ({
  emptyState,
  files,
  folderGlyphs,
  folderOptions,
  folderStats,
  selectedFileIds,
  selectedFolder,
  selectedFolderLabel,
  onDeleteSelected,
  onExitBatch,
  onOpenFolder,
  onToggleSelection,
}) => {
  return (
    <div className="fileeasy-device fileeasy-device--desktop">
      <div className="fileeasy-screen fileeasy-screen--admin">
        <div className="fileeasy-admin-topbar">
          <div>
            <div className="fileeasy-page-title">文件管理</div>
            <div className="fileeasy-subtitle">已切换为批量删除模式</div>
          </div>
          <Button variant="ghost" onClick={onExitBatch}>
            退出批量
          </Button>
        </div>

        <div className="fileeasy-batch-bar">
          <strong>已选择 {selectedFileIds.length} 个文件</strong>
          <Button variant="danger" onClick={onDeleteSelected}>
            删除已选文件
          </Button>
        </div>

        <div className="fileeasy-admin-layout">
          <FileEasyAdminPrototypeFolderSidebar
            folderGlyphs={folderGlyphs}
            folderOptions={folderOptions}
            folderStats={folderStats}
            selectedFolder={selectedFolder}
            onOpenFolder={onOpenFolder}
          />

          <div className="fileeasy-file-pane">
            <div className="fileeasy-pane-header">
              <div>
                <div className="fileeasy-pane-header__path">文件管理 / {selectedFolderLabel}</div>
                <strong>{selectedFolderLabel}</strong>
              </div>
              <span>已选 {selectedFileIds.length} 个文件</span>
            </div>

            <div className="fileeasy-batch-list">
              {files.map((file) => {
                const checked = selectedFileIds.includes(file.id);
                return (
                  <button
                    key={file.id}
                    className={`fileeasy-batch-row ${checked ? 'selected' : ''}`}
                    type="button"
                    onClick={() => onToggleSelection(file.id)}
                  >
                    <span className="fileeasy-check">{checked ? '√' : ''}</span>
                    <span className="fileeasy-batch-name">{file.name}</span>
                    <span>{file.kind}</span>
                    <span>{file.size}</span>
                    <span>{file.time}</span>
                  </button>
                );
              })}
              {files.length === 0 ? emptyState : null}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FileEasyAdminPrototypeBatchScene;
