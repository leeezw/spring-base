import { PageContainer, ProCard, StatisticCard } from '@ant-design/pro-components';
import { Table, Tag } from 'antd';
import { useEffect, useState } from 'react';
import { getDashboard } from '../api/cert';
import type { Dashboard as DashboardData } from '../types';
import { statusTag } from '../components/statusTag';

export default function Dashboard() {
  const [data, setData] = useState<DashboardData>();

  useEffect(() => {
    getDashboard().then(setData);
  }, []);

  return (
    <PageContainer header={{ title: '概览' }}>
      <ProCard gutter={16} ghost>
        <StatisticCard colSpan={6} statistic={{ title: '证书总数', value: data?.total ?? 0 }} />
        <StatisticCard colSpan={6} statistic={{ title: '有效', value: data?.active ?? 0, valueStyle: { color: '#52c41a' } }} />
        <StatisticCard colSpan={6} statistic={{ title: '临期', value: data?.expiring ?? 0, valueStyle: { color: '#faad14' } }} />
        <StatisticCard colSpan={6} statistic={{ title: '失败', value: data?.failed ?? 0, valueStyle: { color: '#ff4d4f' } }} />
      </ProCard>

      <ProCard title="临期证书" style={{ marginTop: 16 }}>
        <Table
          rowKey="id"
          pagination={false}
          dataSource={data?.expiringList ?? []}
          columns={[
            { title: '主域名', dataIndex: 'primaryDomain' },
            { title: '状态', dataIndex: 'status', render: (s) => statusTag(s) },
            {
              title: '剩余天数',
              dataIndex: 'daysRemaining',
              render: (d: number) => <Tag color={d <= 7 ? 'red' : 'orange'}>{d} 天</Tag>,
            },
            { title: '到期时间', dataIndex: 'notAfter' },
          ]}
        />
      </ProCard>
    </PageContainer>
  );
}
