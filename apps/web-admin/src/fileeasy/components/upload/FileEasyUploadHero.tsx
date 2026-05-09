import React from 'react';
const FileEasyUploadHero: React.FC = () => {
  return (
    <header className="fileeasy-hero">
      <div className="fileeasy-hero__copy">
        <span className="fileeasy-eyebrow">FileEasy</span>
        <h1>上传文件</h1>
        <div className="fileeasy-network-note">
          <strong>同一 Wi-Fi / 热点</strong>
          <span>输入密码后上传</span>
        </div>
      </div>
    </header>
  );
};

export default FileEasyUploadHero;
