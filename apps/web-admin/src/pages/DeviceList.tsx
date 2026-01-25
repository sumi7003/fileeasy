import React, { useEffect, useState } from 'react';
import { Button, Card, Grid, message, Modal, Select, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ReloadOutlined, DesktopOutlined, PlaySquareOutlined } from '@ant-design/icons';
import { getDevices, Device } from '../api/device';
import { getPlaylists, assignPlaylists, Playlist } from '../api/playlist';
import dayjs from 'dayjs';

const DeviceList: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Device[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedDeviceIds, setSelectedDeviceIds] = useState<string[]>([]);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [selectedPlaylistIds, setSelectedPlaylistIds] = useState<string[]>([]);
  const [batchMode, setBatchMode] = useState(false);
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;

  const fetchData = async () => {
    setLoading(true);
    try {
      const devices = await getDevices();
      setData(devices);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const fetchPlaylists = async () => {
    const list = await getPlaylists();
    setPlaylists(list);
  };

  useEffect(() => {
    fetchData();
    fetchPlaylists();
    const timer = setInterval(fetchData, 10000);
    return () => clearInterval(timer);
  }, []);

  const handleAssign = async () => {
    if (selectedDeviceIds.length > 0) {
      try {
        setLoading(true);
        // 批量设置清单
        await Promise.all(selectedDeviceIds.map(id => assignPlaylists(id, selectedPlaylistIds)));
        message.success(`已为 ${selectedDeviceIds.length} 台设备下发清单`);
        setIsModalOpen(false);
        setSelectedDeviceIds([]);
        fetchData();
      } catch (e) {
        message.error('下发失败');
      } finally {
        setLoading(false);
      }
    }
  };

  const columns: ColumnsType<Device> = [
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const color = status === 'online' ? 'green' : 'red';
        return <Tag color={color}>{status.toUpperCase()}</Tag>;
      },
    },
    {
      title: '设备名称',
      dataIndex: 'name',
      key: 'name',
      render: (text) => (
        <Space>
          <DesktopOutlined /> {text}
        </Space>
      ),
    },
    {
      title: '当前播放',
      dataIndex: 'playlists',
      key: 'playlists',
      render: (playlists: Playlist[]) => 
        playlists && playlists.length > 0 ? (
          <Space size={[0, 4]} wrap>
            {playlists.map(p => <Tag color="blue" key={p.id}>{p.name}</Tag>)}
          </Space>
        ) : <Tag>无</Tag>,
    },
    {
      title: '最后心跳',
      dataIndex: 'lastHeartbeat',
      key: 'lastHeartbeat',
      render: (date) => date ? dayjs(date).format('HH:mm:ss') : '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button 
          type="link" 
          icon={<PlaySquareOutlined />}
          onClick={() => {
            setSelectedDeviceIds([record.id]);
            setSelectedPlaylistIds(record.playlists?.map(p => p.id) || []);
            setBatchMode(false);
            setIsModalOpen(true);
          }}
        >
          分配节目
        </Button>
      ),
    },
  ];

  const rowSelection = {
    selectedRowKeys: selectedDeviceIds,
    onChange: (keys: React.Key[]) => {
      setSelectedDeviceIds(keys as string[]);
    },
  };

  return (
    <Card 
      title="设备管理" 
      extra={
        <Space>
          {selectedDeviceIds.length > 0 && (
            <Button 
              type="primary" 
              danger
              onClick={() => {
                setSelectedPlaylistIds([]);
                setBatchMode(true);
                setIsModalOpen(true);
              }}
            >
              批量分配 ({selectedDeviceIds.length})
            </Button>
          )}
          <Button 
            type="primary" 
            icon={<ReloadOutlined />} 
            onClick={fetchData} 
            loading={loading}
            block={isMobile}
          >
            刷新
          </Button>
        </Space>
      }
    >
      <Table 
        rowSelection={rowSelection}
        columns={columns} 
        dataSource={data} 
        rowKey="id" 
        loading={loading} 
        size={isMobile ? 'small' : 'middle'}
        pagination={{ pageSize: isMobile ? 5 : 10 }} 
        scroll={isMobile ? { x: 720 } : undefined}
      />

      <Modal 
        title={batchMode ? "批量分配播放列表" : "分配播放列表"} 
        open={isModalOpen} 
        onOk={handleAssign} 
        confirmLoading={loading}
        onCancel={() => setIsModalOpen(false)}
      >
        <p>
          {batchMode 
            ? `正在为选中的 ${selectedDeviceIds.length} 台设备统一分配播放列表：` 
            : "请选择要下发给设备的播放列表（支持多个，将轮流播放）："}
        </p>
        <Select
          mode="multiple"
          style={{ width: '100%' }}
          placeholder="选择播放列表"
          value={selectedPlaylistIds}
          onChange={setSelectedPlaylistIds}
          options={playlists.map(p => ({ label: p.name, value: p.id }))}
        />
      </Modal>
    </Card>
  );
};

export default DeviceList;
