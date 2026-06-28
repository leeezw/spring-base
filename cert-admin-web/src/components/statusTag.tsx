import { Tag } from 'antd';

const CERT_STATUS: Record<string, { color: string; text: string }> = {
  PENDING: { color: 'default', text: '待签发' },
  ISSUING: { color: 'processing', text: '签发中' },
  ACTIVE: { color: 'success', text: '有效' },
  EXPIRING: { color: 'warning', text: '临期' },
  FAILED: { color: 'error', text: '失败' },
};

const TASK_STATUS: Record<string, { color: string; text: string }> = {
  RUNNING: { color: 'processing', text: '执行中' },
  SUCCESS: { color: 'success', text: '成功' },
  FAILED: { color: 'error', text: '失败' },
};

export function statusTag(status: string) {
  const s = CERT_STATUS[status] || { color: 'default', text: status };
  return <Tag color={s.color}>{s.text}</Tag>;
}

export function taskStatusTag(status: string) {
  const s = TASK_STATUS[status] || { color: 'default', text: status };
  return <Tag color={s.color}>{s.text}</Tag>;
}
