import { PlusOutlined } from '@ant-design/icons';
import {
  ActionType,
  ModalForm,
  PageContainer,
  ProColumns,
  ProFormDependency,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import { Button, message, Popconfirm, Tag } from 'antd';
import { useRef } from 'react';
import { deleteDeployTarget, pageDeployTargets, saveDeployTarget, testDeployTarget } from '../api/cert';
import type { DeployTarget } from '../types';

const TYPE_LABEL: Record<string, string> = {
  nginx_ssh: 'Nginx(SSH)',
  west_ssh: '西部数码服务器(SSH)',
  baota: '宝塔面板',
  qiniu: '七牛云',
};

// 各类型对应的配置字段
const CONFIG_FIELDS: Record<string, string[]> = {
  nginx_ssh: ['host', 'port', 'username', 'authType', 'password', 'privateKey', 'fullchainPath', 'keyPath', 'reloadCmd'],
  west_ssh: ['host', 'port', 'username', 'authType', 'password', 'privateKey', 'fullchainPath', 'keyPath', 'reloadCmd'],
  baota: ['panelUrl', 'apiKey', 'siteName'],
  qiniu: ['accessKey', 'secretKey', 'domain'],
};

export default function DeployTargets() {
  const actionRef = useRef<ActionType>();

  const columns: ProColumns<DeployTarget>[] = [
    { title: '名称', dataIndex: 'name' },
    { title: '类型', dataIndex: 'type', search: false, render: (_, r) => <Tag>{TYPE_LABEL[r.type] || r.type}</Tag> },
    {
      title: '状态',
      dataIndex: 'enabled',
      search: false,
      render: (_, r) => (r.enabled === 1 ? <Tag color="blue">启用</Tag> : <Tag>停用</Tag>),
    },
    { title: '备注', dataIndex: 'remark', search: false },
    {
      title: '操作',
      valueType: 'option',
      render: (_, r) => [
        <a
          key="test"
          onClick={async () => {
            const hide = message.loading('测试连接中...', 0);
            try {
              await testDeployTarget(r.id);
              message.success('连接成功');
            } finally {
              hide();
            }
          }}
        >
          测试
        </a>,
        <EditForm key="edit" record={r} reload={() => actionRef.current?.reload()} />,
        <Popconfirm
          key="del"
          title="确认删除？"
          onConfirm={async () => {
            await deleteDeployTarget(r.id);
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
    <PageContainer header={{ title: '部署目标' }}>
      <ProTable<DeployTarget>
        rowKey="id"
        actionRef={actionRef}
        columns={columns}
        request={async (params) => {
          const res = await pageDeployTargets({ pageNum: params.current, pageSize: params.pageSize, keyword: params.name });
          return { data: res.records, total: res.total, success: true };
        }}
        toolBarRender={() => [<EditForm key="add" reload={() => actionRef.current?.reload()} />]}
      />
    </PageContainer>
  );
}

function EditForm({ record, reload }: { record?: DeployTarget; reload: () => void }) {
  const isEdit = !!record;
  return (
    <ModalForm
      title={isEdit ? '编辑部署目标' : '新增部署目标'}
      trigger={isEdit ? <a>编辑</a> : <Button type="primary" icon={<PlusOutlined />}>新增目标</Button>}
      width={560}
      initialValues={record ?? { type: 'nginx_ssh', enabled: 1, authType: 'password', port: '22', reloadCmd: 'nginx -s reload' }}
      onFinish={async (values: Record<string, unknown>) => {
        const { name, type, enabled, remark, ...rest } = values;
        const fields = CONFIG_FIELDS[String(type)] || [];
        const config: Record<string, string> = {};
        fields.forEach((k) => {
          if (rest[k] !== undefined && rest[k] !== '') config[k] = String(rest[k]);
        });
        await saveDeployTarget({ name, type, enabled, remark, config }, record?.id);
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
        options={Object.entries(TYPE_LABEL).map(([value, label]) => ({ label, value }))}
      />
      <ProFormDependency name={['type', 'authType']}>
        {({ type, authType }) => {
          if (type === 'baota') {
            return (
              <>
                <ProFormText name="panelUrl" label="面板地址" placeholder="http://1.2.3.4:8888" rules={[{ required: true }]} />
                <ProFormText.Password name="apiKey" label="接口密钥" rules={[{ required: true }]} />
                <ProFormText name="siteName" label="站点名称/域名" rules={[{ required: true }]} />
              </>
            );
          }
          if (type === 'qiniu') {
            return (
              <>
                <ProFormText name="accessKey" label="AccessKey" rules={[{ required: true }]} />
                <ProFormText.Password name="secretKey" label="SecretKey" rules={[{ required: true }]} />
                <ProFormText name="domain" label="加速域名" rules={[{ required: true }]} />
              </>
            );
          }
          // SSH 类（nginx_ssh / west_ssh）
          return (
            <>
              <ProFormText name="host" label="主机地址" rules={[{ required: true }]} />
              <ProFormText name="port" label="端口" initialValue="22" />
              <ProFormText name="username" label="用户名" rules={[{ required: true }]} />
              <ProFormSelect
                name="authType"
                label="认证方式"
                options={[
                  { label: '密码', value: 'password' },
                  { label: '私钥', value: 'key' },
                ]}
              />
              {authType === 'key' ? (
                <ProFormTextArea name="privateKey" label="私钥(PEM)" fieldProps={{ rows: 4 }} rules={[{ required: true }]} />
              ) : (
                <ProFormText.Password name="password" label="密码" rules={[{ required: true }]} />
              )}
              <ProFormText name="fullchainPath" label="证书链落盘路径" placeholder="/etc/nginx/ssl/fullchain.pem" rules={[{ required: true }]} />
              <ProFormText name="keyPath" label="私钥落盘路径" placeholder="/etc/nginx/ssl/privkey.pem" rules={[{ required: true }]} />
              <ProFormText name="reloadCmd" label="重载命令" initialValue="nginx -s reload" />
            </>
          );
        }}
      </ProFormDependency>
      <ProFormText name="remark" label="备注" />
    </ModalForm>
  );
}
