import request from './request';
import type {
  AcmeAccount,
  Certificate,
  Dashboard,
  DeployTarget,
  DnsProvider,
  PageResult,
  TaskLog,
} from '../types';

// ---------- 认证 ----------
export interface LoginResult {
  token: string;
  userInfo: Record<string, unknown>;
}

export function login(data: { username: string; password: string; tenantCode?: string }) {
  return request.post<unknown, LoginResult>('/api/auth/login', {
    tenantCode: 'default',
    ...data,
  });
}

// ---------- 概览 ----------
export function getDashboard() {
  return request.get<unknown, Dashboard>('/api/cert/dashboard');
}

// ---------- 证书 ----------
export function pageCertificates(params: Record<string, unknown>) {
  return request.get<unknown, PageResult<Certificate>>('/api/cert/certificates/page', { params });
}
export function getCertificate(id: number) {
  return request.get<unknown, Certificate>(`/api/cert/certificates/${id}`);
}
export function issueCertificate(data: Record<string, unknown>) {
  return request.post<unknown, number>('/api/cert/certificates/issue', data);
}
export function renewCertificate(id: number) {
  return request.post<unknown, void>(`/api/cert/certificates/${id}/renew`);
}
export function deployCertificate(id: number) {
  return request.post<unknown, string>(`/api/cert/certificates/${id}/deploy`);
}
export function updateBindings(id: number, targetIds: number[]) {
  return request.put<unknown, void>(`/api/cert/certificates/${id}/bindings`, targetIds);
}
export function exportPem(id: number) {
  return request.get<unknown, Record<string, string>>(`/api/cert/certificates/${id}/pem`);
}
export function deleteCertificate(id: number) {
  return request.delete<unknown, void>(`/api/cert/certificates/${id}`);
}

// ---------- ACME 账户 ----------
export function pageAccounts(params: Record<string, unknown>) {
  return request.get<unknown, PageResult<AcmeAccount>>('/api/cert/accounts/page', { params });
}
export function listAccounts() {
  return request.get<unknown, AcmeAccount[]>('/api/cert/accounts/list');
}
export function saveAccount(data: Record<string, unknown>, id?: number) {
  return id
    ? request.put<unknown, void>(`/api/cert/accounts/${id}`, data)
    : request.post<unknown, void>('/api/cert/accounts', data);
}
export function deleteAccount(id: number) {
  return request.delete<unknown, void>(`/api/cert/accounts/${id}`);
}

// ---------- DNS 服务商 ----------
export function pageDnsProviders(params: Record<string, unknown>) {
  return request.get<unknown, PageResult<DnsProvider>>('/api/cert/dns-providers/page', { params });
}
export function listDnsProviders() {
  return request.get<unknown, DnsProvider[]>('/api/cert/dns-providers/list');
}
export function saveDnsProvider(data: Record<string, unknown>, id?: number) {
  return id
    ? request.put<unknown, void>(`/api/cert/dns-providers/${id}`, data)
    : request.post<unknown, void>('/api/cert/dns-providers', data);
}
export function deleteDnsProvider(id: number) {
  return request.delete<unknown, void>(`/api/cert/dns-providers/${id}`);
}

// ---------- 部署目标 ----------
export function pageDeployTargets(params: Record<string, unknown>) {
  return request.get<unknown, PageResult<DeployTarget>>('/api/cert/deploy-targets/page', { params });
}
export function listDeployTargets() {
  return request.get<unknown, DeployTarget[]>('/api/cert/deploy-targets/list');
}
export function saveDeployTarget(data: Record<string, unknown>, id?: number) {
  return id
    ? request.put<unknown, void>(`/api/cert/deploy-targets/${id}`, data)
    : request.post<unknown, void>('/api/cert/deploy-targets', data);
}
export function testDeployTarget(id: number) {
  return request.post<unknown, void>(`/api/cert/deploy-targets/${id}/test`);
}
export function deleteDeployTarget(id: number) {
  return request.delete<unknown, void>(`/api/cert/deploy-targets/${id}`);
}

// ---------- 任务日志 ----------
export function pageTaskLogs(params: Record<string, unknown>) {
  return request.get<unknown, PageResult<TaskLog>>('/api/cert/task-logs/page', { params });
}
