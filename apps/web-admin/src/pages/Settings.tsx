import React, { useEffect, useState } from 'react';
import { Button, Card, Divider, Grid, message, Space, Typography, Upload } from 'antd';
import { UploadOutlined, CloudDownloadOutlined } from '@ant-design/icons';
import { uploadApk, getUpdateInfo, UpdateInfo } from '../api/update';
import type { UploadRequestOption } from 'rc-upload/lib/interface';

const { Title, Text } = Typography;

const Settings: React.FC = () => {
  const [updateInfo, setUpdateInfo] = useState<UpdateInfo | null>(null);
  const [loading, setLoading] = useState(false);
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;

  const fetchUpdateInfo = async () => {
    try {
      const info = await getUpdateInfo();
      setUpdateInfo(info);
    } catch (error) {
      console.error('Failed to fetch update info:', error);
    }
  };

  useEffect(() => {
    fetchUpdateInfo();
  }, []);

  const handleUpload = async (options: UploadRequestOption) => {
    const { file, onSuccess, onError } = options;
    setLoading(true);
    try {
      await uploadApk(file as File);
      message.success('APK 上传成功，局域网终端将收到更新通知');
      onSuccess?.('ok');
      fetchUpdateInfo();
    } catch (err) {
      message.error(`上传失败: ${err}`);
      onError?.(err as Error);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = () => {
    window.open('/api/v1/update/download', '_blank');
  };

  const handleDownloadCurrent = () => {
    window.open('/api/v1/system/app-apk', '_blank');
  };

  return (
    <div style={{ maxWidth: isMobile ? '100%' : 800, margin: '0 auto', padding: isMobile ? '0 4px' : 0 }}>
      <Title level={isMobile ? 3 : 2}>系统设置</Title>

      <Card title="当前版本下载" style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Text>下载当前正在运行的安装包 (用于安装到其他终端)</Text>
          </div>
          <Button 
            type="primary"
            icon={<CloudDownloadOutlined />} 
            onClick={handleDownloadCurrent}
          >
            下载 APK
          </Button>
        </div>
      </Card>
      
      <Card title="APK 在线升级" extra={<CloudDownloadOutlined />}>
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <Text strong>当前分发版本：</Text>
              {updateInfo?.hasUpdate ? (
                <Text>{updateInfo.versionName} (Build {updateInfo.versionCode})</Text>
              ) : (
                <Text type="secondary">未上传更新包</Text>
              )}
            </div>
            {updateInfo?.hasUpdate && (
              <Button 
                type="link" 
                icon={<CloudDownloadOutlined />} 
                onClick={handleDownload}
              >
                下载当前 APK
              </Button>
            )}
          </div>

          <Divider />

          <div>
            <Title level={4}>上传新版本 APK</Title>
            <div style={{ color: 'rgba(0, 0, 0, 0.45)', marginBottom: 16 }}>
              上传后的 APK 将自动分发给局域网内所有 Xplay 终端。终端在下次心跳时会自动下载并提示安装。
            </div>
            
            <Upload 
              customRequest={handleUpload} 
              showUploadList={false}
              accept=".apk"
            >
              <Button type="primary" icon={<UploadOutlined />} loading={loading} block={isMobile}>
                选择并上传 APK 文件
              </Button>
            </Upload>
          </div>
        </Space>
      </Card>
    </div>
  );
};

export default Settings;
