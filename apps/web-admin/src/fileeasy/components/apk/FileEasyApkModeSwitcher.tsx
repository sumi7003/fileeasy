import React from 'react';
import type { ApkMode } from '../../types/apk';
import Button from '../shared/Button';

type FileEasyApkModeSwitcherProps = {
  mode: ApkMode;
  onSelect: (mode: ApkMode) => void;
};

const FileEasyApkModeSwitcher: React.FC<FileEasyApkModeSwitcherProps> = ({ mode, onSelect }) => {
  return (
    <div className="fileeasy-mode-switcher">
      <Button
        className={mode === 'first-install' ? 'active-chip' : undefined}
        variant={mode === 'first-install' ? 'secondary' : 'ghost'}
        onClick={() => onSelect('first-install')}
      >
        首次安装态
      </Button>
      <Button
        className={mode === 'normal' ? 'active-chip' : undefined}
        variant={mode === 'normal' ? 'secondary' : 'ghost'}
        onClick={() => onSelect('normal')}
      >
        正常运行态
      </Button>
      <Button
        className={mode === 'network-missing' ? 'active-chip' : undefined}
        variant={mode === 'network-missing' ? 'secondary' : 'ghost'}
        onClick={() => onSelect('network-missing')}
      >
        无网络态
      </Button>
    </div>
  );
};

export default FileEasyApkModeSwitcher;
