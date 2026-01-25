import React, { useEffect, useState } from 'react';
import { Button, Card, Form, Grid, Input, InputNumber, message, Modal, Popconfirm, Select, Table } from 'antd';
import { PlusOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';
import { getPlaylists, createPlaylist, updatePlaylist, deletePlaylist, Playlist } from '../api/playlist';
import { getMedia, Media } from '../api/media';
import { TimePicker, Checkbox, Space } from 'antd';
import dayjs from 'dayjs';

const daysOptions = [
  { label: '周一', value: '1' },
  { label: '周二', value: '2' },
  { label: '周三', value: '3' },
  { label: '周四', value: '4' },
  { label: '周五', value: '5' },
  { label: '周六', value: '6' },
  { label: '周日', value: '7' },
];

const PlaylistManager: React.FC = () => {
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [mediaList, setMediaList] = useState<Media[]>([]);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingPlaylist, setEditingPlaylist] = useState<Playlist | null>(null);
  const [form] = Form.useForm();
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  
  // New playlist state
  const [selectedMediaItems, setSelectedMediaItems] = useState<{mediaId: string, duration: number}[]>([]);

  const fetchData = async () => {
    const [plData, mediaData] = await Promise.all([getPlaylists(), getMedia()]);
    setPlaylists(plData);
    setMediaList(mediaData);
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSubmit = async (values: any) => {
    if (selectedMediaItems.length === 0) {
      message.error('请至少添加一个素材');
      return;
    }

    const items = selectedMediaItems.map((item, index) => ({
      mediaId: item.mediaId,
      duration: item.duration,
      order: index + 1,
    }));

    const data = {
      name: values.name,
      description: values.description,
      startTime: values.timeRange ? values.timeRange[0].format('HH:mm:ss') : undefined,
      endTime: values.timeRange ? values.timeRange[1].format('HH:mm:ss') : undefined,
      daysOfWeek: values.daysOfWeek ? values.daysOfWeek.join(',') : '1,2,3,4,5,6,7',
      items,
    };

    try {
      if (editingPlaylist) {
        await updatePlaylist(editingPlaylist.id, data);
        message.success('更新成功');
      } else {
        await createPlaylist(data);
        message.success('创建成功');
      }
      handleCloseModal();
      fetchData();
    } catch (error) {
      message.error(editingPlaylist ? '更新失败' : '创建失败');
    }
  };

  const handleEdit = (playlist: Playlist) => {
    setEditingPlaylist(playlist);
    
    // 设置表单值
    form.setFieldsValue({
      name: playlist.name,
      description: playlist.description,
      timeRange: playlist.startTime && playlist.endTime 
        ? [dayjs(playlist.startTime, 'HH:mm:ss'), dayjs(playlist.endTime, 'HH:mm:ss')]
        : undefined,
      daysOfWeek: playlist.daysOfWeek ? playlist.daysOfWeek.split(',') : ['1', '2', '3', '4', '5', '6', '7'],
    });
    
    // 设置已选素材
    const items = playlist.items.map(item => ({
      mediaId: item.media.id,
      duration: item.duration,
    }));
    setSelectedMediaItems(items);
    
    setIsModalVisible(true);
  };

  const handleCloseModal = () => {
    setIsModalVisible(false);
    setEditingPlaylist(null);
    form.resetFields();
    setSelectedMediaItems([]);
  };

  const handleOpenCreate = () => {
    setEditingPlaylist(null);
    form.resetFields();
    setSelectedMediaItems([]);
    setIsModalVisible(true);
  };

  const handleDeletePlaylist = async (id: string) => {
    try {
      await deletePlaylist(id);
      message.success('已删除');
      fetchData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  return (
    <Card 
      title="播放列表管理" 
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={handleOpenCreate} block={isMobile}>
          新建列表
        </Button>
      }
    >
      <Table 
        dataSource={playlists} 
        rowKey="id"
        size={isMobile ? 'small' : 'middle'}
        scroll={isMobile ? { x: 760 } : undefined}
        columns={[
          { title: '名称', dataIndex: 'name' },
          { title: '素材数', render: (_, record) => record.items.length },
          { 
            title: '排期', 
            render: (_, record) => (
              <span>
                {record.startTime ? `${record.startTime} - ${record.endTime}` : '全天'}
                {record.daysOfWeek && <div style={{ fontSize: '12px', color: '#999' }}>周: {record.daysOfWeek}</div>}
              </span>
            )
          },
          { title: '创建时间', dataIndex: 'createdAt', render: (date) => dayjs(date).format('YYYY-MM-DD HH:mm') },
          {
            title: '操作',
            render: (_, record) => (
              <Space>
                <Button icon={<EditOutlined />} onClick={() => handleEdit(record)}>
                  编辑
                </Button>
                <Popconfirm
                  title="确定删除该播放列表？"
                  onConfirm={() => handleDeletePlaylist(record.id)}
                >
                  <Button danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            )
          }
        ]} 
      />

      <Modal 
        title={editingPlaylist ? '编辑播放列表' : '新建播放列表'} 
        open={isModalVisible} 
        onOk={() => form.submit()} 
        onCancel={handleCloseModal}
        okText={editingPlaylist ? '保存' : '创建'}
        width={isMobile ? '100%' : 800}
        style={isMobile ? { top: 16 } : undefined}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="name" label="列表名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>

          <Form.Item name="timeRange" label="播放时段 (留空为全天)">
            <TimePicker.RangePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="daysOfWeek" label="播放日期" initialValue={['1', '2', '3', '4', '5', '6', '7']}>
            <Checkbox.Group options={daysOptions} />
          </Form.Item>
          
          <div style={{ marginBottom: 16 }}>
            <h4>选择素材:</h4>
            <Select 
              mode="multiple"
              style={{ width: '100%' }} 
              placeholder="选择素材加入列表 (可多选)"
              value={selectedMediaItems.map(item => item.mediaId)}
              onChange={(ids: string[]) => {
                // Map new IDs to items, preserving existing durations if possible
                const newItems = ids.map(id => {
                  const existing = selectedMediaItems.find(item => item.mediaId === id);
                  if (existing) return existing;
                  
                  const media = mediaList.find(m => m.id === id);
                  return {
                    mediaId: id,
                    duration: media?.type === 'video' ? 30 : 10
                  };
                });
                setSelectedMediaItems(newItems);
              }}
              options={mediaList.map(m => ({ label: m.originalName, value: m.id }))}
            />
          </div>

          <div style={{ border: '1px solid #eee', padding: 16, borderRadius: 4 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <h4 style={{ margin: 0 }}>已选素材 (按顺序播放):</h4>
              {selectedMediaItems.length > 0 && (
                <Button size="small" onClick={() => setSelectedMediaItems([])}>清空全部</Button>
              )}
            </div>
            {selectedMediaItems.map((item, idx) => {
              const media = mediaList.find(m => m.id === item.mediaId);
              return (
                <div key={idx} style={{ display: 'flex', marginBottom: 8, alignItems: 'center', gap: 8 }}>
                  <span style={{ width: 20 }}>{idx + 1}.</span>
                  <span style={{ flex: 1 }}>{media?.originalName}</span>
                  <span>{media?.type === 'video' ? '视频(自动播放)' : '展示时长(秒):'}</span>
                  {media?.type !== 'video' && (
                    <InputNumber 
                      value={item.duration} 
                      onChange={val => {
                        const newItems = [...selectedMediaItems];
                        newItems[idx].duration = val || 10;
                        setSelectedMediaItems(newItems);
                      }} 
                    />
                  )}
                  <Button 
                    danger 
                    icon={<DeleteOutlined />} 
                    onClick={() => {
                      const newItems = [...selectedMediaItems];
                      newItems.splice(idx, 1);
                      setSelectedMediaItems(newItems);
                    }}
                  />
                </div>
              );
            })}
          </div>
        </Form>
      </Modal>
    </Card>
  );
};

export default PlaylistManager;

