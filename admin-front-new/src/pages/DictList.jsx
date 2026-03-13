import { useState, useCallback, useEffect, useRef } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, InputNumber, Switch, message, Drawer, Divider, Row, Col, Badge, Tooltip, ColorPicker, Card } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, BookOutlined, SettingOutlined } from '@ant-design/icons';
import request from '../api/index.js';
import { usePermission } from '../hooks/usePermission.jsx';
import { clearDictCache } from '../hooks/useDict.jsx';
import ProTableV2 from '../components/ProTableV2.jsx';

export default function DictList() {
  const { hasPermission } = usePermission();
  const actionRef = useRef();
  
  // 字典类型
  const [dictDrawerVisible, setDictDrawerVisible] = useState(false);
  const [editingDict, setEditingDict] = useState(null);
  const [dictForm] = Form.useForm();
  const [dictSubmitLoading, setDictSubmitLoading] = useState(false);

  // 字典数据项
  const [selectedDict, setSelectedDict] = useState(null);
  const [items, setItems] = useState([]);
  const [itemsLoading, setItemsLoading] = useState(false);
  const [itemDrawerVisible, setItemDrawerVisible] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [itemForm] = Form.useForm();
  const [itemSubmitLoading, setItemSubmitLoading] = useState(false);

  // 加载字典数据项
  const loadItems = useCallback(async (dictId) => {
    setItemsLoading(true);
    try {
      const res = await request.get(`/system/dict/${dictId}/items`);
      if (res.code === 200) setItems(res.data || []);
    } catch (e) { message.error('加载失败'); }
    finally { setItemsLoading(false); }
  }, []);

  // 选中字典
  const handleSelectDict = useCallback((dict) => {
    setSelectedDict(dict);
    loadItems(dict.id);
  }, [loadItems]);

  // ============ 字典类型 CRUD ============
  const fetchDicts = useCallback(async (params) => {
    return await request.get('/system/dict/page', { params: { page: params.current, size: params.pageSize, keyword: params.keyword } });
  }, []);

  const handleAddDict = () => {
    setEditingDict(null);
    dictForm.setFieldsValue({ dictCode: '', dictName: '', description: '', sortOrder: 0, status: true });
    setDictDrawerVisible(true);
  };

  const handleEditDict = (record) => {
    setEditingDict(record);
    dictForm.setFieldsValue({ ...record, status: record.status === 1 });
    setDictDrawerVisible(true);
  };

  const handleDictSubmit = async (values) => {
    setDictSubmitLoading(true);
    try {
      const payload = { ...values, status: values.status ? 1 : 0 };
      if (editingDict) {
        payload.id = editingDict.id;
        const res = await request.put('/system/dict', payload);
        if (res.code === 200) { message.success('更新成功'); clearDictCache(editingDict.dictCode); }
        else { message.error(res.message); return; }
      } else {
        const res = await request.post('/system/dict', payload);
        if (res.code === 200) message.success('创建成功');
        else { message.error(res.message); return; }
      }
      setDictDrawerVisible(false);
      actionRef.current?.reload();
    } catch (e) { message.error('操作失败'); }
    finally { setDictSubmitLoading(false); }
  };

  const handleDeleteDict = (record) => {
    Modal.confirm({
      title: '确认删除', content: `删除字典「${record.dictName}」将同时删除其所有数据项。`,
      okText: '删除', okButtonProps: { danger: true },
      onOk: async () => {
        const res = await request.delete(`/system/dict/${record.id}`);
        if (res.code === 200) {
          message.success('删除成功');
          clearDictCache(record.dictCode);
          actionRef.current?.reload();
          if (selectedDict?.id === record.id) { setSelectedDict(null); setItems([]); }
        } else message.error(res.message);
      }
    });
  };

  // ============ 字典数据项 CRUD ============
  const handleAddItem = () => {
    setEditingItem(null);
    itemForm.setFieldsValue({ itemValue: '', itemLabel: '', itemColor: '', itemIcon: '', description: '', sortOrder: 0, status: true, isDefault: false });
    setItemDrawerVisible(true);
  };

  const handleEditItem = (record) => {
    setEditingItem(record);
    itemForm.setFieldsValue({ ...record, status: record.status === 1, isDefault: record.isDefault === 1 });
    setItemDrawerVisible(true);
  };

  const handleItemSubmit = async (values) => {
    setItemSubmitLoading(true);
    try {
      const payload = { ...values, status: values.status ? 1 : 0, isDefault: values.isDefault ? 1 : 0, dictId: selectedDict.id };
      if (editingItem) {
        payload.id = editingItem.id;
        const res = await request.put('/system/dict/item', payload);
        if (res.code === 200) { message.success('更新成功'); clearDictCache(selectedDict.dictCode); }
        else { message.error(res.message); return; }
      } else {
        const res = await request.post('/system/dict/item', payload);
        if (res.code === 200) { message.success('创建成功'); clearDictCache(selectedDict.dictCode); }
        else { message.error(res.message); return; }
      }
      setItemDrawerVisible(false);
      loadItems(selectedDict.id);
      actionRef.current?.reload();
    } catch (e) { message.error('操作失败'); }
    finally { setItemSubmitLoading(false); }
  };

  const handleDeleteItem = (record) => {
    Modal.confirm({
      title: '确认删除', content: `确定删除「${record.itemLabel}」？`,
      okText: '删除', okButtonProps: { danger: true },
      onOk: async () => {
        const res = await request.delete(`/system/dict/item/${record.id}`);
        if (res.code === 200) {
          message.success('删除成功');
          clearDictCache(selectedDict.dictCode);
          loadItems(selectedDict.id);
          actionRef.current?.reload();
        } else message.error(res.message);
      }
    });
  };

  // ============ 表格列 ============
  const dictColumns = [
    { title: '字典名称', dataIndex: 'dictName', key: 'dictName', width: 140,
      render: (text, record) => (
        <a onClick={() => handleSelectDict(record)} style={{ fontWeight: selectedDict?.id === record.id ? 700 : 400 }}>{text}</a>
      ),
    },
    { title: '字典编码', dataIndex: 'dictCode', key: 'dictCode', width: 160,
      render: (text) => <code style={{ background: '#f5f5f5', padding: '2px 6px', borderRadius: 4, fontSize: 12 }}>{text}</code>,
    },
    { title: '数据项', key: 'itemCount', width: 80, align: 'center',
      render: (_, record) => <Badge count={record.items?.length || 0} showZero size="small" style={{ backgroundColor: '#3f8cff' }} />,
    },
    { title: '状态', dataIndex: 'status', key: 'status', width: 70,
      render: (v) => <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '停用'}</Tag>,
    },
    { title: '操作', key: 'action', width: 100, fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
          {hasPermission('system:dict:edit') && <Button type="text" icon={<EditOutlined />} size="small" onClick={() => handleEditDict(record)} />}
          {hasPermission('system:dict:delete') && <Button type="text" icon={<DeleteOutlined />} size="small" danger onClick={() => handleDeleteDict(record)} />}
        </Space>
      ),
    },
  ];

  const itemColumns = [
    { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 60, align: 'center' },
    { title: '标签', dataIndex: 'itemLabel', key: 'itemLabel', width: 120,
      render: (text, record) => <Tag color={record.itemColor || 'default'}>{text}</Tag>,
    },
    { title: '值', dataIndex: 'itemValue', key: 'itemValue', width: 100,
      render: (v) => <code style={{ background: '#f5f5f5', padding: '2px 6px', borderRadius: 4 }}>{v}</code>,
    },
    { title: '颜色', dataIndex: 'itemColor', key: 'itemColor', width: 100,
      render: (v) => v ? <Tag color={v}>{v}</Tag> : '-',
    },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: '默认', dataIndex: 'isDefault', key: 'isDefault', width: 60, align: 'center',
      render: (v) => v === 1 ? <Tag color="blue">是</Tag> : '-',
    },
    { title: '状态', dataIndex: 'status', key: 'status', width: 60,
      render: (v) => <Tag color={v === 1 ? 'success' : 'default'}>{v === 1 ? '启用' : '停用'}</Tag>,
    },
    { title: '操作', key: 'action', width: 100, fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
          {hasPermission('system:dict:edit') && <Button type="text" icon={<EditOutlined />} size="small" onClick={() => handleEditItem(record)} />}
          {hasPermission('system:dict:delete') && <Button type="text" icon={<DeleteOutlined />} size="small" danger onClick={() => handleDeleteItem(record)} />}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, height: 'calc(100vh - 120px)' }}>
      {/* 左侧：字典类型列表 */}
      <div style={{ overflow: 'auto' }}>
        <ProTableV2
          actionRef={actionRef}
          headerTitle="字典类型"
          columns={dictColumns}
          request={fetchDicts}
          rowKey="id"
          search={false}
          scroll={{ x: 550 }}
          pagination={{ pageSize: 10 }}
          toolbar={{
            actions: [
              hasPermission('system:dict:add') && (
                <Button key="add" type="primary" icon={<PlusOutlined />} onClick={handleAddDict}>新增字典</Button>
              ),
            ].filter(Boolean),
          }}
        />
      </div>

      {/* 右侧：字典数据项 */}
      <Card
        title={selectedDict ? (
          <Space>
            <BookOutlined />
            <span>{selectedDict.dictName}</span>
            <code style={{ fontSize: 12, color: '#8c8c8c' }}>{selectedDict.dictCode}</code>
          </Space>
        ) : '字典数据'}
        extra={selectedDict && hasPermission('system:dict:add') && (
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={handleAddItem}>新增数据项</Button>
        )}
        style={{ height: '100%', overflow: 'auto' }}
        styles={{ body: { padding: selectedDict ? 0 : 24 } }}
      >
        {selectedDict ? (
          <Table
            dataSource={items}
            columns={itemColumns}
            rowKey="id"
            loading={itemsLoading}
            pagination={false}
            size="small"
            scroll={{ x: 600 }}
          />
        ) : (
          <div style={{ textAlign: 'center', color: '#8c8c8c', marginTop: 100 }}>
            <BookOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />
            <p style={{ marginTop: 16 }}>点击左侧字典名称查看数据项</p>
          </div>
        )}
      </Card>

      {/* 字典类型抽屉 */}
      <Drawer
        title={editingDict ? '编辑字典' : '新增字典'}
        open={dictDrawerVisible} onClose={() => setDictDrawerVisible(false)}
        width={420} destroyOnClose
        extra={<Space><Button onClick={() => setDictDrawerVisible(false)}>取消</Button><Button type="primary" onClick={() => dictForm.submit()} loading={dictSubmitLoading}>保存</Button></Space>}
      >
        <Form form={dictForm} layout="vertical" onFinish={handleDictSubmit}>
          <Form.Item name="dictName" label="字典名称" rules={[{ required: true }]}><Input placeholder="如 岗位类别" /></Form.Item>
          <Form.Item name="dictCode" label="字典编码" rules={[{ required: true }]}><Input placeholder="如 post_category" disabled={!!editingDict} /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={2} /></Form.Item>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="sortOrder" label="排序"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item></Col>
            <Col span={12}><Form.Item name="status" label="状态" valuePropName="checked"><Switch checkedChildren="启用" unCheckedChildren="停用" /></Form.Item></Col>
          </Row>
        </Form>
      </Drawer>

      {/* 数据项抽屉 */}
      <Drawer
        title={editingItem ? '编辑数据项' : '新增数据项'}
        open={itemDrawerVisible} onClose={() => setItemDrawerVisible(false)}
        width={420} destroyOnClose
        extra={<Space><Button onClick={() => setItemDrawerVisible(false)}>取消</Button><Button type="primary" onClick={() => itemForm.submit()} loading={itemSubmitLoading}>保存</Button></Space>}
      >
        <Form form={itemForm} layout="vertical" onFinish={handleItemSubmit}>
          <Row gutter={16}>
            <Col span={12}><Form.Item name="itemLabel" label="标签名" rules={[{ required: true }]}><Input placeholder="如 高管" /></Form.Item></Col>
            <Col span={12}><Form.Item name="itemValue" label="数据值" rules={[{ required: true }]}><Input placeholder="如 1" /></Form.Item></Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="itemColor" label="标签颜色" tooltip="支持: red, orange, blue, green, cyan, magenta, success, default 等">
                <Input placeholder="如 red" />
              </Form.Item>
            </Col>
            <Col span={12}><Form.Item name="sortOrder" label="排序"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item></Col>
          </Row>
          <Form.Item name="description" label="描述"><Input.TextArea rows={2} /></Form.Item>
          <Row gutter={16}>
            <Col span={8}><Form.Item name="status" label="状态" valuePropName="checked"><Switch checkedChildren="启用" unCheckedChildren="停用" /></Form.Item></Col>
            <Col span={8}><Form.Item name="isDefault" label="默认值" valuePropName="checked"><Switch checkedChildren="是" unCheckedChildren="否" /></Form.Item></Col>
          </Row>
        </Form>
      </Drawer>
    </div>
  );
}
