import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { L3DataPage } from './L3DataPage';
import { L4GovernancePage } from './L4GovernancePage';
import { L5PassportPage } from './L5PassportPage';
const mocks = vi.hoisted(() => ({ createUnit: vi.fn(), createEvent: vi.fn(), decodeSn: vi.fn() }));
vi.mock('../api/l3',()=>({l3Api:{summary:vi.fn().mockResolvedValue({models:1,revisions:1,batteryUnits:1,events:0}),models:vi.fn().mockResolvedValue({items:[{id:'m1',modelCode:'M1',manufacturerName:'Maker',batteryCategory:'EV',revisionCount:1}],total:1}),units:vi.fn().mockResolvedValue({items:[{id:'u1',snRaw:'SN1',snNormalized:'SN1',modelCode:'M1',revisionCode:'R1',status:'ACTIVE',createdAt:'2026-01-01'}],total:1}),unit:vi.fn().mockResolvedValue({unit:{id:'u1',snRaw:'SN1',modelCode:'M1',revisionCode:'R1',status:'ACTIVE'},bindings:[],events:[]}),model:vi.fn().mockResolvedValue({model:{id:'m1',modelCode:'M1',manufacturerName:'Maker',batteryCategory:'EV',revisionCount:1},revisions:[]}),createModel:vi.fn(),appendRevision:vi.fn(),bind:vi.fn(),event:vi.fn(),createUnit:mocks.createUnit,createEvent:mocks.createEvent,decodeSn:mocks.decodeSn}}));
vi.mock('../api/l4',()=>({l4Api:{summary:vi.fn().mockResolvedValue({}),reviews:vi.fn().mockResolvedValue({items:[],total:0}),fields:vi.fn().mockResolvedValue([{id:'f1',fieldCode:'sn',valueType:'STRING',visibilityClass:'RESTRICTED',schemaVersion:'1'}]),coverage:vi.fn().mockResolvedValue({total:92,mandatory:69,publicCount:62,categories:{'DPP header':9},implementationStatus:{GENERIC:85},fields:[]}),validationRuns:vi.fn().mockResolvedValue({items:[],total:0}),validationRun:vi.fn(),validate:vi.fn(),submit:vi.fn()}}));
vi.mock('../api/l5',()=>({l5Api:{summary:vi.fn().mockResolvedValue({total:0}),list:vi.fn().mockResolvedValue({items:[],total:0})}}));
afterEach(()=>{cleanup();vi.clearAllMocks()});
describe('L3-L5 pages',()=>{
  it('renders L3 master data tabs and direct add actions',async()=>{
    render(<L3DataPage/>);
    await waitFor(()=>expect(screen.getByText('M1')).toBeInTheDocument());
    expect(screen.getByText('电池单体 (1)')).toBeInTheDocument();
    expect(screen.getByRole('button',{name:'新增单体'})).toBeInTheDocument();
    expect(screen.getByRole('button',{name:'新增事件'})).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button',{name:'新增单体'}));
    expect(await screen.findByText('新增电池单体')).toBeInTheDocument();
    expect(screen.queryByText('记录类型')).not.toBeInTheDocument();
  });
  it('renders L4 governance and opens the master data supplement entry',async()=>{
    render(<L4GovernancePage/>);
    await waitFor(()=>expect(screen.getByText('审核队列')).toBeInTheDocument());
    expect(screen.queryByText('DPP 完整性')).not.toBeInTheDocument();
    fireEvent.click(screen.getByText('校验记录'));
    await waitFor(()=>expect(screen.getByText('暂无校验记录')).toBeInTheDocument());
    expect(screen.queryByText('字段字典')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button',{name:/补录主数据/}));
    expect(await screen.findByText('手动录入单条数据')).toBeInTheDocument();
    expect(screen.getByText('记录类型')).toBeInTheDocument();
  }, 15000);
  it('renders L5 passport summary and empty list',async()=>{render(<L5PassportPage/>);await waitFor(()=>expect(screen.getByText('暂无护照')).toBeInTheDocument());expect(screen.getByText('护照发布')).toBeInTheDocument()});
});
