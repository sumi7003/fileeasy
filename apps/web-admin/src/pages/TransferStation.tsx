import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Grid,
  Image,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import { DeleteOutlined, LinkOutlined, QrcodeOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons';
import type { UploadRequestOption } from 'rc-upload/lib/interface';
import dayjs from 'dayjs';
import {
  deleteTransferFile,
  getTransferFiles,
  getTransferLogs,
  getTransferQrUrl,
  getTransferStorage,
  getTransferShare,
  uploadTransferFile,
  TransferFile,
  TransferLog,
  TransferStorageStatus,
} from '../api/transfer';

const { Text } = Typography;

const MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024; // 2GB

const formatSize = (size: number) => {
  if (!size && size !== 0) return '-';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let value = size;
  let idx = 0;
  while (value >= 1024 && idx < units.length - 1) {
    value /= 1024;
    idx += 1;
  }
  return `${idx === 0 ? value : value.toFixed(2)} ${units[idx]}`;
};

const TransferStation: React.FC = () => {
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const [files, setFiles] = useState<TransferFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [qrVisible, setQrVisible] = useState(false);
  const [qrUrl, setQrUrl] = useState('');
  const [logVisible, setLogVisible] = useState(false);
  const [logs, setLogs] = useState<TransferLog[]>([]);
  const [logTitle, setLogTitle] = useState('');
  const [storageStatus, setStorageStatus] = useState<TransferStorageStatus | null>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  const fetchFiles = async () => {
    setLoading(true);
    try {
      const data = await getTransferFiles();
      setFiles(data);
      const storage = await getTransferStorage();
      setStorageStatus(storage);
    } catch (err) {
      message.error('获取文件列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFiles();
  }, []);

  const handleUpload = async (options: UploadRequestOption) => {
    const { file, onSuccess, onError, onProgress } = options;
    try {
      const data = await uploadTransferFile(file as File, (percent) => {
        onProgress?.({ percent });
      });
      message.success(`${data.originalName} 上传成功`);
      onSuccess?.('ok');
      fetchFiles();
    } catch (err) {
      message.error(`${(file as File).name} 上传失败`);
      onError?.(err as Error);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteTransferFile(id);
      message.success('已删除');
      fetchFiles();
    } catch (err) {
      message.error('删除失败');
    }
  };

  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) return;
    setLoading(true);
    try {
      await Promise.all(selectedRowKeys.map((id) => deleteTransferFile(String(id))));
      message.success(`已删除 ${selectedRowKeys.length} 个文件`);
      setSelectedRowKeys([]);
      fetchFiles();
    } catch (err) {
      message.error('批量删除失败');
    } finally {
      setLoading(false);
    }
  };

  const beforeUpload = (file: File) => {
    if (storageStatus?.blocked) {
      message.error('存储空间不足，已禁止上传');
      return Upload.LIST_IGNORE;
    }
    if (file.name.length > 255) {
      message.error('文件名长度不能超过 255 字符');
      return Upload.LIST_IGNORE;
    }
    if (file.size > MAX_FILE_SIZE) {
      message.error('单文件大小不能超过 2GB');
      return Upload.LIST_IGNORE;
    }
    return true;
  };

  const openShareLink = async (record: TransferFile) => {
    try {
      if (record.shareUrl) {
        window.open(record.shareUrl, '_blank');
        return;
      }
      const data = await getTransferShare(record.id);
      window.open(data.shareUrl, '_blank');
    } catch (err) {
      message.error('获取分享链接失败');
    }
  };

  const copyShareLink = async (record: TransferFile) => {
    try {
      const shareUrl = record.shareUrl ?? (await getTransferShare(record.id)).shareUrl;
      await navigator.clipboard.writeText(shareUrl);
      message.success('分享链接已复制');
    } catch (err) {
      message.error('复制失败');
    }
  };

  const showQr = (record: TransferFile) => {
    setQrUrl(getTransferQrUrl(record.id));
    setQrVisible(true);
  };

  const showLogs = async (record: TransferFile) => {
    setLogTitle(record.originalName);
    setLogVisible(true);
    try {
      const data = await getTransferLogs(record.id);
      setLogs(data);
    } catch (err) {
      message.error('获取日志失败');
    }
  };

  const filtered = useMemo(() => {
    if (!search) return files;
    const keyword = search.trim().toLowerCase();
    return files.filter((item) => item.originalName.toLowerCase().includes(keyword));
  }, [files, search]);

  const columns = useMemo(
    () => [
      {
        title: '文件名',
        dataIndex: 'originalName',
        key: 'originalName',
        ellipsis: true,
        render: (text: string, record: TransferFile) => (
          <Space direction="vertical" size={2}>
            <Text strong>{text}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>
              ID: {record.id}
            </Text>
          </Space>
        ),
      },
      {
        title: '大小',
        dataIndex: 'size',
        key: 'size',
        width: 120,
        sorter: (a: TransferFile, b: TransferFile) => a.size - b.size,
        render: (size: number) => formatSize(size),
      },
      {
        title: '上传时间',
        dataIndex: 'createdAt',
        key: 'createdAt',
        width: 180,
        sorter: (a: TransferFile, b: TransferFile) => a.createdAt - b.createdAt,
        render: (value: number) => dayjs(value).format('YYYY-MM-DD HH:mm:ss'),
      },
      {
        title: '上传者',
        dataIndex: 'uploaderIp',
        key: 'uploaderIp',
        width: 140,
        render: (value?: string) => value || '-',
      },
      {
        title: '下载次数',
        dataIndex: 'downloadCount',
        key: 'downloadCount',
        width: 100,
        sorter: (a: TransferFile, b: TransferFile) => a.downloadCount - b.downloadCount,
      },
      {
        title: '剩余天数',
        dataIndex: 'remainingDays',
        key: 'remainingDays',
        width: 120,
        sorter: (a: TransferFile, b: TransferFile) => a.remainingDays - b.remainingDays,
        render: (value: number) => (
          <Tag color={value <= 7 ? 'orange' : 'blue'}>{`剩余 ${value} 天`}</Tag>
        ),
      },
      {
        title: '操作',
        key: 'actions',
        width: isMobile ? 180 : 260,
        render: (_: unknown, record: TransferFile) => (
          <Space wrap>
            <Button size="small" icon={<LinkOutlined />} onClick={() => openShareLink(record)}>
              打开
            </Button>
            <Button size="small" onClick={() => copyShareLink(record)}>
              复制链接
            </Button>
            <Button size="small" icon={<QrcodeOutlined />} onClick={() => showQr(record)}>
              二维码
            </Button>
            <Button size="small" onClick={() => showLogs(record)}>
              日志
            </Button>
            <Popconfirm title="确定删除该文件？" onConfirm={() => handleDelete(record.id)}>
              <Button size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          </Space>
        ),
      },
    ],
    [isMobile]
  );

  return (
    <Card
      title="文件中转"
      extra={
        <Space wrap>
          <Input
            allowClear
            placeholder="搜索文件名"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: isMobile ? 160 : 220 }}
          />
          <Upload
            customRequest={handleUpload}
            showUploadList={false}
            multiple={true}
            beforeUpload={beforeUpload}
          >
            <Button type="primary" icon={<UploadOutlined />} block={isMobile}>
              上传文件（单文件 2GB 上限）
            </Button>
          </Upload>
          <Popconfirm title="确定删除选中的文件？" onConfirm={handleBatchDelete} disabled={selectedRowKeys.length === 0}>
            <Button danger disabled={selectedRowKeys.length === 0}>
              批量删除
            </Button>
          </Popconfirm>
          <Button icon={<ReloadOutlined />} onClick={fetchFiles} />
        </Space>
      }
    >
      {storageStatus?.warn && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="存储空间紧张"
          description={`当前已用 ${storageStatus.usedMB}MB，剩余 ${storageStatus.freeMB}MB。请及时清理或释放空间。`}
        />
      )}
      <Upload.Dragger
        customRequest={handleUpload}
        showUploadList={{ showRemoveIcon: false }}
        multiple={true}
        beforeUpload={beforeUpload}
        style={{ marginBottom: 16 }}
      >
        <p className="ant-upload-drag-icon">
          <UploadOutlined />
        </p>
        <p className="ant-upload-text">拖拽文件到此处上传</p>
        <p className="ant-upload-hint">支持多文件，单文件最大 2GB</p>
      </Upload.Dragger>
      <Table
        rowKey="id"
        dataSource={filtered}
        columns={columns as any}
        loading={loading}
        pagination={{ pageSize: 20 }}
        scroll={isMobile ? { x: 900 } : { x: 1200 }}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys),
        }}
      />

      <Modal
        open={qrVisible}
        onCancel={() => setQrVisible(false)}
        title="二维码"
        footer={
          <Button type="primary" onClick={() => window.open(qrUrl, '_blank')}>
            下载二维码
          </Button>
        }
      >
        <div style={{ textAlign: 'center' }}>
          <Image src={qrUrl} width={260} preview={false} />
          <div style={{ marginTop: 12 }}>
            <Text type="secondary">扫码打开下载页面</Text>
          </div>
        </div>
      </Modal>

      <Modal
        open={logVisible}
        onCancel={() => setLogVisible(false)}
        footer={null}
        title={`操作日志 - ${logTitle}`}
        width={isMobile ? '100%' : 680}
      >
        <Table
          rowKey="id"
          dataSource={logs}
          pagination={{ pageSize: 8 }}
          columns={[
            { title: '时间', dataIndex: 'time', render: (v: number) => dayjs(v).format('YYYY-MM-DD HH:mm:ss') },
            { title: '动作', dataIndex: 'action' },
            { title: 'IP', dataIndex: 'ip' },
            { title: '备注', dataIndex: 'clientRemark' },
            { title: '结果', dataIndex: 'result' },
          ]}
          size="small"
        />
      </Modal>
    </Card>
  );
};

export default TransferStation;
