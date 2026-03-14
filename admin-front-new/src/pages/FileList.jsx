import { useState, useRef } from 'react';
import { Button, Space, Tag, Modal, message, Upload, Image, Tooltip } from 'antd';
import {
  UploadOutlined, DeleteOutlined, EyeOutlined, DownloadOutlined,
  FileImageOutlined, FileTextOutlined, FilePdfOutlined, FileExcelOutlined,
  FileUnknownOutlined, CloudUploadOutlined,
} from '@ant-design/icons';
import ProTableV2 from '../components/ProTableV2.jsx';
import request from '../api/index.js';

const FILE_ICONS = {
  'image/jpeg': <FileImageOutlined style={{ color: '#f5222d' }} />,
  'image/png': <FileImageOutlined style={{ color: '#1890ff' }} />,
  'image/gif': <FileImageOutlined style={{ color: '#52c41a' }} />,
  'image/webp': <FileImageOutlined style={{ color: '#722ed1' }} />,
  'application/pdf': <FilePdfOutlined style={{ color: '#cf1322' }} />,
  'text/plain': <FileTextOutlined style={{ color: '#595959' }} />,
  'text/csv': <FileExcelOutlined style={{ color: '#389e0d' }} />,
};

function formatSize(bytes) {
  if (!bytes) return '-';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(1) + ' MB';
}

export default function FileList() {
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewUrl, setPreviewUrl] = useState('');
  const tableRef = useRef();

  const handleUpload = async (info) => {
    const file = info.file;
    if (!file) return;
    
    const formData = new FormData();
    formData.append('file', file);
    formData.append('module', 'attachment');
    
    try {
      const res = await request.post('/system/file/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      if (res.code === 200) {
        message.success('上传成功');
        tableRef.current?.reload?.();
      } else {
        message.error(res.message || '上传失败');
      }
    } catch (e) { message.error('上传失败'); }
  };

  const handlePreview = (record) => {
    if (record.mimeType?.startsWith('image/')) {
      setPreviewUrl(`/api/system/file/${record.id}`);
      setPreviewVisible(true);
    } else {
      window.open(`/api/system/file/${record.id}`, '_blank');
    }
  };

  const handleDelete = (record) => {
    Modal.confirm({
      title: '确认删除',
      content: `删除文件「${record.originalName}」？此操作不可恢复。`,
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          const res = await request.delete(`/system/file/${record.id}`);
          if (res.code === 200) { message.success('删除成功'); tableRef.current?.reload?.(); }
          else message.error(res.message);
        } catch (e) { message.error('删除失败'); }
      }
    });
  };

  const columns = [
    {
      title: '文件名', dataIndex: 'originalName', key: 'originalName', ellipsis: true,
      render: (v, record) => (
        <Space>
          {FILE_ICONS[record.mimeType] || <FileUnknownOutlined style={{ color: '#8c8c8c' }} />}
          <span>{v}</span>
        </Space>
      ),
    },
    {
      title: '类型', dataIndex: 'fileType', key: 'fileType', width: 80,
      hideInSearch: true,
      render: (v) => <Tag>{v?.toUpperCase()}</Tag>,
    },
    {
      title: '大小', dataIndex: 'fileSize', key: 'fileSize', width: 100,
      hideInSearch: true,
      render: formatSize,
    },
    {
      title: '模块', dataIndex: 'module', key: 'module', width: 100,
      hideInSearch: true,
      render: (v) => {
        const map = { avatar: { text: '头像', color: 'purple' }, attachment: { text: '附件', color: 'blue' }, export: { text: '导出', color: 'green' } };
        const item = map[v] || { text: v || '其他', color: 'default' };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '存储', dataIndex: 'storageType', key: 'storageType', width: 80,
      hideInSearch: true,
      render: (v) => <Tag color={v === 'oss' ? 'volcano' : 'cyan'}>{v === 'oss' ? 'OSS' : '本地'}</Tag>,
    },
    {
      title: '预览', key: 'preview', width: 80,
      hideInSearch: true,
      render: (_, record) => {
        if (record.mimeType?.startsWith('image/')) {
          return (
            <Image
              src={`/api/system/file/${record.id}`}
              width={40}
              height={40}
              style={{ objectFit: 'cover', borderRadius: 6 }}
              preview={{ mask: <EyeOutlined /> }}
            />
          );
        }
        return <span style={{ color: '#bfbfbf' }}>—</span>;
      },
    },
    {
      title: '上传时间', dataIndex: 'createTime', key: 'createTime', width: 170,
      hideInSearch: true,
      render: (v) => v?.replace('T', ' ')?.substring(0, 19),
    },
    {
      title: '操作', key: 'action', width: 140,
      hideInSearch: true,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="下载/预览">
            <Button size="small" type="link" icon={<DownloadOutlined />} onClick={() => handlePreview(record)} />
          </Tooltip>
          <Tooltip title="删除">
            <Button size="small" type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)} />
          </Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <>
      <ProTableV2
        ref={tableRef}
        headerTitle="文件管理"
        columns={columns}
        request={(params) => request.get('/system/file/list', { params })}
        rowKey="id"
        toolBarRender={() => [
          <Upload
            key="upload"
            showUploadList={false}
            beforeUpload={() => false}
            onChange={handleUpload}
            multiple
          >
            <Button type="primary" icon={<CloudUploadOutlined />}>上传文件</Button>
          </Upload>
        ]}
      />

      <Image
        style={{ display: 'none' }}
        preview={{
          visible: previewVisible,
          src: previewUrl,
          onVisibleChange: (v) => setPreviewVisible(v),
        }}
      />
    </>
  );
}
