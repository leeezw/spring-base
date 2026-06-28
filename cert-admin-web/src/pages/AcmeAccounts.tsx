import { PlusOutlined } from '@ant-design/icons';
import {
  ActionType,
  ModalForm,
  PageContainer,
  ProColumns,
  ProFormDependency,
  ProFormSelect,
  ProFormSwitch,
  ProFormText,
  ProTable,
} from '@ant-design/pro-components';
import { Button, message, Popconfirm, Tag } from 'antd';
import { useRef } from 'react';
import { deleteAccount, pageAccounts, saveAccount } from '../api/cert';
import type { AcmeAccount } from '../types';

export default function AcmeAccounts() {
  const actionRef = useRef<ActionType>();

  const columns: ProColumns<AcmeAccount>[] = [
    { title: '名称', dataIndex: 'name' },
    { title: 'CA 类型', dataIndex: 'caType', search: false, render: (_, r) => <Tag>{r.caType}</Tag> },
    { title: '邮箱', dataIndex: 'email', search: false },
    { title: 'Directory', dataIndex: 'directoryUrl', search: false, ellipsis: true },
    {
      title: '已注册',
      dataIndex: 'registered',
      search: false,
      render: (_, r) => (r.registered ? <Tag color="green">是</Tag> : <Tag>未注册</Tag>),
    },
    {
      title: '状态',
      dataIndex: 'status',
      search: false,
      render: (_, r) => (r.status === 1 ? <Tag color="blue">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      valueType: 'option',
      render: (_, r) => [
        <EditForm key="edit" record={r} reload={() => actionRef.current?.reload()} />,
        <Popconfirm
          key="del"
          title="确认删除？"
          onConfirm={async () => {
            await deleteAccount(r.id);
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
    <PageContainer header={{ title: 'ACME 账户' }}>
      <ProTable<AcmeAccount>
        rowKey="id"
        actionRef={actionRef}
        columns={columns}
        request={async (params) => {
          const res = await pageAccounts({ pageNum: params.current, pageSize: params.pageSize, keyword: params.name });
          return { data: res.records, total: res.total, success: true };
        }}
        toolBarRender={() => [<EditForm key="add" reload={() => actionRef.current?.reload()} />]}
      />
    </PageContainer>
  );
}

function EditForm({ record, reload }: { record?: AcmeAccount; reload: () => void }) {
  const isEdit = !!record;
  return (
    <ModalForm
      title={isEdit ? '编辑 ACME 账户' : '新增 ACME 账户'}
      trigger={isEdit ? <a>编辑</a> : <Button type="primary" icon={<PlusOutlined />}>新增账户</Button>}
      width={520}
      initialValues={record ?? { caType: 'letsencrypt', staging: false, status: 1 }}
      onFinish={async (values) => {
        await saveAccount(values, record?.id);
        message.success('保存成功');
        reload();
        return true;
      }}
    >
      <ProFormText name="name" label="名称" rules={[{ required: true }]} />
      <ProFormSelect
        name="caType"
        label="CA 类型"
        rules={[{ required: true }]}
        options={[
          { label: "Let's Encrypt", value: 'letsencrypt' },
          { label: 'ZeroSSL', value: 'zerossl' },
        ]}
      />
      <ProFormText name="email" label="邮箱" rules={[{ required: true, type: 'email' }]} />
      <ProFormDependency name={['caType']}>
        {({ caType }) =>
          caType === 'zerossl' ? (
            <>
              <ProFormText name="eabKid" label="EAB KID" tooltip="ZeroSSL 控制台获取" />
              <ProFormText.Password name="eabHmac" label="EAB HMAC" />
            </>
          ) : (
            <ProFormSwitch name="staging" label="使用测试目录(staging)" tooltip="联调阶段建议开启，避开生产限频" />
          )
        }
      </ProFormDependency>
      <ProFormText name="directoryUrl" label="自定义 Directory URL" tooltip="留空则按 CA 类型自动选择" />
      <ProFormText name="remark" label="备注" />
    </ModalForm>
  );
}
