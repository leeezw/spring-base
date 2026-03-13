import { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Space, Tag, Modal, message, Select, Tabs, Drawer, Form, Input, Switch, Descriptions, Empty, Spin, Tooltip, Badge } from 'antd';
import {
  ImportOutlined, EyeOutlined, DeleteOutlined, CodeOutlined, DatabaseOutlined,
  CopyOutlined, ReloadOutlined, SettingOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import './GenCode.css';

export default function GenCode() {
  const [dbTables, setDbTables] = useState([]);
  const [dbLoading, setDbLoading] = useState(false);
  const [genTables, setGenTables] = useState([]);
  const [genLoading, setGenLoading] = useState(false);
  const [importVisible, setImportVisible] = useState(false);
  const [selectedImportTable, setSelectedImportTable] = useState(null);
  const [importing, setImporting] = useState(false);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewData, setPreviewData] = useState({});
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewTable, setPreviewTable] = useState(null);
  const [configVisible, setConfigVisible] = useState(false);
  const [configData, setConfigData] = useState(null);
  const [configForm] = Form.useForm();

  const loadGenTables = useCallback(async () => {
    setGenLoading(true);
    try {
      const res = await request.get('/tool/gen/list');
      if (res.code === 200) setGenTables(res.data || []);
    } catch (e) { /* ignore */ }
    finally { setGenLoading(false); }
  }, []);

  const loadDbTables = useCallback(async () => {
    setDbLoading(true);
    try {
      const res = await request.get('/tool/gen/db/tables');
      if (res.code === 200) setDbTables(res.data || []);
    } catch (e) { /* ignore */ }
    finally { setDbLoading(false); }
  }, []);

  useEffect(() => { loadGenTables(); }, [loadGenTables]);

  const handleOpenImport = () => {
    loadDbTables();
    setSelectedImportTable(null);
    setImportVisible(true);
  };

  const handleImport = async () => {
    if (!selectedImportTable) { message.warning('请选择一张表'); return; }
    setImporting(true);
    try {
      const res = await request.post(`/tool/gen/import?tableName=${selectedImportTable}`);
      if (res.code === 200) {
        message.success(`导入成功：${res.data.className}`);
        setImportVisible(false);
        loadGenTables();
      } else {
        message.error(res.message);
      }
    } catch (e) { message.error(e.message); }
    finally { setImporting(false); }
  };

  const handlePreview = async (record) => {
    setPreviewTable(record);
    setPreviewLoading(true);
    setPreviewVisible(true);
    try {
      const res = await request.get(`/tool/gen/${record.id}/preview`);
      if (res.code === 200) setPreviewData(res.data || {});
      else message.error(res.message);
    } catch (e) { message.error('预览失败'); }
    finally { setPreviewLoading(false); }
  };

  const handleCopyCode = (code) => {
    navigator.clipboard.writeText(code).then(
      () => message.success('已复制到剪贴板'),
      () => message.error('复制失败')
    );
  };

  const handleDelete = (record) => {
    Modal.confirm({
      title: '确认删除',
      content: `删除「${record.tableName}」的生成配置？`,
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          const res = await request.delete(`/tool/gen/${record.id}`);
          if (res.code === 200) { message.success('删除成功'); loadGenTables(); }
          else message.error(res.message);
        } catch (e) { message.error('删除失败'); }
      }
    });
  };

  const handleOpenConfig = async (record) => {
    try {
      const res = await request.get(`/tool/gen/${record.id}`);
      if (res.code === 200) {
        setConfigData(res.data);
        configForm.setFieldsValue({
          className: res.data.className,
          packageName: res.data.packageName,
          moduleName: res.data.moduleName,
          businessName: res.data.businessName,
          functionName: res.data.functionName,
          author: res.data.author,
        });
        setConfigVisible(true);
      }
    } catch (e) { message.error('加载失败'); }
  };

  const handleSaveConfig = async (values) => {
    try {
      const res = await request.put('/tool/gen', { ...configData, ...values });
      if (res.code === 200) { message.success('配置已保存'); setConfigVisible(false); loadGenTables(); }
      else message.error(res.message);
    } catch (e) { message.error('保存失败'); }
  };

  // 已导入的表名集合
  const importedSet = new Set(genTables.map(t => t.tableName));

  const columns = [
    { title: '表名', dataIndex: 'tableName', key: 'tableName', render: (v) => <code>{v}</code> },
    { title: '注释', dataIndex: 'tableComment', key: 'tableComment', ellipsis: true },
    { title: '类名', dataIndex: 'className', key: 'className', render: (v) => <Tag color="blue">{v}</Tag> },
    { title: '业务名', dataIndex: 'businessName', key: 'businessName' },
    { title: '功能名', dataIndex: 'functionName', key: 'functionName', ellipsis: true },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180, render: (v) => v?.replace('T', ' ')?.substring(0, 19) },
    {
      title: '操作', key: 'action', width: 220,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="预览代码"><Button size="small" type="link" icon={<EyeOutlined />} onClick={() => handlePreview(record)}>预览</Button></Tooltip>
          <Tooltip title="编辑配置"><Button size="small" type="link" icon={<SettingOutlined />} onClick={() => handleOpenConfig(record)}>配置</Button></Tooltip>
          <Tooltip title="删除"><Button size="small" type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>删除</Button></Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <div className="gen-page">
      <Card
        title={<span><CodeOutlined style={{ color: '#667eea', marginRight: 8 }} />代码生成</span>}
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={loadGenTables}>刷新</Button>
            <Button type="primary" icon={<ImportOutlined />} onClick={handleOpenImport}>导入表</Button>
          </Space>
        }
      >
        <Table
          dataSource={genTables}
          columns={columns}
          rowKey="id"
          loading={genLoading}
          pagination={false}
          size="middle"
          locale={{ emptyText: <Empty description="暂无导入的表，点击「导入表」开始" /> }}
        />
      </Card>

      {/* 导入表弹窗 */}
      <Modal
        title={<span><DatabaseOutlined /> 选择数据库表</span>}
        open={importVisible}
        onCancel={() => setImportVisible(false)}
        onOk={handleImport}
        okText="导入"
        confirmLoading={importing}
        width={560}
      >
        <div style={{ marginBottom: 12, color: '#8c8c8c', fontSize: 13 }}>
          选择一张表导入，系统会自动解析列信息并生成配置。
        </div>
        <Select
          placeholder="搜索或选择表..."
          showSearch
          style={{ width: '100%' }}
          value={selectedImportTable}
          onChange={setSelectedImportTable}
          loading={dbLoading}
          optionFilterProp="label"
          options={dbTables.map(t => ({
            value: t.table_name,
            label: (
              <span>
                <code>{t.table_name}</code>
                {t.table_comment && <span style={{ color: '#8c8c8c', marginLeft: 8 }}>{t.table_comment}</span>}
                {importedSet.has(t.table_name) && <Tag color="orange" style={{ marginLeft: 8 }}>已导入</Tag>}
              </span>
            ),
            disabled: importedSet.has(t.table_name),
          }))}
        />
      </Modal>

      {/* 代码预览抽屉 */}
      <Drawer
        title={<span><CodeOutlined /> 代码预览 - {previewTable?.className}</span>}
        width={800}
        open={previewVisible}
        onClose={() => setPreviewVisible(false)}
        destroyOnClose
      >
        {previewLoading ? <Spin style={{ display: 'block', textAlign: 'center', marginTop: 100 }} /> : (
          Object.keys(previewData).length > 0 ? (
            <Tabs
              tabPosition="left"
              items={Object.entries(previewData).map(([name, code]) => ({
                key: name,
                label: <span style={{ fontSize: 12 }}>{name}</span>,
                children: (
                  <div className="code-preview">
                    <div className="code-header">
                      <span>{name}</span>
                      <Button size="small" icon={<CopyOutlined />} onClick={() => handleCopyCode(code)}>复制</Button>
                    </div>
                    <pre className="code-content">{code}</pre>
                  </div>
                ),
              }))}
            />
          ) : <Empty description="无预览数据" />
        )}
      </Drawer>

      {/* 配置编辑抽屉 */}
      <Drawer
        title="编辑生成配置"
        width={480}
        open={configVisible}
        onClose={() => setConfigVisible(false)}
        destroyOnClose
        extra={
          <Space>
            <Button onClick={() => setConfigVisible(false)}>取消</Button>
            <Button type="primary" onClick={() => configForm.submit()}>保存</Button>
          </Space>
        }
      >
        <Form form={configForm} layout="vertical" onFinish={handleSaveConfig}>
          <Form.Item name="className" label="实体类名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="packageName" label="包名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="moduleName" label="模块名">
            <Input />
          </Form.Item>
          <Form.Item name="businessName" label="业务名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="functionName" label="功能名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="author" label="作者">
            <Input />
          </Form.Item>
        </Form>

        {configData?.columns && (
          <>
            <div style={{ fontWeight: 600, margin: '16px 0 8px', fontSize: 14 }}>列配置</div>
            <Table
              dataSource={configData.columns}
              rowKey="id"
              size="small"
              pagination={false}
              scroll={{ y: 300 }}
              columns={[
                { title: '列名', dataIndex: 'columnName', width: 120, render: (v) => <code style={{ fontSize: 11 }}>{v}</code> },
                { title: 'Java字段', dataIndex: 'javaField', width: 110 },
                { title: '类型', dataIndex: 'javaType', width: 80, render: (v) => <Tag>{v}</Tag> },
                { title: '列表', dataIndex: 'isList', width: 50, render: (v) => v ? '✓' : '' },
                { title: '查询', dataIndex: 'isQuery', width: 50, render: (v) => v ? '✓' : '' },
                { title: 'HTML', dataIndex: 'htmlType', width: 80, render: (v) => <Tag color="cyan">{v}</Tag> },
              ]}
            />
          </>
        )}
      </Drawer>
    </div>
  );
}
