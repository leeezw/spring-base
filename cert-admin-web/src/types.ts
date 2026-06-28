// 与后端 DTO 对应的类型定义

export interface ResultWrapper<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

export interface AcmeAccount {
  id: number;
  name: string;
  caType: string;
  email: string;
  directoryUrl: string;
  registered: boolean;
  status: number;
  remark?: string;
  createTime?: string;
}

export interface DnsProvider {
  id: number;
  name: string;
  type: string;
  status: number;
  remark?: string;
  createTime?: string;
}

export interface DeployTarget {
  id: number;
  name: string;
  type: string;
  enabled: number;
  remark?: string;
  createTime?: string;
}

export interface Certificate {
  id: number;
  primaryDomain: string;
  sanDomains?: string[];
  challengeType: string;
  acmeAccountId: number;
  dnsProviderId: number;
  keyAlgo: string;
  status: string;
  notBefore?: string;
  notAfter?: string;
  serial?: string;
  issuer?: string;
  autoRenew: number;
  renewBeforeDays: number;
  lastRenewAt?: string;
  lastMessage?: string;
  createTime?: string;
  daysRemaining?: number;
  deployTargetIds?: number[];
}

export interface TaskLog {
  id: number;
  certificateId?: number;
  type: string;
  triggerSource: string;
  status: string;
  message?: string;
  detail?: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface Dashboard {
  total: number;
  active: number;
  expiring: number;
  failed: number;
  expiringList: Certificate[];
}
