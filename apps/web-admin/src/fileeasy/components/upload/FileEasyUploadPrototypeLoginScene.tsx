import React from 'react';
import Button from '../shared/Button';
import Input from '../shared/Input';

type FileEasyUploadPrototypeLoginSceneProps = {
  featureBadges: string[];
  passwordError: boolean;
  passwordValue: string;
  onPasswordChange: (value: string) => void;
  onSubmit: () => void;
};

const FileEasyUploadPrototypeLoginScene: React.FC<FileEasyUploadPrototypeLoginSceneProps> = ({
  featureBadges,
  passwordError,
  passwordValue,
  onPasswordChange,
  onSubmit,
}) => {
  return (
    <div className="fileeasy-device fileeasy-device--mobile">
      <div className="fileeasy-screen fileeasy-screen--web">
        <div className="fileeasy-web-header fileeasy-web-header--hero">
          <div>
            <div className="fileeasy-page-title">FileEasy 文件上传</div>
            <div className="fileeasy-subtitle">上传到当前局域网设备</div>
          </div>
          <div className="fileeasy-header-badge">移动端上传页</div>
        </div>

        <div className="fileeasy-chip-row">
          {featureBadges.map((badge) => (
            <span className="fileeasy-feature-chip" key={badge}>
              {badge}
            </span>
          ))}
        </div>

        <div className="fileeasy-upload-card fileeasy-upload-card--disabled">
          <div className="fileeasy-card-heading">
            <strong>上传文件</strong>
            <span>登录后开始上传</span>
          </div>
          <Button variant="primary" disabled>
            选择文件
          </Button>
          <div className="fileeasy-capability-list">
            <span>支持单文件 / 多文件上传</span>
            <span>上传中断后可恢复</span>
            <span>适配手机和桌面浏览器</span>
          </div>
        </div>

        <div className="fileeasy-login-card">
          <div className="fileeasy-login-card__title">输入密码后开始上传文件</div>
          <Input
            aria-label="输入密码"
            placeholder="请输入访问密码"
            value={passwordValue}
            onChange={(event) => onPasswordChange(event.target.value)}
          />
          <div className="fileeasy-inline-hint">演示密码与首页“修改密码”保持联动</div>
          {passwordError ? <div className="fileeasy-error">密码错误，请重新输入</div> : null}
          <Button variant="primary" onClick={onSubmit}>
            登录
          </Button>
        </div>

        <div className="fileeasy-ghost-card">上传任务将在登录后显示</div>
      </div>
    </div>
  );
};

export default FileEasyUploadPrototypeLoginScene;
