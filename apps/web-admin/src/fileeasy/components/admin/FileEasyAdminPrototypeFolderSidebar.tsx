import React from 'react';
import type { FolderKey } from '../../types/file';

type FolderOption = {
  hint: string;
  key: FolderKey;
  label: string;
};

type FileEasyAdminPrototypeFolderSidebarProps = {
  folderGlyphs: Record<string, string>;
  folderOptions: FolderOption[];
  folderStats: Record<string, number>;
  selectedFolder: FolderKey;
  onOpenFolder: (folderKey: FolderKey) => void;
};

const FileEasyAdminPrototypeFolderSidebar: React.FC<FileEasyAdminPrototypeFolderSidebarProps> = ({
  folderGlyphs,
  folderOptions,
  folderStats,
  selectedFolder,
  onOpenFolder,
}) => {
  return (
    <div className="fileeasy-folder-sidebar">
      <div className="fileeasy-folder-sidebar__header">
        <strong>类型文件夹</strong>
        <span>按文件类型整理内容</span>
      </div>
      <div className="fileeasy-folder-list">
        {folderOptions.map((folder) => {
          const active = selectedFolder === folder.key;
          return (
            <button
              key={folder.key}
              className={`fileeasy-folder-item ${active ? 'active' : ''}`}
              type="button"
              onClick={() => onOpenFolder(folder.key)}
            >
              <span className="fileeasy-folder-icon">{folderGlyphs[folder.key]}</span>
              <div>
                <strong>{folder.label}</strong>
                <small>{folder.hint}</small>
              </div>
              <span className="fileeasy-folder-count">{folderStats[folder.key] ?? 0}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
};

export default FileEasyAdminPrototypeFolderSidebar;
