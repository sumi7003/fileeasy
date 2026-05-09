import React from 'react';
import { Link } from 'react-router-dom';
import Button from '../shared/Button';

type FeedbackTone = 'success' | 'error' | 'warning';

type FileEasyAdminHeroProps = {
  feedback: { message: string; tone: FeedbackTone } | null;
  isBatchMode: boolean;
  onEnterBatchMode: () => void;
  onExitBatchMode: () => void;
  onLogout: () => void;
};

const FileEasyAdminHero: React.FC<FileEasyAdminHeroProps> = ({
  feedback,
  isBatchMode,
  onEnterBatchMode,
  onExitBatchMode,
  onLogout,
}) => {
  return (
    <>
      <header className="fileeasy-admin-hero">
        <div>
          <span className="fileeasy-admin-eyebrow">文件管理</span>
          <h1>{isBatchMode ? '批量删除模式' : '文件管理'}</h1>
          <p>
            {isBatchMode
              ? '先勾选要处理的文件，再统一删除。'
              : '查看最近上传到当前设备的文件，支持搜索、预览、下载、重命名和删除。'}
          </p>
        </div>
        <div className="fileeasy-admin-hero-actions">
          <Link className="fileeasy-admin-button ghost" to="/">
            返回上传页
          </Link>
          {isBatchMode ? (
            <Button className="fileeasy-admin-button secondary" variant="secondary" onClick={onExitBatchMode}>
              退出批量
            </Button>
          ) : (
            <Button className="fileeasy-admin-button secondary" variant="secondary" onClick={onEnterBatchMode}>
              批量删除
            </Button>
          )}
          <Button className="fileeasy-admin-button ghost" variant="ghost" onClick={onLogout}>
            退出登录
          </Button>
        </div>
      </header>

      {feedback ? <div className={`fileeasy-admin-feedback-banner ${feedback.tone}`}>{feedback.message}</div> : null}
    </>
  );
};

export default FileEasyAdminHero;
