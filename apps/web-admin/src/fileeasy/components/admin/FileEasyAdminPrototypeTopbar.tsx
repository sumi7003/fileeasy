import React from 'react';
import Button from '../shared/Button';
import Input from '../shared/Input';

type FileEasyAdminPrototypeTopbarProps = {
  fileCount: number;
  previewableCount: number;
  searchPlaceholder: string;
  searchValue: string;
  selectedFolderLabel: string;
  onEnterBatchMode: () => void;
  onExitDemoLogin: () => void;
  onJumpToApkHome: () => void;
  onSearchChange: (value: string) => void;
};

const FileEasyAdminPrototypeTopbar: React.FC<FileEasyAdminPrototypeTopbarProps> = ({
  fileCount,
  previewableCount,
  searchPlaceholder,
  searchValue,
  selectedFolderLabel,
  onEnterBatchMode,
  onExitDemoLogin,
  onJumpToApkHome,
  onSearchChange,
}) => {
  return (
    <>
      <div className="fileeasy-admin-topbar fileeasy-admin-topbar--hero">
        <div>
          <div className="fileeasy-page-title">文件管理</div>
          <div className="fileeasy-subtitle">搜索、预览、重命名、删除和下载</div>
        </div>
        <div className="fileeasy-inline-actions">
          <Button variant="ghost" onClick={onJumpToApkHome}>
            回到 APK 首页
          </Button>
          <Button variant="primary" onClick={onEnterBatchMode}>
            批量删除模式
          </Button>
        </div>
      </div>

      <div className="fileeasy-admin-metrics">
        <div className="fileeasy-metric-card">
          <span>文件总数</span>
          <strong>{fileCount}</strong>
        </div>
        <div className="fileeasy-metric-card">
          <span>当前目录</span>
          <strong>{selectedFolderLabel}</strong>
        </div>
        <div className="fileeasy-metric-card">
          <span>支持预览</span>
          <strong>{previewableCount} 项</strong>
        </div>
      </div>

      <div className="fileeasy-admin-toolbar">
        <Input
          aria-label="搜索文件"
          placeholder={searchPlaceholder}
          value={searchValue}
          onChange={(event) => onSearchChange(event.target.value)}
        />
        <Button variant="secondary" onClick={onExitDemoLogin}>
          退出登录
        </Button>
      </div>
    </>
  );
};

export default FileEasyAdminPrototypeTopbar;
