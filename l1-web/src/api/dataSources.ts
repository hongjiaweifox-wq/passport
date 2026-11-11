import type { DataSourceInput, PageResult, Summary, DataSource, SourceStatus, SourceType } from '../types/dataSource';
import type { ApiErrorBody } from '../types/common';

const BASE = '/api/v1/l1';
const ORG_CODE = import.meta.env.VITE_ORG_CODE || 'DEMO-ORG';

export class ApiClientError extends Error {
  constructor(readonly status: number, readonly body: ApiErrorBody) { super(body.message); }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', 'X-Org-Code': ORG_CODE, ...init?.headers },
  });
  if (!response.ok) {
    let body: ApiErrorBody;
    try { body = await response.json() as ApiErrorBody; }
    catch { body = { code: 'L1_NETWORK_ERROR', message: `Request failed (${response.status})`, traceId: '' }; }
    throw new ApiClientError(response.status, body);
  }
  const payload = await response.json() as T | { data: T; traceId: string };
  return typeof payload === 'object' && payload !== null && 'data' in payload ? (payload as { data: T }).data : payload as T;
}

export interface DataSourceFilters { q?: string; type?: SourceType; status?: SourceStatus; page?: number; size?: number }
export const dataSourceApi = {
  list: (filters: DataSourceFilters = {}) => {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && value !== '') params.set(key, String(value)); });
    return request<PageResult<DataSource>>(`/data-sources?${params.toString()}`);
  },
  summary: () => request<Summary>('/data-sources/summary'),
  get: (id: string) => request<DataSource>(`/data-sources/${encodeURIComponent(id)}`),
  create: (input: DataSourceInput) => request<DataSource>('/data-sources', { method: 'POST', body: JSON.stringify(input) }),
  update: (id: string, input: DataSourceInput) => request<DataSource>(`/data-sources/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(input) }),
  setStatus: (source: DataSource, status: SourceStatus) => request<DataSource>(`/data-sources/${encodeURIComponent(source.id)}/status`, {
    method: 'PATCH', body: JSON.stringify({ status, version: source.version }),
  }),
};
