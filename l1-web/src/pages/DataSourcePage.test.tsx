import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { DataSourcePage } from './DataSourcePage';

afterEach(cleanup);

const fixtures = vi.hoisted(() => {
  const row = {
    id: 'one', orgCode: 'DEMO-ORG', sourceCode: 'SUPPLIER-PLM', sourceName: '电池制造商 / 供应链', sourceType: 'SUPPLIER',
    ownerTeam: '电池供应链', contact: 'supplier@example.com', dataDomains: ['MODEL', 'UNIT'], deliveryMethod: 'FILE_TEMPLATE',
    frequency: 'ON_CHANGE', authMethod: 'MANUAL', templateTypes: ['BATTERY_MODEL'], status: 'ACTIVE', description: '测试数据源',
    version: 0, createdAt: '2026-11-10T00:00:00Z', updatedAt: '2026-11-10T00:00:00Z',
  };
  const list = { items: [row], total: 1, page: 1, size: 10 };
  const stats = { total: 1, active: 1, draft: 0, suspended: 0 };
  return { row, list, stats };
});

const mocks = vi.hoisted(() => ({ create: vi.fn(), update: vi.fn(), setStatus: vi.fn() }));

vi.mock('../api/dataSources', () => ({
  ApiClientError: class extends Error { body = { message: 'conflict', fieldErrors: {} }; },
  dataSourceApi: {
    list: vi.fn().mockResolvedValue(fixtures.list), summary: vi.fn().mockResolvedValue(fixtures.stats),
    create: mocks.create, update: mocks.update, setStatus: mocks.setStatus, get: vi.fn().mockResolvedValue(fixtures.row),
  },
}));

beforeEach(() => {
  vi.clearAllMocks();
  mocks.create.mockResolvedValue(fixtures.row);
  mocks.update.mockResolvedValue(fixtures.row);
});

describe('DataSourcePage', () => {
  it('shows the loaded registry without template download actions', async () => {
    render(<DataSourcePage />);
    expect(await screen.findByText('电池制造商 / 供应链')).toBeInTheDocument();
    expect(screen.getByText('数据源登记')).toBeInTheDocument();
    expect(screen.getAllByText('已启用').length).toBeGreaterThan(0);
    expect(screen.queryByText('模板')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /下载模板/ })).not.toBeInTheDocument();
  });

  it('does not show import-specific template configuration in source form', async () => {
    render(<DataSourcePage />);
    await screen.findByText('电池制造商 / 供应链');
    fireEvent.click(screen.getAllByRole('button', { name: /登记数据源/ })[0]);
    await waitFor(() => expect(screen.getByText('登记数据源', { selector: '.ant-modal-title' })).toBeInTheDocument());
    expect(screen.queryByText(/授权模板|CSV 模板|导入任务/)).not.toBeInTheDocument();
  }, 15000);

  it('preserves existing template authorization when saving an edited source', async () => {
    render(<DataSourcePage />);
    await screen.findByText('电池制造商 / 供应链');
    fireEvent.click(screen.getByRole('button', { name: '编辑 电池制造商 / 供应链' }));
    await waitFor(() => expect(screen.getByText('编辑数据源', { selector: '.ant-modal-title' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => expect(mocks.update).toHaveBeenCalledWith('one', expect.objectContaining({
      templateTypes: ['BATTERY_MODEL'], status: 'ACTIVE', version: 0,
    })));
  }, 15000);
});
