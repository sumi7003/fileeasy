import React from 'react';
import { Link } from 'react-router-dom';
import Button from '../shared/Button';
import Input from '../shared/Input';
import StatusPill from '../shared/StatusPill';

type FileEasyAdminLoginGateProps = {
  authError: string;
  isSubmitting: boolean;
  password: string;
  onLogin: () => void;
  onPasswordChange: (value: string) => void;
  onPasswordKeyDown: (event: React.KeyboardEvent<HTMLInputElement>) => void;
};

const FileEasyAdminLoginGate: React.FC<FileEasyAdminLoginGateProps> = ({
  authError,
  isSubmitting,
  password,
  onLogin,
  onPasswordChange,
  onPasswordKeyDown,
}) => {
  return (
    <div className="fileeasy-admin-page">
      <div className="fileeasy-admin-shell">
        <header className="fileeasy-admin-hero">
          <div>
            <span className="fileeasy-admin-eyebrow">文件管理</span>
            <h1>输入密码后进入文件管理</h1>
            <p>管理已经上传到当前设备的文件，重点查看最近上传、搜索文件和处理下载、重命名、删除。</p>
          </div>
          <div className="fileeasy-admin-hero-actions">
            <Link className="fileeasy-admin-button ghost" to="/">
              返回上传页
            </Link>
          </div>
        </header>

        <main className="fileeasy-admin-login-grid">
          <section className="fileeasy-admin-card">
            <div className="fileeasy-admin-card-header">
              <div>
                <h2>输入密码后进入文件管理</h2>
                <p>网页端不提供改密，只负责查看和处理已上传文件。</p>
              </div>
              <StatusPill className="fileeasy-admin-pill neutral" tone="neutral">
                未验证
              </StatusPill>
            </div>

            <label className="fileeasy-admin-field">
              <span>访问密码</span>
              <Input
                autoComplete="current-password"
                className="fileeasy-admin-field__input"
                placeholder="请输入访问密码"
                type="password"
                value={password}
                onChange={(event) => onPasswordChange(event.target.value)}
                onKeyDown={onPasswordKeyDown}
              />
            </label>

            {authError ? <div className="fileeasy-admin-feedback error">{authError}</div> : null}

            <div className="fileeasy-admin-login-actions">
              <Button className="fileeasy-admin-button primary" disabled={isSubmitting} variant="primary" onClick={onLogin}>
                {isSubmitting ? '验证中...' : '进入文件管理'}
              </Button>
              <p className="fileeasy-admin-helper">密码修改仅允许在 APK 本地入口完成。</p>
            </div>
          </section>

          <section className="fileeasy-admin-card subtle">
            <div className="fileeasy-admin-card-header">
              <div>
                <h2>进入后可完成</h2>
                <p>这是一个轻量文件管理页，不是系统后台，也不是完整文件浏览器。</p>
              </div>
            </div>
            <div className="fileeasy-admin-feature-list">
              <span>最近上传列表与文件名搜索</span>
              <span>图片 / 视频 / 音频 / PDF 在线预览</span>
              <span>下载、重命名、单个删除、批量删除</span>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
};

export default FileEasyAdminLoginGate;
