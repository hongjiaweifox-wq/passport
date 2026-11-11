import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Col, Empty, Input, message, Popconfirm, Row, Select, Skeleton, Space, Statistic, Table, Tag, Tooltip, Typography } from 'antd';
import type { TableProps } from 'antd';
import { CloudServerOutlined, EditOutlined, EyeOutlined, PauseCircleOutlined, PlayCircleOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, DatabaseOutlined } from '@ant-design/icons';
import type { DataSource, DataSourceInput, SourceStatus } from '../types/dataSource';
import { ApiClientError, dataSourceApi } from '../api/dataSources';
import { DataSourceFormModal } from '../components/DataSourceFormModal';
import { DataSourceDetailDrawer } from '../components/DataSourceDetailDrawer';
import { deliveryOptions, frequencyOptions, labelFor, sourceTypeOptions, statusOptions } from '../constants/options';
import './DataSourcePage.css';

export function DataSourcePage() {
  const [messageApi, contextHolder] = message.useMessage();
  const [rows, setRows] = useState<DataSource[]>([]);
  const [summary, setSummary] = useState({ total: 0, active: 0, draft: 0, suspended: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [sourceType, setSourceType] = useState<string>();
  const [status, setStatus] = useState<string>();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<DataSource>();
  const [detail, setDetail] = useState<DataSource>();
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [result, stats] = await Promise.all([
        dataSourceApi.list({ q: appliedQuery, type: sourceType as never, status: status as never, page, size: pageSize }),
        dataSourceApi.summary(),
      ]);
      setRows(result.items); setTotal(result.total); setSummary(stats);
    } catch (e) {
      const err = e instanceof Error ? e.message : '加载数据失败';
      setError(err);
    } finally { setLoading(false); }
  }, [appliedQuery, sourceType, status, page, pageSize]);
  useEffect(() => { void load(); }, [load]);

  const openCreate = () => { setEditing(undefined); setFormOpen(true); };
  const openEdit = (source: DataSource) => { setEditing(source); setFormOpen(true); };
  const save = async (input: DataSourceInput) => {
    setSaving(true);
    try {
      if (editing) await dataSourceApi.update(editing.id, { ...input, version: editing.version });
      else await dataSourceApi.create(input);
      messageApi.success(editing ? '数据源已更新' : '数据源已登记');
      setFormOpen(false); setEditing(undefined); await load();
    } catch (e) {
      if (e instanceof ApiClientError && e.body.fieldErrors && Object.keys(e.body.fieldErrors).length) {
        messageApi.error(Object.values(e.body.fieldErrors).join('；'));
      } else messageApi.error(e instanceof Error ? e.message : '保存失败');
    } finally { setSaving(false); }
  };
  const setSourceStatus = async (source: DataSource, next: SourceStatus) => {
    try { await dataSourceApi.setStatus(source, next); messageApi.success(next === 'ACTIVE' ? '数据源已启用' : '数据源已暂停'); await load(); }
    catch (e) { messageApi.error(e instanceof Error ? e.message : '状态更新失败'); }
  };

  const columns = useMemo<TableProps<DataSource>['columns']>(() => [
    { title: '数据源', key: 'source', width: 245, render: (_, row) => <div className="source-cell"><span className="source-icon"><CloudServerOutlined /></span><span><Typography.Text strong>{row.sourceName}</Typography.Text><Typography.Text type="secondary" className="source-code">{row.sourceCode}</Typography.Text></span></div> },
    { title: '来源类型', dataIndex: 'sourceType', width: 150, render: (value: string) => labelFor(sourceTypeOptions, value) },
    { title: '责任团队', dataIndex: 'ownerTeam', width: 140 },
    { title: '交付方式', dataIndex: 'deliveryMethod', width: 130, render: (value: string) => labelFor(deliveryOptions, value) },
    { title: '更新频率', dataIndex: 'frequency', width: 100, render: (value: string) => labelFor(frequencyOptions, value) },
    { title: '数据域', key: 'domains', width: 180, render: (_, row) => <Space size={[0, 4]} wrap>{row.dataDomains.slice(0, 2).map((domain) => <Tag key={domain}>{domain}</Tag>)}{row.dataDomains.length > 2 && <Tooltip title={row.dataDomains.slice(2).join(', ')}><Tag>+{row.dataDomains.length - 2}</Tag></Tooltip>}</Space> },
    { title: '状态', dataIndex: 'status', width: 92, render: (value: string) => { const option = statusOptions.find((item) => item.value === value); return <Tag color={option?.color}>{option?.label ?? value}</Tag>; } },
    { title: '更新时间', dataIndex: 'updatedAt', width: 150, render: (value: string) => new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) },
    { title: '操作', key: 'actions', fixed: 'right', width: 155, render: (_, row) => <Space size={2}>
      <Tooltip title="查看详情"><Button type="text" icon={<EyeOutlined />} onClick={() => setDetail(row)} aria-label={`查看 ${row.sourceName} 详情`} /></Tooltip>
      <Tooltip title="编辑"><Button type="text" icon={<EditOutlined />} onClick={() => openEdit(row)} aria-label={`编辑 ${row.sourceName}`} /></Tooltip>
      {row.status === 'ACTIVE' ? <Popconfirm title="暂停该数据源？" description="暂停后，此来源会被标记为不可用。" okText="暂停" cancelText="取消" onConfirm={() => void setSourceStatus(row, 'SUSPENDED')}><Button type="text" danger icon={<PauseCircleOutlined />} aria-label={`暂停 ${row.sourceName}`} /></Popconfirm> : <Popconfirm title="启用该数据源？" okText="启用" cancelText="取消" onConfirm={() => void setSourceStatus(row, 'ACTIVE')}><Button type="text" icon={<PlayCircleOutlined />} aria-label={`启用 ${row.sourceName}`} /></Popconfirm>}
    </Space> },
  ], [messageApi]);

  const handleSearch = () => { setPage(1); setAppliedQuery(query.trim()); };
  const clearFilters = () => { setQuery(''); setAppliedQuery(''); setSourceType(undefined); setStatus(undefined); setPage(1); };

  return <div className="data-page">
    {contextHolder}
    <div className="page-heading"><div><Typography.Text className="eyebrow">L1 · DATA SOURCE REGISTRY</Typography.Text><Typography.Title level={2}>数据源登记</Typography.Title><Typography.Paragraph type="secondary">登记电池护照数据的提供方、责任团队与交付方式。</Typography.Paragraph></div><Button type="primary" size="large" icon={<PlusOutlined />} onClick={openCreate}>登记数据源</Button></div>
    <Row gutter={[14, 14]} className="summary-row">
      <Col xs={12} md={6}><Card className="stat-card"><Statistic title="数据源总数" value={summary.total} prefix={<DatabaseOutlined />} /></Card></Col>
      <Col xs={12} md={6}><Card className="stat-card"><Statistic title="已启用" value={summary.active} valueStyle={{ color: '#17805d' }} prefix={<PlayCircleOutlined />} /></Card></Col>
      <Col xs={12} md={6}><Card className="stat-card"><Statistic title="待完善" value={summary.draft} valueStyle={{ color: '#bc7b16' }} /></Card></Col>
      <Col xs={12} md={6}><Card className="stat-card"><Statistic title="已暂停" value={summary.suspended} valueStyle={{ color: '#667985' }} prefix={<PauseCircleOutlined />} /></Card></Col>
    </Row>
    <Card className="registry-card" variant="borderless">
      <div className="filter-bar">
        <Input allowClear prefix={<SearchOutlined />} placeholder="搜索名称、代码或责任团队" value={query} onChange={(event) => setQuery(event.target.value)} onPressEnter={handleSearch} className="search-input" />
        <Select allowClear placeholder="全部来源类型" value={sourceType} onChange={(value) => { setSourceType(value); setPage(1); }} options={sourceTypeOptions} className="filter-select" />
        <Select allowClear placeholder="全部状态" value={status} onChange={(value) => { setStatus(value); setPage(1); }} options={statusOptions.map(({ value, label }) => ({ value, label }))} className="filter-select small" />
        <Button type="primary" onClick={handleSearch}>查询</Button><Button onClick={clearFilters}>重置</Button><Button icon={<ReloadOutlined />} onClick={() => void load()} aria-label="刷新" />
      </div>
      {error && <Alert type="error" showIcon message="数据加载失败" description={error} action={<Button size="small" onClick={() => void load()}>重试</Button>} className="load-error" />}
      {loading && rows.length === 0 ? <Skeleton active paragraph={{ rows: 7 }} /> : <Table<DataSource>
        rowKey="id" columns={columns} dataSource={rows} loading={loading} scroll={{ x: 1700 }} size="middle"
        locale={{ emptyText: error ? <Empty description="暂时无法加载数据" /> : <Empty description="还没有数据源，登记一个数据源开始构建护照数据链路" /> }}
        pagination={{ current: page, pageSize, total, showSizeChanger: true, showTotal: (count) => `共 ${count} 个数据源`, onChange: (nextPage, nextSize) => { setPage(nextPage); setPageSize(nextSize); } }}
      />}
    </Card>
    <div className="page-footnote"><DatabaseOutlined /> 数据登记信息保存于本地数据云数据库；原型默认组织：DEMO-ORG。<Typography.Link href="/方案/L1-数据源与接入规范.html" target="_blank">查看 L1 数据规范</Typography.Link></div>
    <DataSourceFormModal key={editing?.id ?? 'new'} open={formOpen} initial={editing ? {
      sourceCode: editing.sourceCode, sourceName: editing.sourceName, sourceType: editing.sourceType,
      ownerTeam: editing.ownerTeam, contact: editing.contact ?? undefined, dataDomains: editing.dataDomains,
      deliveryMethod: editing.deliveryMethod, frequency: editing.frequency, authMethod: editing.authMethod,
      templateTypes: editing.templateTypes, status: editing.status, description: editing.description ?? undefined,
      version: editing.version,
    } : undefined} loading={saving} onCancel={() => { setFormOpen(false); setEditing(undefined); }} onSubmit={save} />
    <DataSourceDetailDrawer source={detail} open={Boolean(detail)} onClose={() => setDetail(undefined)} />
  </div>;
}
