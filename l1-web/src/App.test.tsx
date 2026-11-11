import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from './App';
vi.mock('./pages/DataSourcePage',()=>({DataSourcePage:()=> <div>数据源页面内容</div>}));
vi.mock('./pages/L3DataPage',()=>({L3DataPage:()=> <div>L3 主数据页面内容</div>}));
vi.mock('./pages/L4GovernancePage',()=>({L4GovernancePage:()=> <div>L4 治理页面内容</div>}));
vi.mock('./pages/L5PassportPage',()=>({L5PassportPage:()=> <div>L5 护照页面内容</div>}));
vi.mock('./pages/SpecificationsPage',()=>({SpecificationsPage:()=> <div>规范定义页面内容</div>}));
vi.mock('./pages/PublicPassportPage',()=>({PublicPassportPage:({publicCode}:{publicCode:string})=><div>公开护照 {publicCode}</div>}));
afterEach(()=>{cleanup();window.location.hash=''});

describe('App navigation',()=>{
  it('navigates between the remaining management pages without import options',async()=>{
    window.location.hash='#/sources';render(<App/>);
    expect(screen.getByText('数据源页面内容')).toBeInTheDocument();
    expect(screen.queryByRole('menuitem',{name:/导入任务|数据模板/})).not.toBeInTheDocument();
    for(const [name,text,hash] of [['结构化数据','L3 主数据页面内容','#/l3'],['治理审核','L4 治理页面内容','#/l4'],['护照发布','L5 护照页面内容','#/l5'],['定义与规范','规范定义页面内容','#/specs']] as const){
      fireEvent.click(screen.getByRole('menuitem',{name:new RegExp(name)}));
      await waitFor(()=>expect(window.location.hash).toBe(hash));
      expect(await screen.findByText(text)).toBeInTheDocument();
    }
  });
  it('redirects the removed imports route to data source registry',()=>{
    window.location.hash='#/imports';render(<App/>);
    expect(screen.getByText('数据源页面内容')).toBeInTheDocument();
    expect(screen.queryByRole('menuitem',{name:/导入任务/})).not.toBeInTheDocument();
  });
  it('renders the public passport route without the admin shell',()=>{window.location.hash='#/p/abc123';render(<App/>);expect(screen.getByText('公开护照 abc123')).toBeInTheDocument();expect(screen.queryByRole('menu')).not.toBeInTheDocument()});
});
