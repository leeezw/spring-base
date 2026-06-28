import { PlusOutlined } from '@ant-design/icons';
import {
  ActionType,
  ModalForm,
  PageContainer,
  ProColumns,
  ProFormDependency,
  ProFormSelect,
  ProFormText,
  ProTable,
} from '@ant-design/pro-components';
import { Button, message, Popconfirm, Tag } from 'antd';
import { useRef } from 'react';
import { deleteDnsProvider, pageDnsProviders, saveDnsProvider } from '../api/cert';
import type { DnsProvider } from '../types';

export default function DnsProviders() {
  const actionRef = useRef<ActionType>();

  const columns: ProColumns<DnsProvider>[] = [
    { title: '名称', dataIndex: 'name' },
    { title: '类型', dataIndex: 'type', search: false, render: (_, r) => <Tag>{r.type}</Tag> },
    {
      title: '状态',
      dataIndex: 'status',
      search: false,
      render: (_, r) => (r.status === 1 ? <Tag color="blue">启用</Tag> : <Tag>停用</Tag>),
    },
    { title: '备注', dataIndex: 'remark', search: false },
    {
      title: '操作',
      valueType: 'option',
      render: (_, r) => [
        <EditForm key="edit" record={r} reload={() => actionRef.current?.reload()} />,
        <Popconfirm
          key="del"
          title="确认删除？"
          onConfirm={async () => {
            await deleteDnsProvider(r.id);
            message.success('已删除');
            actionRef.current?.reload();
          }}
        >
          <a style={{ color: '#ff4d4f' }}>删除</a>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <PageContainer header={{ title: 'DNS 服务商' }}>
      <ProTable<DnsProvider>
        rowKey="id"
        actionRef={actionRef}
        columns={columns}
        request={async (params) => {
          const res = await pageDnsProviders({ pageNum: params.current, pageSize: params.pageSize, keyword: params.name });
          return { data: res.records, total: res.total, success: true };
        }}
        toolBarRender={() => [<EditForm key="add" reload={() => actionRef.current?.reload()} />]}
      />
    </PageContainer>
  );
}

function EditForm({ record, reload }: { record?: DnsProvider; reload: () => void }) {
  const isEdit = !!record;
  return (
    <ModalForm
      title={isEdit ? '编辑 DNS 服务商' : '新增 DNS 服务商'}
      trigger={isEdit ? <a>编辑</a> : <Button type="primary" icon={<PlusOutlined />}>新增服务商</Button>}
      width={520}
      initialValues={record ?? { type: 'west', status: 1 }}
      onFinish={async (values: Record<string, unknown>) => {
        const { name, type, status, remark, ...rest } = values;
        // 凭证字段折叠进 credential
        const credential: Record<string, string> = {};
        ['username', 'apiPassword', 'endpoint', 'apiToken', 'secretId', 'secretKey'].forEach((k) => {
          if (rest[k]) credential[k] = String(rest[k]);
        });
        await saveDnsProvider({ name, type, status, remark, credential }, record?.id);
        message.success('保存成功');
        reload();
        return true;
      }}
    >
      <ProFormText name="name" label="名称" rules={[{ required: true }]} />
      <ProFormSelect
        name="type"
        label="类型"
        rules={[{ required: true }]}
        options={[
          { label: '西部数码', value: 'west' },
          { label: '手动（兜底）', value: 'manual' },
        ]}
      />
      <ProFormDependency name={['type']}>
        {({ type }) =>
          type === 'west' ? (
            <>
              <ProFormText name="username" label="账号" rules={[{ required: true }]} />
              <ProFormText.Password name="apiPassword" label="API 密码/密钥" rules={[{ required: true }]} />
              <ProFormText name="endpoint" label="自定义 API 地址" tooltip="留空使用默认" />
            </>
          ) : (
            <div style={{ color: '#999', marginBottom: 12 }}>
              手动模式不自动写记录，仅作兜底；建议使用具备 API 的服务商以实现全自动续期。
            </div>
          )
        }
      </ProFormDependency>
      <ProFormText name="remark" label="备注" />
    </ModalForm>
  );
}
