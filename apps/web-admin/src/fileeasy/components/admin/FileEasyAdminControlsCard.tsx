import React from 'react';
import { FILEEASY_FOLDER_LABELS } from '../../constants/fileTypes';
import type { FolderKey } from '../../types/file';
import Button from '../shared/Button';
import Input from '../shared/Input';

type FileEasyAdminControlsCardProps = {
  allVisibleSelected: boolean;
  folderCounts: Record<FolderKey, number>;
  folderOrder: FolderKey[];
  isBatchMode: boolean;
  isLoading: boolean;
  query: string;
  selectedFolder: FolderKey;
  onQueryChange: (value: string) => void;
  onRefresh: () => void;
  onSelectFolder: (folder: FolderKey) => void;
  onToggleSelectVisible: () => void;
};

const FileEasyAdminControlsCard: React.FC<FileEasyAdminControlsCardProps> = ({
  allVisibleSelected,
  folderCounts,
  folderOrder,
  isBatchMode,
  isLoading,
  query,
  selectedFolder,
  onQueryChange,
  onRefresh,
  onSelectFolder,
  onToggleSelectVisible,
}) => {
  const origin = typeof window === 'undefined' ? '' : window.location.origin;
  const uploadUrl = origin ? `${origin}/` : '/';
  const adminUrl = origin ? `${origin}/admin` : '/admin';
  const saveFolderHint = '文件/FileEasy';
  const copyText = async (value: string) => {
    if (typeof navigator === 'undefined' || !navigator.clipboard) return;
    try {
      await navigator.clipboard.writeText(value);
    } catch (error) {
      console.warn('Failed to copy FileEasy admin value', error);
    }
  };

  return (
    <section className="fileeasy-admin-card">
      <div className="fileeasy-admin-address-grid">
        <article className="fileeasy-admin-address-card">
          <span>上传地址</span>
          <strong>{uploadUrl}</strong>
          <button type="button" onClick={() => void copyText(uploadUrl)}>
            复制
          </button>
        </article>
        <article className="fileeasy-admin-address-card">
          <span>管理页地址</span>
          <strong>{adminUrl}</strong>
          <button type="button" onClick={() => void copyText(adminUrl)}>
            复制
          </button>
        </article>
        <article className="fileeasy-admin-address-card">
          <span>默认保存目录</span>
          <strong>{saveFolderHint}</strong>
          <button type="button" onClick={() => void copyText(saveFolderHint)}>
            复制
          </button>
        </article>
      </div>

      <div className="fileeasy-admin-toolbar">
        <label className="fileeasy-admin-search">
          <span>搜索文件名</span>
          <Input
            className="fileeasy-admin-search__input"
            placeholder="搜索文件名"
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
          />
        </label>
        <div className="fileeasy-admin-toolbar-actions">
          <Button className="fileeasy-admin-button ghost" disabled={isLoading} variant="ghost" onClick={onRefresh}>
            {isLoading ? '加载中...' : '刷新列表'}
          </Button>
          {isBatchMode ? (
            <Button className="fileeasy-admin-button ghost" variant="ghost" onClick={onToggleSelectVisible}>
              {allVisibleSelected ? '取消当前结果' : '选择当前结果'}
            </Button>
          ) : null}
        </div>
      </div>

      <div className="fileeasy-admin-filter-row">
        {folderOrder.map((folder) => (
          <button
            key={folder}
            className={`fileeasy-admin-filter-chip ${selectedFolder === folder ? 'active' : ''}`}
            type="button"
            onClick={() => onSelectFolder(folder)}
          >
            <span>{FILEEASY_FOLDER_LABELS[folder]}</span>
            <strong>{folderCounts[folder]}</strong>
          </button>
        ))}
      </div>
    </section>
  );
};

export default FileEasyAdminControlsCard;
