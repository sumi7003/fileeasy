import React from 'react';
import Button from '../shared/Button';
import Input from '../shared/Input';

type FileEasyUploadSidePanelProps = {
  isAuthenticated: boolean;
  isLoggingIn: boolean;
  loginError: string;
  onLogout: () => void;
  pendingTaskCount: number;
  password: string;
  showPassword: boolean;
  onLogin: () => void;
  onPasswordChange: (value: string) => void;
  onPasswordKeyDown: (event: React.KeyboardEvent<HTMLInputElement>) => void;
  onTogglePassword: () => void;
};

const FileEasyUploadSidePanel: React.FC<FileEasyUploadSidePanelProps> = ({
  isAuthenticated,
  isLoggingIn,
  loginError,
  onLogout,
  pendingTaskCount,
  password,
  showPassword,
  onLogin,
  onPasswordChange,
  onPasswordKeyDown,
  onTogglePassword,
}) => {
  if (!isAuthenticated) {
    return (
      <div className="fileeasy-card fileeasy-card--login">
        <div className="fileeasy-card__header">
          <div>
            <div className="fileeasy-step-card__marker">第一步</div>
            <h2>输入密码</h2>
          </div>
        </div>

        <label className="fileeasy-field">
          <span>密码</span>
          <div className="fileeasy-password-row">
            <Input
              type={showPassword ? 'text' : 'password'}
              placeholder="输入密码"
              value={password}
              onChange={(event) => onPasswordChange(event.target.value)}
              onKeyDown={onPasswordKeyDown}
            />
            <Button className="fileeasy-button" variant="ghost" onClick={onTogglePassword}>
              {showPassword ? '隐藏' : '显示'}
            </Button>
          </div>
        </label>

        {loginError ? <div className="fileeasy-feedback error">{loginError}</div> : null}

        <div className="fileeasy-inline-note">验证后 7 天内免密</div>

        {pendingTaskCount > 0 ? (
          <div className="fileeasy-inline-warning">
            <strong>待继续任务</strong>
            <span>{pendingTaskCount} 个</span>
          </div>
        ) : null}

        <Button
          block
          className="fileeasy-button primary"
          disabled={isLoggingIn}
          variant="primary"
          onClick={onLogin}
        >
          {isLoggingIn ? '验证中...' : '确认密码'}
        </Button>
      </div>
    );
  }

  return (
    <div className="fileeasy-card fileeasy-card--session">
      <div className="fileeasy-card__header">
        <div>
          <div className="fileeasy-step-card__marker">第一步</div>
          <h2>密码已验证</h2>
        </div>
        <Button className="fileeasy-button ghost" variant="ghost" onClick={onLogout}>
          退出
        </Button>
      </div>

      <div className="fileeasy-inline-success">
        <strong>可直接上传</strong>
        <span>7 天内免密</span>
      </div>
    </div>
  );
};

export default FileEasyUploadSidePanel;
