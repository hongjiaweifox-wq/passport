import { useEffect, useState } from 'react';
import { ConfigProvider, Layout, Menu, Select, Typography } from 'antd';
import { CloudServerOutlined, DatabaseOutlined, SafetyCertificateOutlined, ApartmentOutlined, AuditOutlined, FileTextOutlined } from '@ant-design/icons';
import { DataSourcePage } from './pages/DataSourcePage';
import { L3DataPage } from './pages/L3DataPage';
import { L4GovernancePage } from './pages/L4GovernancePage';
import { L5PassportPage } from './pages/L5PassportPage';
import { SpecificationsPage } from './pages/SpecificationsPage';
import { PublicPassportPage } from './pages/PublicPassportPage';
import { PublicPassportSummaryPage } from './pages/PublicPassportSummaryPage';
import { PublicAuthPage } from './pages/PublicAuthPage';
import { isPublicAuthenticated } from './pages/publicPassportShared';
import { DEMO_IDENTITIES, setDemoIdentity, useDemoIdentity } from './auth/demoIdentity';
import './styles.css';

type ConsolePage = 'sources' | 'l3' | 'l4' | 'l5' | 'specs';
type Route =
  | { kind: 'console'; page: ConsolePage }
  | { kind: 'public'; code: string; view: 'passport' | 'summary' }
  | { kind: 'auth'; callback: string };

const CONSOLE_PAGES: ConsolePage[] = ['sources', 'l3', 'l4', 'l5', 'specs'];

function route(): Route {
  const hash = window.location.hash.replace(/^#\/?/, '');
  const [path, query = ''] = hash.split('?');
  const parts = path.split('/').filter(Boolean);
  if (parts[0] === 'p' && parts[1]) {
    const code = decodeURIComponent(parts[1]);
    return parts[2] === 'summary' ? { kind: 'public', code, view: 'summary' } : { kind: 'public', code, view: 'passport' };
  }
  if (parts[0] === 'auth') {
    return { kind: 'auth', callback: new URLSearchParams(query).get('callback') ?? '' };
  }
  const page = parts[0];
  return { kind: 'console', page: (CONSOLE_PAGES.includes(page as ConsolePage) ? page : 'sources') as ConsolePage };
}

export function App() {
  const [routeState, setRouteState] = useState<Route>(route);
  const identity = useDemoIdentity();

  useEffect(() => {
    const sync = () => setRouteState(route());
    window.addEventListener('hashchange', sync);
    return () => window.removeEventListener('hashchange', sync);
  }, []);

  useEffect(() => {
    if (routeState.kind === 'public' && routeState.view === 'summary' && !isPublicAuthenticated()) {
      window.location.hash = `/auth?callback=${encodeURIComponent(`/p/${routeState.code}/summary`)}`;
    }
  }, [routeState]);

  const navigate = (key: string) => {
    if (CONSOLE_PAGES.includes(key as ConsolePage)) window.location.hash = `/${key}`;
  };

  if (routeState.kind === 'auth') {
    return (
      <ConfigProvider theme={{ token: { colorPrimary: '#087f8c', borderRadius: 8 } }}>
        <PublicAuthPage callback={routeState.callback} />
      </ConfigProvider>
    );
  }

  if (routeState.kind === 'public') {
    const content = routeState.view === 'summary'
      ? (isPublicAuthenticated()
        ? <PublicPassportSummaryPage publicCode={routeState.code} />
        : <PublicAuthPage callback={`/p/${routeState.code}/summary`} />)
      : <PublicPassportPage publicCode={routeState.code} />;
    return <ConfigProvider theme={{ token: { colorPrimary: '#087f8c', borderRadius: 8 } }}>{content}</ConfigProvider>;
  }

  const safePage = routeState.page;
  const pageLabel: Record<ConsolePage, string> = {
    sources: 'L1 数据源管理', l3: 'L3 结构化主数据', l4: 'L4 数据治理审核', l5: 'L5 护照发布', specs: '定义与规范',
  };

  return (
    <ConfigProvider theme={{ token: { colorPrimary: '#087f8c', borderRadius: 8, fontFamily: "'DM Sans','Noto Sans SC',sans-serif", colorBgLayout: '#f3f7f9' }, components: { Menu: { darkItemBg: '#123444', darkSubMenuItemBg: '#123444', darkItemSelectedBg: '#ffffff18' }, Button: { controlHeight: 36 } } }}>
      <Layout className="app-shell">
        <Layout.Sider width={248} theme="dark" className="app-sider">
          <div className="brand"><span className="brand-mark"><DatabaseOutlined /></span><span className="brand-copy">PASSPORT CLOUD<small>TUYA · DATA PLATFORM</small></span></div>
          <div className="sider-caption">DATA PLATFORM</div>
          <Menu theme="dark" mode="inline" selectedKeys={[safePage]} onClick={({ key }) => navigate(key)} items={[
            { key: 'specs', icon: <FileTextOutlined />, label: '定义与规范' },
            { key: 'sources', icon: <CloudServerOutlined />, label: '数据源登记' },
            { key: 'l3', icon: <ApartmentOutlined />, label: '结构化数据' },
            { key: 'l4', icon: <AuditOutlined />, label: '治理审核' },
            { key: 'l5', icon: <SafetyCertificateOutlined />, label: '护照发布' },
          ]} />
          <div className="sider-note"><b>电池护照数据管理</b>从结构化主数据、治理审核到护照发布的统一管理平台。</div>
        </Layout.Sider>
        <Layout className="app-main">
          <div className="topbar">
            <Typography.Text className="crumb">数据平台　/　{pageLabel[safePage]}</Typography.Text>
            <div className="org-chip">
              <span className="org-dot" />演示组织 <Typography.Text strong>DEMO-ORG</Typography.Text>
              <Select aria-label="演示身份" size="small" value={identity.userId} onChange={(id) => { const found = DEMO_IDENTITIES.find((x) => x.userId === id); if (found) setDemoIdentity(found); }} options={DEMO_IDENTITIES.map((x) => ({ value: x.userId, label: x.label }))} style={{ width: 170, marginLeft: 12 }} />
            </div>
          </div>
          <Layout.Content className="content">
            {safePage === 'sources' ? <DataSourcePage /> : safePage === 'l3' ? <L3DataPage /> : safePage === 'l4' ? <L4GovernancePage /> : safePage === 'l5' ? <L5PassportPage /> : <SpecificationsPage />}
          </Layout.Content>
        </Layout>
      </Layout>
    </ConfigProvider>
  );
}
