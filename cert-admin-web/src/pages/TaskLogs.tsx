import { ActionType, PageContainer, ProColumns, ProTable } from '@ant-design/pro-components';
import { Tag } from 'antd';
import { useRef } from 'react';
import { pageTaskLogs } from '../api/cert';
import type { TaskLog } from '../types';
import { taskStatusTag } from '../components/statusTag';

const TYPE_LABEL: Record<string, string> = {
  ISSUE: '签发',
  RENEW: '续期',
  DEPLOY: '部署',
  REVOKE: '吊销',
};

export default function TaskLogs() {
  const actionRef = useRef<ActionType>();

  const columns: ProColumns<TaskLog>[] = [
    { title: 'ID', dataIndex: 'id', search: false, width: 80 },
    { title: '证书ID', dataIndex: 'certificateId', width: 90 },
    {
      title: '类型',
      dataIndex: 'type',
      valueEnum: { ISSUE: { text: '签发' }, RENEW: { text: '续期' }, DEPLOY: { text: '部署' }, REVOKE: { text: '吊销' } },
      render: (_, r) => <Tag>{TYPE_LABEL[r.type] || r.type}</Tag>,
    },
    {
      title: '触发',
      dataIndex: 'triggerSource',
      search: false,
      render: (_, r) => <Tag color={r.triggerSource === 'SCHEDULED' ? 'purple' : 'default'}>{r.triggerSource === 'SCHEDULED' ? '定时' : '手动'}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueEnum: { RUNNING: { text: '执行中' }, SUCCESS: { text: '成功' }, FAILED: { text: '失败' } },
      render: (_, r) => taskStatusTag(r.status),
    },
    { title: '信息', dataIndex: 'message', search: false, ellipsis: true },
    { title: '开始时间', dataIndex: 'startedAt', search: false },
    { title: '结束时间', dataIndex: 'finishedAt', search: false },
  ];

  return (
    <PageContainer header={{ title: '任务日志' }}>
      <ProTable<TaskLog>
        rowKey="id"
        actionRef={actionRef}
        columns={columns}
        expandable={{
          expandedRowRender: (r) => (
            <pre style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{r.detail || '无详情'}</pre>
          ),
        }}
        request={async (params) => {
          const res = await pageTaskLogs({
            pageNum: params.current,
            pageSize: params.pageSize,
            certificateId: params.certificateId,
            type: params.type,
            status: params.status,
          });
          return { data: res.records, total: res.total, success: true };
        }}
      />
    </PageContainer>
  );
}
