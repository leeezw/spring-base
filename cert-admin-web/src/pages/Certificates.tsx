import { PlusOutlined } from '@ant-design/icons';
import {
  ActionType,
  ModalForm,
  PageContainer,
  ProColumns,
  ProFormDigit,
  ProFormSelect,
  ProFormSwitch,
  ProTable,
} from '@ant-design/pro-components';
import { Button, message, Modal, Popconfirm, Space, Tag, Typography } from 'antd';
import { useRef } from 'react';
import {
  deleteCertificate,
  deployCertificate,
  exportPem,
  issueCertificate,
  listAccounts,
  listDeployTargets,
  listDnsProviders,
  pageCertificates,
  renewCertificate,
} from '../api/cert';
import type { Certificate } from '../types';
import { statusTag } from '../components/statusTag';

function downloadText(filename: string, content: string) {
  const blob = new Blob([content], { type: 'text/plain' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export default function Certificates() {
  const actionRef = useRef<ActionType>();

  const showPem = async (id: number, domain: string) => {
    const pem = await exportPem(id);
    Modal.info({
      title: `证书 PEM - ${domain}`,
      width: 720,
      content: (
        <Space direction="vertical" style={{ width: '100%' }}>
          <Button onClick={() => downloadText(`${domain}.fullchain.pem`, pem.fullchain)}>下载 fullchain.pem</Button>
          <Button onClick={() => downloadText(`${domain}.privkey.pem`, pem.privkey)}>下载 privkey.pem</Button>
          <Typography.Paragraph
            code
            copyable
            style={{ maxHeight: 200, overflow: 'auto', whiteSpace: 'pre-wrap' }}
          >
            {pem.fullchain}
          </Typography.Paragraph>
        </Space>
      ),
    });
  };

  const columns: ProColumns<Certificate>[] = [
    { title: '主域名', dataIndex: 'primaryDomain', copyable: true },
    {
      title: '附加域名',
      dataIndex: 'sanDomains',
      search: false,
      render: (_, r) => (r.sanDomains?.length ? r.sanDomains.map((d) => <Tag key={d}>{d}</Tag>) : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueEnum: {
        PENDING: { text: '待签发' },
        ISSUING: { text: '签发中' },
        ACTIVE: { text: '有效' },
        EXPIRING: { text: '临期' },
        FAILED: { text: '失败' },
      },
      render: (_, r) => statusTag(r.status),
    },
    {
      title: '剩余天数',
      dataIndex: 'daysRemaining',
      search: false,
      render: (_, r) =>
        r.daysRemaining == null ? '-' : <Tag color={r.daysRemaining <= 7 ? 'red' : r.daysRemaining <= 15 ? 'orange' : 'green'}>{r.daysRemaining} 天</Tag>,
    },
    { title: '密钥算法', dataIndex: 'keyAlgo', search: false },
    {
      title: '自动续期',
      dataIndex: 'autoRenew',
      search: false,
      render: (_, r) => (r.autoRenew === 1 ? <Tag color="blue">是</Tag> : <Tag>否</Tag>),
    },
    { title: '到期时间', dataIndex: 'notAfter', search: false },
    {
      title: '操作',
      valueType: 'option',
      width: 240,
      render: (_, r) => [
        <Popconfirm
          key="renew"
          title="确认立即续期/重新签发？"
          onConfirm={async () => {
            await renewCertificate(r.id);
            message.success('续期完成');
            actionRef.current?.reload();
          }}
        >
          <a>续期</a>
        </Popconfirm>,
        <Popconfirm
          key="deploy"
          title="确认部署到绑定目标？"
          onConfirm={async () => {
            const msg = await deployCertificate(r.id);
            Modal.success({ title: '部署结果', content: <pre>{msg}</pre> });
            actionRef.current?.reload();
          }}
        >
          <a>部署</a>
        </Popconfirm>,
        <a key="pem" onClick={() => showPem(r.id, r.primaryDomain)}>
          下载
        </a>,
        <Popconfirm
          key="del"
          title="确认删除该证书记录？"
          onConfirm={async () => {
            await deleteCertificate(r.id);
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
    <PageContainer header={{ title: '证书管理' }}>
      <ProTable<Certificate>
        rowKey="id"
        actionRef={actionRef}
        columns={columns}
        request={async (params) => {
          const res = await pageCertificates({
            pageNum: params.current,
            pageSize: params.pageSize,
            keyword: params.primaryDomain,
            status: params.status,
          });
          return { data: res.records, total: res.total, success: true };
        }}
        toolBarRender={() => [
          <ModalForm
            key="issue"
            title="新建签发"
            trigger={
              <Button type="primary" icon={<PlusOutlined />}>
                新建签发
              </Button>
            }
            width={560}
            onFinish={async (values) => {
              await issueCertificate(values);
              message.success('签发请求完成');
              actionRef.current?.reload();
              return true;
            }}
          >
            <ProFormSelect
              name="domains"
              label="域名"
              mode="tags"
              placeholder="输入域名后回车，可多个；支持通配 *.example.com"
              rules={[{ required: true, message: '请输入域名' }]}
              extra="第一个域名作为主域名"
            />
            <ProFormSelect
              name="acmeAccountId"
              label="ACME 账户"
              rules={[{ required: true, message: '请选择 ACME 账户' }]}
              request={async () => (await listAccounts()).map((a) => ({ label: `${a.name}（${a.caType}）`, value: a.id }))}
            />
            <ProFormSelect
              name="dnsProviderId"
              label="DNS 服务商"
              rules={[{ required: true, message: '请选择 DNS 服务商' }]}
              request={async () => (await listDnsProviders()).map((d) => ({ label: `${d.name}（${d.type}）`, value: d.id }))}
            />
            <ProFormSelect
              name="keyAlgo"
              label="密钥算法"
              initialValue="RSA2048"
              options={[
                { label: 'RSA 2048', value: 'RSA2048' },
                { label: 'ECDSA P-256', value: 'EC256' },
              ]}
            />
            <ProFormSelect
              name="deployTargetIds"
              label="部署目标"
              mode="multiple"
              placeholder="可选，签发成功后自动部署"
              request={async () => (await listDeployTargets()).map((t) => ({ label: `${t.name}（${t.type}）`, value: t.id }))}
            />
            <ProFormSwitch name="autoRenew" label="自动续期" initialValue fieldProps={{ defaultChecked: true }} />
            <ProFormDigit name="renewBeforeDays" label="到期前续期天数" initialValue={30} min={1} max={89} />
          </ModalForm>,
        ]}
      />
    </PageContainer>
  );
}
