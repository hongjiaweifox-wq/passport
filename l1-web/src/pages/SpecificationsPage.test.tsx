import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { SpecificationsPage } from './SpecificationsPage';
import { l4Api } from '../api/l4';
const fields = vi.hoisted(() => Array.from({ length: 30 }, (_, i) => ({ id: `f${i}`, dppNo: i + 1, fieldCode: `field_${i}`, valueType: 'STRING', visibilityClass: i === 0 ? 'RESTRICTED' : 'PUBLIC', schemaVersion: '1', displayNameZh: `字段 ${i}`, exampleValue: `示例-${i}`, requirementNotes: i === 0 ? '字段 0 的中文说明' : undefined })));
vi.mock('../api/l4',()=>({l4Api:{fields:vi.fn().mockResolvedValue(fields),updateFieldVisibility:vi.fn().mockResolvedValue({}),coverage:vi.fn().mockResolvedValue({total:92,mandatory:69,publicCount:62,categories:{'DPP header':9},implementationStatus:{GENERIC:85},fields:[]})}}));
vi.mock('../api/l3',()=>({l3Api:{snRule:vi.fn().mockResolvedValue({version:'V7.0',length:16,pattern:'^C([A-F])…$',example:'CAACBE2508040001',segments:[{position:'X1',name:'固定码',rule:'1 位',note:'固定字符 C，代表 CONOW'},{position:'X3',name:'产品机型',rule:'1 位',note:'见产品机型代码表'}],codeTables:[{key:'product',title:'产品机型代码',position:'X3',options:[{code:'A',label:'CBE2000'}]}]})}}));
afterEach(()=>{cleanup();vi.clearAllMocks();});
describe('SpecificationsPage',()=>{
  it('shows merged DPP fields and SN encoding rules on one page',async()=>{
    render(<SpecificationsPage/>);
    expect(await screen.findByText('DPP 属性总数')).toBeInTheDocument();
    expect(screen.getByText('92')).toBeInTheDocument();
    expect(await screen.findByText('field_0')).toBeInTheDocument();
    expect(screen.getAllByText('受限').length).toBeGreaterThan(0);
    expect(screen.getByRole('columnheader', { name: '实例值' })).toBeInTheDocument();
    expect(screen.getByText('示例-0')).toBeInTheDocument();
    fireEvent.click(screen.getByText('SN 编码规则'));
    expect(await screen.findByText('涂鸦储能产品整机 SN 编码规则')).toBeInTheDocument();
    expect(screen.getByText('CAACBE2508040001')).toBeInTheDocument();
    expect(screen.getByText('产品机型代码 · X3')).toBeInTheDocument();
    expect(screen.getByText('CBE2000')).toBeInTheDocument();
  });

  it('applies the selected page size in the pagination control',async()=>{
    render(<SpecificationsPage/>);
    expect(await screen.findByText('field_0')).toBeInTheDocument();
    expect(screen.queryAllByText('field_19')).toHaveLength(0);
    const panels = screen.getAllByRole('tabpanel');
    const panel = panels[panels.length - 1];
    const selector = panel.querySelector('.ant-pagination-options .ant-select-selector');
    expect(selector).not.toBeNull();
    fireEvent.mouseDown(selector!);
    fireEvent.click(await screen.findByText('20 / page'));
    await waitFor(() => expect(screen.getAllByText('field_19').length).toBeGreaterThan(0));
  });

  it('shows Chinese labels for category, visibility and implementation filters',async()=>{
    render(<SpecificationsPage/>);
    expect(await screen.findByText('field_0')).toBeInTheDocument();
    const categoryPlaceholder = screen.getByText('全部类别');
    fireEvent.mouseDown(categoryPlaceholder.closest('.ant-select-selector')!);
    fireEvent.click(await screen.findByText('DPP 标识信息'));
    await waitFor(() => expect(document.querySelector('.ant-select-selection-item[title="DPP 标识信息"]')).not.toBeNull());

    expect(screen.getByText('全部可见范围')).toBeInTheDocument();
    expect(screen.getByText('全部实现状态')).toBeInTheDocument();
    const visibilityPlaceholder = screen.getByText('全部可见范围');
    fireEvent.mouseDown(visibilityPlaceholder.closest('.ant-select-selector')!);
    expect(await screen.findByTitle('公开')).toBeInTheDocument();
  }, 15000);

  it('filters fields by keyword',async()=>{
    render(<SpecificationsPage/>);
    expect(await screen.findByText('field_0')).toBeInTheDocument();
    fireEvent.change(screen.getByPlaceholderText('搜索字段编码或名称'), { target: { value: 'field_29' } });
    await waitFor(() => expect(screen.getByText('field_29')).toBeInTheDocument());
    expect(screen.queryByText('field_0')).not.toBeInTheDocument();
  });

  it('shows the Chinese field note in a popover when clicking the question mark', async () => {
    render(<SpecificationsPage/>);
    expect(await screen.findByText('field_0')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '查看 字段 0 说明' }));
    expect(await screen.findByText('字段 0 的中文说明')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '查看 字段 1 说明' })).not.toBeInTheDocument();
  });

  it('updates the visibility class inline', async () => {
    render(<SpecificationsPage/>);
    expect(await screen.findByText('field_0')).toBeInTheDocument();
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '修改 字段 0 可见范围' }).closest('.ant-select-selector')!);
    fireEvent.click(await screen.findByText('内部'));
    await waitFor(() => expect(l4Api.updateFieldVisibility).toHaveBeenCalledWith('f0', 'INTERNAL'));
    await waitFor(() => expect(screen.getAllByText('内部').length).toBeGreaterThan(0));
  }, 15000);

  it('lets users choose which columns are visible',async()=>{
    render(<SpecificationsPage/>);
    expect(await screen.findByText('field_0')).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: '存储目标' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /列设置/ }));
    fireEvent.click(await screen.findByRole('checkbox', { name: '存储目标' }));
    await waitFor(() => expect(screen.queryByRole('columnheader', { name: '存储目标' })).not.toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: '显示全部列' }));
    await waitFor(() => expect(screen.getByRole('columnheader', { name: '存储目标' })).toBeInTheDocument());
  }, 15000);
});
