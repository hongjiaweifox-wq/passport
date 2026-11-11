import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { ManualRecordModal } from './ManualRecordModal';

const mocks = vi.hoisted(() => ({
  createModel: vi.fn(), createUnit: vi.fn(), createEvent: vi.fn(), decodeSn: vi.fn(), fields: vi.fn(),
}));

vi.mock('../api/l3', () => ({ l3Api: {
  createModel: mocks.createModel, createUnit: mocks.createUnit, createEvent: mocks.createEvent,
  decodeSn: mocks.decodeSn,
} }));
vi.mock('../api/l4', () => ({ l4Api: { fields: mocks.fields } }));

afterEach(() => { cleanup(); vi.clearAllMocks(); });
beforeEach(() => { mocks.fields.mockResolvedValue([{ fieldCode: 'manufacturing_place', displayNameZh: '生产地点', implementationStatus: 'GENERIC' }]); });

describe('ManualRecordModal', () => {
  it('locks the type and uses the requested single-record title', async () => {
    render(<ManualRecordModal open initialType="BATTERY_UNIT" lockType onClose={vi.fn()} onCreated={vi.fn()} />);
    expect(await screen.findByText('新增电池单体')).toBeInTheDocument();
    expect(screen.queryByText('记录类型')).not.toBeInTheDocument();
    expect(screen.getByLabelText('序列号 SN')).toBeInTheDocument();
  });

  it('previews decoded SN, creates a unit, and shows autofill result', async () => {
    mocks.decodeSn.mockResolvedValue({ valid: true, productCode: 'A', productName: 'CBE2000', factoryCode: 'C', factoryName: '鹏元晟', cellBrandCode: 'E', cellBrandName: '亿纬锂能', manufacturedDate: '2025-08-04', batchCode: 'SN-AC-20250804', modelMatch: { status: 'MATCHED', modelId: 'm-a', modelCode: 'MODEL-A', revisionId: 'r-1', revisionCode: 'R1' } });
    mocks.createUnit.mockResolvedValue({ id: 'unit-1', autoFilled: { manufactured_at: '2025-08-04T00:00Z', batch_code: 'SN-AC-20250804' }, conflicts: {} });
    render(<ManualRecordModal open onClose={vi.fn()} onCreated={vi.fn()} />);
    expect(await screen.findByText('手动录入单条数据')).toBeInTheDocument();
    const recordTypeSelect = document.querySelector('.ant-modal-body .ant-select-selector');
    expect(recordTypeSelect).not.toBeNull();
    fireEvent.mouseDown(recordTypeSelect!);
    fireEvent.click(await screen.findByText('电池单体 / SN'));
    fireEvent.change(screen.getByLabelText('序列号 SN'), { target: { value: 'CAACBE2508040001' } });
    expect(await screen.findByText('SN 解析成功', {}, { timeout: 2500 })).toBeInTheDocument();
    expect(await screen.findByText(/SN-AC-20250804/)).toBeInTheDocument();
    await waitFor(() => expect(screen.getByDisplayValue('MODEL-A')).toBeInTheDocument());
    await waitFor(() => expect(screen.getByDisplayValue('R1')).toBeInTheDocument());
    const manufacturedTime = document.querySelector('input[placeholder="Select date"]') as HTMLInputElement;
    await waitFor(() => expect(manufacturedTime.value).toContain('2025-08-04'));
    expect(await screen.findByText(/已根据 SN 回填 2025-08-04/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => expect(mocks.createUnit).toHaveBeenCalledWith(expect.objectContaining({ sn: 'CAACBE2508040001', modelCode: 'MODEL-A', revisionCode: 'R1', manufacturedAt: expect.stringContaining('2025-08-04') })));
    expect(await screen.findByText('电池单体已创建')).toBeInTheDocument();
    expect(screen.getByText(/自动回填 2 项/)).toBeInTheDocument();
  });

  it('clearly explains when the parsed SN has no registered model mapping', async () => {
    mocks.decodeSn.mockResolvedValue({ valid: true, productCode: 'C', productName: 'CBE2000 Pro', modelMatch: { status: 'NO_MATCH' } });
    render(<ManualRecordModal open onClose={vi.fn()} onCreated={vi.fn()} />);
    const recordTypeSelect = document.querySelector('.ant-modal-body .ant-select-selector');
    fireEvent.mouseDown(recordTypeSelect!);
    fireEvent.click(await screen.findByText('电池单体 / SN'));
    fireEvent.change(screen.getByLabelText('序列号 SN'), { target: { value: 'CBCACE2509270047' } });
    expect(await screen.findByText('SN 已解析，但尚未配置对应的型号和修订')).toBeInTheDocument();
    expect(screen.getByText(/请先创建并绑定该 SN 产品代码的型号及有效修订/)).toBeInTheDocument();
  });

  it('supports adding and removing DPP extension rows', async () => {
    render(<ManualRecordModal open onClose={vi.fn()} onCreated={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /添加 DPP 属性/ }));
    expect(screen.getByPlaceholderText('属性值')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '删除 DPP 属性' }));
    expect(screen.queryByPlaceholderText('属性值')).not.toBeInTheDocument();
  });

  it('shows a readable message when the API rejects an unknown DPP field', async () => {
    const { LayerApiError } = await import('../api/client');
    mocks.createUnit.mockRejectedValue(new LayerApiError(400, { code: 'INGEST_UNKNOWN_ATTRIBUTE', message: 'unknown', traceId: '' }));
    render(<ManualRecordModal open onClose={vi.fn()} onCreated={vi.fn()} />);
    const recordTypeSelect = document.querySelector('.ant-modal-body .ant-select-selector');
    fireEvent.mouseDown(recordTypeSelect!);
    fireEvent.click(await screen.findByText('电池单体 / SN'));
    fireEvent.change(screen.getByLabelText('序列号 SN'), { target: { value: 'CUSTOM-SN-1' } });
    fireEvent.click(screen.getByRole('button', { name: /保\s*存/ }));
    await waitFor(() => expect(screen.getByText(/DPP 扩展属性编码不在字典中/)).toBeInTheDocument());
  });
});
