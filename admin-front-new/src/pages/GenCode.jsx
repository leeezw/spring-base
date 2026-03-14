import { useState, useEffect, useCallback } from 'react';
import { Card, Table, Button, Space, Tag, Modal, message, Select, Tabs, Drawer, Form, Input, Switch, Tooltip, Spin, Empty, InputNumber, Popconfirm } from 'antd';
import {
  ImportOutlined, EyeOutlined, DeleteOutlined, CodeOutlined, DatabaseOutlined,
  CopyOutlined, ReloadOutlined, SettingOutlined, DownloadOutlined, RocketOutlined,
  SyncOutlined, CheckCircleOutlined,
} from '@ant-design/icons';
import request from '../api/index.js';
import { usePermission } from '../hooks/usePermission.jsx';
import './GenCode.css';

const QUERY_TYPES = [
  { value: 'EQ', label: '=' },
  { value: 'NE', label: '!=' },
  { value: 'LIKE', label: 'LIKE' },
  { value: 'GT', label: '>' },
  { value: 'GE', label: '>=' },
  { value: 'LT', label: '<' },
  { value: 'LE', label: '<=' },
  { value: 'BETWEEN', label: 'BETWEEN' },
];

const HTML_TYPES = [
  { value: 'input', label: '文本框' },
  { value: 'textarea', label: '文本域' },
  { value: 'inputNumber', label: '数字框' },
  { value: 'select', label: '下拉框' },
  { value: 'switch', label: '开关' },
  { value: 'datetime', label: '日期选择' },
  { value: 'imageUpload', label: '图片上传' },
  { value: 'fileUpload', label: '文件上传' },
  { value: 'editor', label: '富文本' },
];

const JAVA_TYPES = ['String', 'Integer', 'Long', 'Double', 'Boolean', 'LocalDateTime', 'LocalDate'];

export default function GenCode() {
  const { hasPermission } = usePermission();
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
  const [configLoading, setConfigLoading] = useState(false);
  const [configForm] = Form.useForm();
  const [generating, setGenerating] = useState({});
  const [syncing, setSyncing] = useState({});

  const loadGenTables = useCallback(async () => {
    setGenLoading(true);
    try {
      const res = await request.get('/tool/gen/list');
      if (res.code === 200) setGenTables(res.data || []);
    } catch (e) { /* */ }
    finally { setGenLoading(false); }
  }, []);

  const loadDbTables = useCallback(async () => {
    setDbLoading(true);
    try {
      const res = await request.get('/tool/gen/db/tables');
      if (res.code === 200) setDbTables(res.data || []);
    } catch (e) { /* */ }
    finally { setDbLoading(false); }
  }, []);

  useEffect(() => { loadGenTables(); }, [loadGenTables]);

  const handleOpenImport = () => { loadDbTables(); setSelectedImportTable(null); setImportVisible(true); };

  const handleImport = async () => {
    if (!selectedImportTable) { message.warning('请选择一张表'); return; }
    setImporting(true);
    try {
      const res = await request.post(`/tool/gen/import?tableName=${selectedImportTable}`);
      if (res.code === 200) { message.success(`导入成功：${res.data.className}`); setImportVisible(false); loadGenTables(); }
      else message.error(res.message);
    } catch (e) { message.error(e.message); }
    finally { setImporting(false); }
  };

  // 预览
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
    navigator.clipboard.writeText(code).then(() => message.success('已复制'), () => message.error('复制失败'));
  };

  // 配置编辑
  const handleOpenConfig = async (record) => {
    setConfigLoading(true);
    setConfigVisible(true);
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
      }
    } catch (e) { message.error('加载失败'); }
    finally { setConfigLoading(false); }
  };

  const handleColumnChange = (colId, field, value) => {
    setConfigData(prev => ({
      ...prev,
      columns: prev.columns.map(c => c.id === colId ? { ...c, [field]: value } : c)
    }));
  };

  const handleSaveConfig = async (values) => {
    try {
      const payload = { ...configData, ...values };
      const res = await request.put('/tool/gen', payload);
      if (res.code === 200) { message.success('配置已保存'); setConfigVisible(false); loadGenTables(); }
      else message.error(res.message);
    } catch (e) { message.error('保存失败'); }
  };

  // 生成到项目
  const handleGenerate = async (record) => {
    setGenerating(prev => ({ ...prev, [record.id]: true }));
    try {
      const res = await request.post(`/tool/gen/${record.id}/generate`);
      if (res.code === 200) {
        const files = Object.entries(res.data);
        Modal.success({
          title: '生成成功',
          width: 560,
          content: (
            <div style={{ maxHeight: 300, overflow: 'auto' }}>
              {files.map(([name, path]) => (
                <div key={name} style={{ padding: '4px 0', fontSize: 12 }}>
                  <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 6 }} />
                  <strong>{name}</strong>: <code style={{ fontSize: 11, color: '#666' }}>{path}</code>
                </div>
              ))}
            </div>
          ),
        });
      } else message.error(res.message);
    } catch (e) { message.error('生成失败'); }
    finally { setGenerating(prev => ({ ...prev, [record.id]: false })); }
  };

  // 同步表结构
  const handleSync = async (record) => {
    setSyncing(prev => ({ ...prev, [record.id]: true }));
    try {
      const res = await request.post(`/tool/gen/${record.id}/sync`);
      if (res.code === 200) message.success('同步成功');
      else message.error(res.message);
    } catch (e) { message.error('同步失败'); }
    finally { setSyncing(prev => ({ ...prev, [record.id]: false })); }
  };

  // 下载ZIP
  const handleDownload = (record) => {
    const token = localStorage.getItem('uc_token');
    const a = document.createElement('a');
    a.href = `/api/tool/gen/${record.id}/download?token=${token}`;
    a.download = `${record.className}.zip`;
    a.click();
  };

  const handleDelete = (record) => {
    Modal.confirm({
      title: '确认删除', content: `删除「${record.tableName}」的生成配置？`,
      okButtonProps: { danger: true },
      onOk: async () => {
        const res = await request.delete(`/tool/gen/${record.id}`);
        if (res.code === 200) { message.success('删除成功'); loadGenTables(); }
      }
    });
  };

  const importedSet = new Set(genTables.map(t => t.tableName));

  const columns = [
    { title: '表名', dataIndex: 'tableName', render: (v) => <code>{v}</code> },
    { title: '注释', dataIndex: 'tableComment', ellipsis: true },
    { title: '类名', dataIndex: 'className', render: (v) => <Tag color="blue">{v}</Tag> },
    { title: '业务名', dataIndex: 'businessName' },
    { title: '功能名', dataIndex: 'functionName', ellipsis: true },
    { title: '创建时间', dataIndex: 'createTime', width: 170, render: (v) => v?.replace('T', ' ')?.substring(0, 19) },
    {
      title: '操作', key: 'action', width: 320, fixed: 'right',
      render: (_, record) => (
        <Space size={4} wrap>
          {hasPermission('dev:gen:query') && <Button size="small" type="link" icon={<EyeOutlined />} onClick={() => handlePreview(record)}>预览</Button>}
          {hasPermission('dev:gen:edit') && <Button size="small" type="link" icon={<SettingOutlined />} onClick={() => handleOpenConfig(record)}>配置</Button>}
          {hasPermission('dev:gen:generate') && <Button size="small" type="link" icon={<RocketOutlined />} loading={generating[record.id]} onClick={() => handleGenerate(record)}>生成</Button>}
          {hasPermission('dev:gen:generate') && <Button size="small" type="link" icon={<DownloadOutlined />} onClick={() => handleDownload(record)}>ZIP</Button>}
          {hasPermission('dev:gen:sync') && <Button size="small" type="link" icon={<SyncOutlined spin={syncing[record.id]} />} onClick={() => handleSync(record)}>同步</Button>}
          {hasPermission('dev:gen:delete') && <Button size="small" type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>删除</Button>}
        </Space>
      ),
    },
  ];

  // 列编辑表格
  const columnEditColumns = [
    { title: '列名', dataIndex: 'columnName', width: 130, fixed: 'left', render: (v) => <code style={{ fontSize: 11 }}>{v}</code> },
    { title: '注释', dataIndex: 'columnComment', width: 120,
      render: (v, row) => <Input size="small" value={v} onChange={e => handleColumnChange(row.id, 'columnComment', e.target.value)} />
    },
    { title: 'Java字段', dataIndex: 'javaField', width: 130,
      render: (v, row) => <Input size="small" value={v} onChange={e => handleColumnChange(row.id, 'javaField', e.target.value)} />
    },
    { title: 'Java类型', dataIndex: 'javaType', width: 120,
      render: (v, row) => <Select size="small" value={v} style={{ width: '100%' }} onChange={val => handleColumnChange(row.id, 'javaType', val)}
        options={JAVA_TYPES.map(t => ({ value: t, label: t }))} />
    },
    { title: '必填', dataIndex: 'isRequired', width: 55, align: 'center',
      render: (v, row) => <Switch size="small" checked={v} onChange={val => handleColumnChange(row.id, 'isRequired', val)} />
    },
    { title: '列表', dataIndex: 'isList', width: 55, align: 'center',
      render: (v, row) => <Switch size="small" checked={v} onChange={val => handleColumnChange(row.id, 'isList', val)} />
    },
    { title: '新增', dataIndex: 'isInsert', width: 55, align: 'center',
      render: (v, row) => <Switch size="small" checked={v} onChange={val => handleColumnChange(row.id, 'isInsert', val)} />
    },
    { title: '编辑', dataIndex: 'isEdit', width: 55, align: 'center',
      render: (v, row) => <Switch size="small" checked={v} onChange={val => handleColumnChange(row.id, 'isEdit', val)} />
    },
    { title: '查询', dataIndex: 'isQuery', width: 55, align: 'center',
      render: (v, row) => <Switch size="small" checked={v} onChange={val => handleColumnChange(row.id, 'isQuery', val)} />
    },
    { title: '查询方式', dataIndex: 'queryType', width: 95,
      render: (v, row) => <Select size="small" value={v} style={{ width: '100%' }} onChange={val => handleColumnChange(row.id, 'queryType', val)}
        options={QUERY_TYPES} />
    },
    { title: '表单类型', dataIndex: 'htmlType', width: 110,
      render: (v, row) => <Select size="small" value={v} style={{ width: '100%' }} onChange={val => handleColumnChange(row.id, 'htmlType', val)}
        options={HTML_TYPES} />
    },
    { title: '字典', dataIndex: 'dictType', width: 120,
      render: (v, row) => <Input size="small" value={v} placeholder="dict_key" onChange={e => handleColumnChange(row.id, 'dictType', e.target.value)} />
    },
  ];

  return (
    <div className="gen-page">
      <Card
        title={<span><CodeOutlined style={{ color: '#667eea', marginRight: 8 }} />代码生成</span>}
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={loadGenTables}>刷新</Button>
            {hasPermission('dev:gen:import') && <Button type="primary" icon={<ImportOutlined />} onClick={handleOpenImport}>导入表</Button>}
          </Space>
        }
      >
        <Table
          dataSource={genTables} columns={columns} rowKey="id" loading={genLoading}
          pagination={false} size="middle" scroll={{ x: 1100 }}
          locale={{ emptyText: <Empty description="暂无导入的表，点击「导入表」开始" /> }}
        />
      </Card>

      {/* 导入表弹窗 */}
      <Modal title={<span><DatabaseOutlined /> 选择数据库表</span>} open={importVisible}
        onCancel={() => setImportVisible(false)} onOk={handleImport} okText="导入" confirmLoading={importing} width={560}>
        <div style={{ marginBottom: 12, color: '#8c8c8c', fontSize: 13 }}>选择一张表导入，系统会自动解析列信息。</div>
        <Select placeholder="搜索或选择表..." showSearch style={{ width: '100%' }} value={selectedImportTable}
          onChange={setSelectedImportTable} loading={dbLoading} optionFilterProp="label"
          options={dbTables.map(t => ({
            value: t.table_name,
            label: (<span><code>{t.table_name}</code>{t.table_comment && <span style={{ color: '#8c8c8c', marginLeft: 8 }}>{t.table_comment}</span>}
              {importedSet.has(t.table_name) && <Tag color="orange" style={{ marginLeft: 8 }}>已导入</Tag>}</span>),
            disabled: importedSet.has(t.table_name),
          }))}
        />
      </Modal>

      {/* 代码预览 */}
      <Drawer title={<span><CodeOutlined /> 代码预览 - {previewTable?.className}</span>}
        width={800} open={previewVisible} onClose={() => setPreviewVisible(false)} destroyOnClose>
        {previewLoading ? <Spin style={{ display: 'block', textAlign: 'center', marginTop: 100 }} /> : (
          Object.keys(previewData).length > 0 ? (
            <Tabs tabPosition="left" items={Object.entries(previewData).map(([name, code]) => ({
              key: name, label: <span style={{ fontSize: 12 }}>{name}</span>,
              children: (
                <div className="code-preview">
                  <div className="code-header"><span>{name}</span><Button size="small" icon={<CopyOutlined />} onClick={() => handleCopyCode(code)}>复制</Button></div>
                  <pre className="code-content">{code}</pre>
                </div>
              ),
            }))} />
          ) : <Empty description="无预览数据" />
        )}
      </Drawer>

      {/* 配置编辑 */}
      <Drawer title={`编辑配置 - ${configData?.tableName || ''}`}
        width={1100} open={configVisible} onClose={() => setConfigVisible(false)} destroyOnClose
        extra={<Space><Button onClick={() => setConfigVisible(false)}>取消</Button><Button type="primary" onClick={() => configForm.submit()}>保存</Button></Space>}>
        {configLoading ? <Spin style={{ display: 'block', textAlign: 'center', marginTop: 100 }} /> : (
          <>
            <Form form={configForm} layout="inline" onFinish={handleSaveConfig} style={{ marginBottom: 16, gap: 8, flexWrap: 'wrap' }}>
              <Form.Item name="className" label="类名" rules={[{ required: true }]}><Input style={{ width: 150 }} /></Form.Item>
              <Form.Item name="packageName" label="包名" rules={[{ required: true }]}><Input style={{ width: 200 }} /></Form.Item>
              <Form.Item name="businessName" label="业务名" rules={[{ required: true }]}><Input style={{ width: 130 }} /></Form.Item>
              <Form.Item name="functionName" label="功能名"><Input style={{ width: 130 }} /></Form.Item>
              <Form.Item name="author" label="作者"><Input style={{ width: 100 }} /></Form.Item>
            </Form>
            <div style={{ fontWeight: 600, marginBottom: 8, fontSize: 14 }}>字段配置</div>
            <Table dataSource={configData?.columns || []} columns={columnEditColumns} rowKey="id"
              size="small" pagination={false} scroll={{ x: 1200, y: 400 }} bordered />
          </>
        )}
      </Drawer>
    </div>
  );
}
