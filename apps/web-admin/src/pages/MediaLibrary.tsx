import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, Grid, Image, List, message, Popconfirm, Tag, Upload } from 'antd';
import { UploadOutlined, VideoCameraOutlined, DeleteOutlined } from '@ant-design/icons';
import { getMedia, uploadMedia, deleteMedia, Media } from '../api/media';
import type { UploadRequestOption } from 'rc-upload/lib/interface';

const MediaLibrary: React.FC = () => {
  const [mediaList, setMediaList] = useState<Media[]>([]);
  const [loading, setLoading] = useState(false);
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;

  const listColumns = useMemo(() => {
    if (screens.md) return 4;
    if (screens.sm) return 2;
    return 1;
  }, [screens.md, screens.sm]);

  const fetchMedia = async () => {
    setLoading(true);
    try {
      const data = await getMedia();
      setMediaList(data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMedia();
  }, []);

  const handleUpload = async (options: UploadRequestOption) => {
    const { file, onSuccess, onError } = options;
    try {
      await uploadMedia(file as File);
      // Removed message.success here to avoid multiple messages for batch upload
      onSuccess?.('ok');
      fetchMedia();
    } catch (err) {
      message.error(`${(file as File).name} 上传失败`);
      onError?.(err as Error);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteMedia(id);
      message.success('已删除');
      fetchMedia();
    } catch (err) {
      message.error('删除失败');
    }
  };

  return (
    <Card 
      title="素材库" 
      extra={
        <Upload 
          customRequest={handleUpload} 
          showUploadList={false} 
          multiple={true}
          onChange={(info) => {
            if (info.file.status === 'done') {
              // We can show a single success message when all are done, 
              // but customRequest is called per file.
            }
          }}
        >
          <Button type="primary" icon={<UploadOutlined />} block={isMobile}>
            批量上传
          </Button>
        </Upload>
      }
    >
      <List
        grid={{ gutter: 12, column: listColumns }}
        dataSource={mediaList}
        loading={loading}
        renderItem={(item) => (
          <List.Item>
            <Card
              hoverable
              actions={[
                <Popconfirm
                  key="delete"
                  title="确定删除该素材？"
                  onConfirm={() => handleDelete(item.id)}
                >
                  <DeleteOutlined />
                </Popconfirm>
              ]}
              cover={
                item.type === 'image' ? (
                  <Image 
                    src={item.url} 
                    height={isMobile ? 180 : 150} 
                    style={{ objectFit: 'cover' }} 
                  />
                ) : (
                  <div style={{ height: isMobile ? 180 : 150, background: '#000', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}>
                    <VideoCameraOutlined style={{ fontSize: 32 }} />
                  </div>
                )
              }
            >
              <Card.Meta 
                title={item.originalName} 
                description={
                  <>
                    <Tag color={item.type === 'image' ? 'blue' : 'orange'}>{item.type}</Tag>
                    {(item.size / 1024 / 1024).toFixed(2)} MB
                  </>
                } 
              />
            </Card>
          </List.Item>
        )}
      />
    </Card>
  );
};

export default MediaLibrary;
