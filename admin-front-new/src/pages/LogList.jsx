import { useState, useEffect, useCallback } from 'react';
import { Tabs, Tag, Space, Button, Modal, message, Tooltip, Drawer, Descriptions, Badge, Table, Card } from 'antd';
import {
  FileTextOutlined, LoginOutlined, DeleteOutlined, EyeOutlined,
  CheckCircleOutlined, CloseCircleOutlined, ClearOutlined, UserOutlined, LogoutOutlined,
} from '@ant-design/icons';
import ProTableV2 from '../components/ProTableV2.jsx';
import request from '../api/index.js';
import { usePermission } from '../hooks/usePermission.jsx';

const OP_TYPE_MAP = {
  QUERY: { text: '查询', color: 'blue' },
  INSERT: { text: '新增', color: 'green' },
  UPDATE: { text: '修改', color: 'orange' },
  DELETE: { text: '删除', color: 'red' },
  IMPORT: { text: '导入', color: 'purple' },
  EXPORT: { text: '导出', color: 'cyan' },
  OTHER: { text: '其他', color: 'default' },
};

export default function LogList() {
  const { hasPermission } = usePermission();
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailRecord, setDetailRecord] = useState(null);
  const [onlineUsers, setOnlineUsers] = useState([]);
  const [onlineLoading, setOnlineLoading] = useState(false);

  const loadOnline = useCallback(async () => {
    setOnlineLoading(true);
    try {
      const res = await request.get('/system/log/online');
      if (res.code === 200) setOnlineUsers(res.data || []);
    } catch (e) { /* */ }
    finally { setOnlineLoading(false); }
  }, []);

  const handleForceLogout = (record) => {
    Modal.confirm({
      title: '强制下线', content: `确定将用户「${record.username}」强制下线吗？`,
      okButtonProps: { danger: true },
      onOk: async () => {
        const res = await request.delete(`/system/log/online/${record.userId}`);
        if (res.code === 200) { message.success('已下线'); loadOnline(); }
      }
    });
  };

  const handleCleanOperation = () => {
    Modal.confirm({
      title: '确认清空', content: '清空所有操作日志？此操作不可恢复。',
      okButtonProps: { danger: true },
      onOk: async () => {
        const res = await request.delete('/system/log/operation/clean');
        if (res.code === 200) message.success('已清空');
      }
    });
  };

  const handleCleanLogin = () => {
    Modal.confirm({
      title: '确认清空', content: '清空所有登录日志？',
      okButtonProps: { danger: true },
      onOk: async () => {
        const res = await request.delete('/system/log/login/clean');
        if (res.code === 200) message.success('已清空');
      }
    });
  };

  const operationColumns = [
    { title: '模块', dataIndex: 'module', width: 100, render: (v) => <Tag>{v}</Tag> },
    { title: '类型', dataIndex: 'operationType', width: 80,
      valueType: 'select', valueEnum: Object.fromEntries(Object.entries(OP_TYPE_MAP).map(([k, v]) => [k, { text: v.text }])),
      render: (_, r) => { const m = OP_TYPE_MAP[r.operationType] || OP_TYPE_MAP.OTHER; return <Tag color={m.color}>{m.text}</Tag>; }
    },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    { title: '操作人', dataIndex: 'username', width: 100 },
    { title: 'IP', dataIndex: 'ip', width: 130, hideInSearch: true },
    { title: '状态', dataIndex: 'status', width: 80,
      valueType: 'select', valueEnum: { 1: { text: '成功', status: 'Success' }, 0: { text: '失败', status: 'Error' } },
      render: (_, r) => r.status === 1
        ? <Badge status="success" text="成功" />
        : <Badge status="error" text="失败" />
    },
    { title: '耗时', dataIndex: 'duration', width: 80, hideInSearch: true, render: (v) => v ? `${v}ms` : '-' },
    { title: '时间', dataIndex: 'createTime', width: 170, hideInSearch: true, render: (v) => v?.replace('T', ' ')?.substring(0, 19) },
    {
      title: '操作', width: 60, hideInSearch: true,
      render: (_, record) => (
        <Tooltip title="详情">
          <Button size="small" type="link" icon={<EyeOutlined />} onClick={() => { setDetailRecord(record); setDetailVisible(true); }} />
        </Tooltip>
      ),
    },
  ];

  const loginColumns = [
    { title: '用户名', dataIndex: 'username', width: 120 },
    { title: 'IP地址', dataIndex: 'ip', width: 140, hideInSearch: true },
    { title: '状态', dataIndex: 'status', width: 100,
      valueType: 'select', valueEnum: { 1: { text: '成功', status: 'Success' }, 0: { text: '失败', status: 'Error' } },
      render: (_, r) => r.status === 1
        ? <Space><CheckCircleOutlined style={{ color: '#52c41a' }} />成功</Space>
        : <Space><CloseCircleOutlined style={{ color: '#ff4d4f' }} />失败</Space>
    },
    { title: '消息', dataIndex: 'message', ellipsis: true, hideInSearch: true },
    { title: '浏览器', dataIndex: 'userAgent', ellipsis: true, width: 200, hideInSearch: true,
      render: (v) => { if (!v) return '-'; try { const m = v.match(/(Chrome|Firefox|Safari|Edge|Opera)\/[\d.]+/); return m ? m[0] : v.substring(0, 40); } catch(e) { return '-'; } }
    },
    { title: '登录时间', dataIndex: 'loginTime', width: 170, hideInSearch: true, render: (v) => v?.replace('T', ' ')?.substring(0, 19) },
  ];

  return (
    <>
      <Tabs defaultActiveKey="operation" onChange={(key) => { if (key === 'online') loadOnline(); }} items={[
        {
          key: 'operation',
          label: <span><FileTextOutlined /> 操作日志</span>,
          children: (
            <ProTableV2
              headerTitle="操作日志"
              columns={operationColumns}
              request={(params) => request.get('/system/log/operation', { params })}
              rowKey="id"
              toolBarRender={() => [
                hasPermission('system:log:delete') && <Button key="clean" danger icon={<ClearOutlined />} onClick={handleCleanOperation}>清空</Button>
              ].filter(Boolean)}
            />
          ),
        },
        {
          key: 'login',
          label: <span><LoginOutlined /> 登录日志</span>,
          children: (
            <ProTableV2
              headerTitle="登录日志"
              columns={loginColumns}
              request={(params) => request.get('/system/log/login', { params })}
              rowKey="id"
              toolBarRender={() => [
                hasPermission('system:log:delete') && <Button key="clean" danger icon={<ClearOutlined />} onClick={handleCleanLogin}>清空</Button>
              ].filter(Boolean)}
            />
          ),
        },
        {
          key: 'online',
          label: <span><UserOutlined /> 在线用户</span>,
          children: (
            <Card title={`在线用户 (${onlineUsers.length})`} extra={<Button onClick={loadOnline}>刷新</Button>}>
              <Table
                dataSource={onlineUsers}
                rowKey="userId"
                loading={onlineLoading}
                pagination={false}
                size="middle"
                columns={[
                  { title: '用户ID', dataIndex: 'userId', width: 80 },
                  { title: '用户名', dataIndex: 'username', width: 120 },
                  { title: '昵称', dataIndex: 'nickname', width: 120 },
                  { title: '租户', dataIndex: 'tenantId', width: 80, render: (v) => <Tag>{v === 0 ? '平台' : `#${v}`}</Tag> },
                  { title: '剩余有效期', dataIndex: 'ttl', width: 120, render: (v) => v > 0 ? `${Math.floor(v / 60)}分钟` : '-' },
                  { title: '操作', width: 100, render: (_, record) => (
                    hasPermission('system:log:force-logout') && <Button size="small" danger icon={<LogoutOutlined />} onClick={() => handleForceLogout(record)}>强制下线</Button>
                  )},
                ]}
              />
            </Card>
          ),
        },
      ]} />

      {/* 操作日志详情抽屉 */}
      <Drawer title="操作日志详情" width={560} open={detailVisible} onClose={() => setDetailVisible(false)} destroyOnClose>
        {detailRecord && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="模块">{detailRecord.module}</Descriptions.Item>
            <Descriptions.Item label="操作类型"><Tag color={(OP_TYPE_MAP[detailRecord.operationType] || {}).color}>{(OP_TYPE_MAP[detailRecord.operationType] || {}).text}</Tag></Descriptions.Item>
            <Descriptions.Item label="描述">{detailRecord.description}</Descriptions.Item>
            <Descriptions.Item label="方法"><code style={{ fontSize: 11 }}>{detailRecord.method}</code></Descriptions.Item>
            <Descriptions.Item label="请求URL">{detailRecord.requestUrl}</Descriptions.Item>
            <Descriptions.Item label="操作人">{detailRecord.username}</Descriptions.Item>
            <Descriptions.Item label="IP">{detailRecord.ip}</Descriptions.Item>
            <Descriptions.Item label="状态">{detailRecord.status === 1 ? <Badge status="success" text="成功" /> : <Badge status="error" text="失败" />}</Descriptions.Item>
            <Descriptions.Item label="耗时">{detailRecord.duration}ms</Descriptions.Item>
            <Descriptions.Item label="时间">{detailRecord.createTime?.replace('T', ' ')?.substring(0, 19)}</Descriptions.Item>
            {detailRecord.requestParams && (
              <Descriptions.Item label="请求参数"><pre style={{ maxHeight: 200, overflow: 'auto', fontSize: 11, margin: 0 }}>{detailRecord.requestParams}</pre></Descriptions.Item>
            )}
            {detailRecord.responseData && (
              <Descriptions.Item label="响应数据"><pre style={{ maxHeight: 200, overflow: 'auto', fontSize: 11, margin: 0 }}>{detailRecord.responseData}</pre></Descriptions.Item>
            )}
            {detailRecord.errorMsg && (
              <Descriptions.Item label="错误信息"><span style={{ color: '#ff4d4f' }}>{detailRecord.errorMsg}</span></Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Drawer>
    </>
  );
}
