import React from 'react';
import type { ApkDialog } from '../../types/apk';
import Button from '../shared/Button';
import DialogShell from '../shared/DialogShell';
import Input from '../shared/Input';

type FileEasyApkDialogProps = {
  currentPassword: string;
  displayPassword: string;
  dialog: Exclude<ApkDialog, 'none'>;
  nextPassword: string;
  confirmPassword: string;
  onClose: () => void;
  onCompleteOnboarding: () => void;
  onConfirmPasswordChange: () => void;
  onCurrentPasswordChange: (value: string) => void;
  onGoToPasswordDialog: () => void;
  onNextPasswordChange: (value: string) => void;
  onConfirmPasswordInputChange: (value: string) => void;
  onRecoverNetwork: () => void;
};

const FileEasyApkDialog: React.FC<FileEasyApkDialogProps> = ({
  currentPassword,
  displayPassword,
  dialog,
  nextPassword,
  confirmPassword,
  onClose,
  onCompleteOnboarding,
  onConfirmPasswordChange,
  onCurrentPasswordChange,
  onGoToPasswordDialog,
  onNextPasswordChange,
  onConfirmPasswordInputChange,
  onRecoverNetwork,
}) => {
  return (
    <DialogShell
      backdropClassName="fileeasy-modal-backdrop"
      compact
      footer={
        dialog === 'welcome' ? (
          <>
            <Button variant="ghost" onClick={onClose}>
              稍后处理
            </Button>
            <Button variant="secondary" onClick={onCompleteOnboarding}>
              直接开始使用
            </Button>
            <Button variant="primary" onClick={onGoToPasswordDialog}>
              去修改密码
            </Button>
          </>
        ) : dialog === 'password' ? (
          <>
            <Button variant="ghost" onClick={onClose}>
              取消
            </Button>
            <Button
              variant="primary"
              onClick={onConfirmPasswordChange}
              disabled={!nextPassword || nextPassword !== confirmPassword}
            >
              保存密码
            </Button>
          </>
        ) : (
          <>
            <Button variant="ghost" onClick={onClose}>
              我知道了
            </Button>
            <Button variant="primary" onClick={onRecoverNetwork}>
              模拟网络恢复
            </Button>
          </>
        )
      }
      onClose={onClose}
      panelClassName="fileeasy-modal"
      title={
        dialog === 'welcome'
          ? '首次使用说明'
          : dialog === 'password'
            ? '修改系统密码'
            : '当前未检测到可用局域网地址'
      }
    >
      {dialog === 'welcome' ? (
        <div className="fileeasy-guide-list">
          <span>默认密码为 {displayPassword}</span>
          <span>上传页和管理页共用同一套密码</span>
          <span>建议先连接 Wi-Fi 或热点，再给他人扫码上传</span>
          <span>首次安装建议立即修改默认密码</span>
        </div>
      ) : null}

      {dialog === 'password' ? (
        <>
          <div className="fileeasy-form-row">
            <label htmlFor="apk-password-current">当前密码</label>
            <Input
              id="apk-password-current"
              value={currentPassword}
              onChange={(event) => onCurrentPasswordChange(event.target.value)}
            />
          </div>
          <div className="fileeasy-form-row">
            <label htmlFor="apk-password-next">新密码</label>
            <Input
              id="apk-password-next"
              placeholder="请输入新的系统密码"
              value={nextPassword}
              onChange={(event) => onNextPasswordChange(event.target.value)}
            />
          </div>
          <div className="fileeasy-form-row">
            <label htmlFor="apk-password-confirm">确认新密码</label>
            <Input
              id="apk-password-confirm"
              placeholder="再次输入新密码"
              value={confirmPassword}
              onChange={(event) => onConfirmPasswordInputChange(event.target.value)}
            />
          </div>
          <div className="fileeasy-muted">保存后，上传页与管理页都将使用新密码访问。</div>
          {nextPassword && confirmPassword && nextPassword !== confirmPassword ? (
            <div className="fileeasy-error">两次输入的新密码不一致，请重新确认</div>
          ) : null}
        </>
      ) : null}

      {dialog === 'network' ? (
        <div className="fileeasy-guide-list">
          <span>请确认设备已连接 Wi-Fi 或已开启热点</span>
          <span>网络可用后，首页会自动刷新上传地址和二维码</span>
          <span>在网络恢复前，二维码和上传地址不会展示给其他设备</span>
        </div>
      ) : null}
    </DialogShell>
  );
};

export default FileEasyApkDialog;
