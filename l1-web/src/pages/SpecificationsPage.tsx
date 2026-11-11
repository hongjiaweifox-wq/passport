import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Checkbox, Empty, Input, Popover, Select, Space, Table, Tabs, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { SearchOutlined, SettingOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { l4Api, type CoverageView, type FieldDefinition } from '../api/l4';
import { l3Api, type SnRule } from '../api/l3';
import './LayerPages.css';

const CATEGORY_LABELS: Record<string, string> = {
  'DPP header': 'DPP 标识信息',
  'Identifiers and product data': '标识与产品数据',
  'Battery materials and composition': '电池材料与成分',
  'Battery carbon footprint': '电池碳足迹',
  'Performance and durability': '性能与耐久性',
  'Supply chain due diligence': '供应链尽职调查',
  'Circularity and resource efficiency': '循环性与资源效率',
  'Symbols, labels and documentation of conformity': '符号、标签与合规文件',
  'Other': '其他',
};
const VISIBILITY_LABELS: Record<string, string> = {
  PUBLIC: '公开',
  INTERNAL: '内部',
  REGULATOR: '监管方',
  RESTRICTED: '受限',
};
const IMPLEMENTATION_LABELS: Record<string, string> = {
  GENERATED: '生成',
  GENERIC: '通用',
  NATIVE: '原生',
};

const categoryLabel = (value?: string) => value ? (CATEGORY_LABELS[value] ?? value) : '—';
const implementationLabel = (value?: string) => value ? (IMPLEMENTATION_LABELS[value] ?? value) : '—';

const VISIBILITY_OPTIONS = Object.entries(VISIBILITY_LABELS).map(([value, label]) => ({ value, label }));
const visibilityColor = (value?: string) => value === 'PUBLIC' ? 'green' : value === 'INTERNAL' ? 'default' : value === 'REGULATOR' ? 'purple' : 'blue';

const FIELD_COLUMNS: TableColumnsType<FieldDefinition> = [
  { key: 'dppNo', title: '编号', dataIndex: 'dppNo', width: '5%' },
  { key: 'fieldCode', title: '字段编码', dataIndex: 'fieldCode', width: '12%' },
  { key: 'displayNameZh', title: '字段名称', dataIndex: 'displayNameZh', width: '9%' },
  { key: 'exampleValue', title: '实例值', dataIndex: 'exampleValue', width: '12%', render: (v, record) => <div className="field-example">
    <span className="field-example-value">{v || '—'}</span>
    {record.requirementNotes && <Popover trigger="click" placement="bottomLeft" title="字段说明" content={<div className="field-note-popover">{record.requirementNotes}</div>}>
      <Button type="text" size="small" className="field-note-trigger" aria-label={`查看 ${record.displayNameZh ?? record.fieldCode} 说明`} icon={<QuestionCircleOutlined />} />
    </Popover>}
  </div> },
  { key: 'category', title: '类别', dataIndex: 'category', width: '10%', render: v => categoryLabel(v) },
  { key: 'valueType', title: '类型', dataIndex: 'valueType', width: '7%' },
  { key: 'unitCode', title: '单位', dataIndex: 'unitCode', width: '6%' },
  { key: 'mandatoryStationary', title: '必填', dataIndex: 'mandatoryStationary', width: '6%', render: v => v ? <Tag color="red">必填</Tag> : '—' },
  { key: 'implementationStatus', title: '实现', dataIndex: 'implementationStatus', width: '7%', render: v => <Tag>{implementationLabel(v)}</Tag> },
  { key: 'storageTarget', title: '存储目标', dataIndex: 'storageTarget', width: '7%' },
  { key: 'visibilityClass', title: '可见范围', dataIndex: 'visibilityClass', width: '10%' },
  { key: 'legalReference', title: '法规依据', dataIndex: 'legalReference', width: '9%' },
];
const ALL_FIELD_KEYS = FIELD_COLUMNS.map(column => String(column.key));

function DppFieldsPanel({ coverage, fields, loading, onVisibilityChange }: { coverage?: CoverageView; fields: FieldDefinition[]; loading: boolean; onVisibilityChange: (field: FieldDefinition, visibility: string) => void }) {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState<string>();
  const [visibility, setVisibility] = useState<string>();
  const [mandatory, setMandatory] = useState('ALL');
  const [status, setStatus] = useState<string>();
  const [visibleKeys, setVisibleKeys] = useState<string[]>(ALL_FIELD_KEYS);

  const columns = useMemo(() => FIELD_COLUMNS.filter(column => visibleKeys.includes(String(column.key))).map(column => column.key === 'visibilityClass' ? {
    ...column,
    render: (value: string, record: FieldDefinition) => <Select
      size="small" value={value} className="visibility-select" popupMatchSelectWidth={false}
      aria-label={`修改 ${record.displayNameZh ?? record.fieldCode} 可见范围`}
      options={VISIBILITY_OPTIONS.map(option => ({ value: option.value, label: <Tag color={visibilityColor(option.value)}>{option.label}</Tag> }))}
      onChange={next => onVisibilityChange(record, next)}
    />,
  } : column), [visibleKeys, onVisibilityChange]);

  const filtered = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return fields.filter(field => {
      if (keyword && !`${field.fieldCode} ${field.displayNameZh ?? ''}`.toLowerCase().includes(keyword)) return false;
      if (category && field.category !== category) return false;
      if (visibility && field.visibilityClass !== visibility) return false;
      if (mandatory !== 'ALL' && String(Boolean(field.mandatoryStationary)) !== mandatory) return false;
      if (status && field.implementationStatus !== status) return false;
      return true;
    }).sort((a, b) => {
      if (a.dppNo != null && b.dppNo != null) return a.dppNo - b.dppNo;
      if (a.dppNo != null) return -1;
      if (b.dppNo != null) return 1;
      return a.fieldCode.localeCompare(b.fieldCode);
    });
  }, [fields, query, category, visibility, mandatory, status]);

  const reset = () => { setQuery(''); setCategory(undefined); setVisibility(undefined); setMandatory('ALL'); setStatus(undefined); };

  return <>
    <div className="spec-stats">
      <div className="spec-stat"><span>DPP 属性总数</span><b>{coverage?.total ?? 0}</b></div>
      <div className="spec-stat"><span>固定式储能 (&gt;2kWh) 必填字段</span><b>{coverage?.mandatory ?? 0}</b></div>
      <div className="spec-stat"><span>公开可见字段 (PUBLIC)</span><b>{coverage?.publicCount ?? 0}</b></div>
    </div>
    <Space wrap className="filter-bar spec-filter-bar">
      <Input value={query} onChange={e => setQuery(e.target.value)} prefix={<SearchOutlined />} placeholder="搜索字段编码或名称" allowClear style={{ width: 200 }} />
      <Select value={category} onChange={setCategory} placeholder="全部类别" allowClear optionFilterProp="label" style={{ width: 190 }} options={Object.keys(coverage?.categories ?? {}).map(value => ({ value, label: categoryLabel(value) }))} />
      <Select value={visibility} onChange={setVisibility} placeholder="全部可见范围" allowClear style={{ width: 140 }} options={VISIBILITY_OPTIONS} />
      <Select value={mandatory} onChange={setMandatory} style={{ width: 130 }} options={[{ value: 'ALL', label: '全部必填状态' }, { value: 'true', label: '必填' }, { value: 'false', label: '非必填' }]} />
      <Select value={status} onChange={setStatus} placeholder="全部实现状态" allowClear style={{ width: 150 }} options={Object.keys(coverage?.implementationStatus ?? {}).map(value => ({ value, label: implementationLabel(value) }))} />
      <Button onClick={reset}>重置</Button>
      <Typography.Text type="secondary">共 {filtered.length} 个字段</Typography.Text>
      <Popover trigger="click" placement="bottomRight" title="选择展示列" content={<div style={{ width: 180 }}>
        <Button type="link" size="small" style={{ padding: 0, marginBottom: 8 }} onClick={() => setVisibleKeys(ALL_FIELD_KEYS)}>显示全部列</Button>
        <Checkbox.Group
          value={visibleKeys}
          onChange={values => { if (values.length > 0) setVisibleKeys(values as string[]); }}
          style={{ display: 'flex', flexDirection: 'column', gap: 8 }}
          options={FIELD_COLUMNS.map(column => ({ label: String(column.title), value: String(column.key) }))}
        />
      </div>}>
        <Button icon={<SettingOutlined />}>列设置</Button>
      </Popover>
    </Space>
    <Table
      className="spec-fields-table" tableLayout="fixed" size="small" rowKey="id" dataSource={filtered} loading={loading} columns={columns}
      pagination={{ defaultPageSize: 15, showSizeChanger: true, showTotal: total => `共 ${total} 个字段` }}
      locale={{ emptyText: <Empty description="没有匹配的字段" /> }}
    />
  </>;
}

function SnRulePanel({ rule, loading, error }: { rule?: SnRule; loading: boolean; error: string }) {
  if (error) return <Alert type="error" showIcon message="SN 编码规则加载失败" description={error} />;
  if (!rule && !loading) return <Empty description="暂无 SN 编码规则" />;
  return <>
    <Typography.Title level={4}>涂鸦储能产品整机 SN 编码规则</Typography.Title>
    <Typography.Paragraph type="secondary">16 位整机 SN，规则版本 {rule?.version ?? '—'}。示例：<Typography.Text code>{rule?.example ?? '—'}</Typography.Text></Typography.Paragraph>
    <Alert type="info" showIcon message="编码结构" description={<Space direction="vertical" size={4}><span>长度：{rule?.length ?? '—'} 位</span><Typography.Text code copyable style={{ wordBreak: 'break-all' }}>{rule?.pattern ?? '—'}</Typography.Text></Space>} style={{ marginBottom: 16 }} />
    <Table rowKey="position" size="small" pagination={false} loading={loading} dataSource={rule?.segments ?? []} columns={[{ title: '位置', dataIndex: 'position', width: 110 }, { title: '字段', dataIndex: 'name', width: 130 }, { title: '规则', dataIndex: 'rule', width: 170 }, { title: '说明', dataIndex: 'note' }]} />
    <Typography.Title level={5} style={{ marginTop: 24 }}>代码表</Typography.Title>
    <div className="spec-code-tables">
      {(rule?.codeTables ?? []).map(table => <Card key={table.key} size="small" title={`${table.title} · ${table.position}`}>
        <Table rowKey="code" size="small" pagination={false} dataSource={table.options} columns={[{ title: '代码', dataIndex: 'code', width: 90 }, { title: '含义', dataIndex: 'label' }]} />
      </Card>)}
    </div>
  </>;
}

export function SpecificationsPage() {
  const [fields, setFields] = useState<FieldDefinition[]>([]);
  const [coverage, setCoverage] = useState<CoverageView>();
  const [snRule, setSnRule] = useState<SnRule>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [snError, setSnError] = useState('');

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.allSettled([l4Api.fields(), l4Api.coverage(), l3Api.snRule()])
      .then(([f, c, r]) => {
        if (cancelled) return;
        if (f.status === 'fulfilled') setFields(f.value);
        if (c.status === 'fulfilled') setCoverage(c.value);
        if (r.status === 'fulfilled') setSnRule(r.value);
        const dppErrors = [f, c].filter(x => x.status === 'rejected').map(x => x.reason instanceof Error ? x.reason.message : '加载失败');
        setError(dppErrors.length ? `DPP 字段加载失败：${dppErrors.join('；')}` : '');
        setSnError(r.status === 'rejected' ? `${r.reason instanceof Error ? r.reason.message : '加载失败'}；请确认后端已重启并包含 /api/v1/l3/sn-rule 接口` : '');
      })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const changeVisibility = async (field: FieldDefinition, visibility: string) => {
    const previous = fields;
    const next = fields.map(item => item.id === field.id ? { ...item, visibilityClass: visibility as FieldDefinition['visibilityClass'] } : item);
    setFields(next);
    setCoverage(current => current ? { ...current, publicCount: next.filter(item => item.visibilityClass === 'PUBLIC').length } : current);
    try {
      await l4Api.updateFieldVisibility(field.id, visibility);
      setError('');
    } catch (e) {
      setFields(previous);
      setCoverage(current => current ? { ...current, publicCount: previous.filter(item => item.visibilityClass === 'PUBLIC').length } : current);
      setError(`可见范围更新失败：${e instanceof Error ? e.message : '请稍后重试'}`);
    }
  };

  return <div className="data-page layer-page spec-page">
    <div className="page-heading"><div><Typography.Text className="eyebrow">DEFINITIONS · SPECIFICATIONS</Typography.Text><Typography.Title level={2}>定义与规范</Typography.Title><Typography.Paragraph type="secondary">集中查看 DPP 字段完整性、字段字典与整机 SN 编码规则，作为数据录入与治理的统一依据。</Typography.Paragraph></div></div>
    {error && <Alert className="load-error" type="error" showIcon message={error} />}
    <Card className="registry-card"><Tabs items={[
      { key: 'fields', label: 'DPP 字段', children: <DppFieldsPanel coverage={coverage} fields={fields} loading={loading} onVisibilityChange={(field, visibility) => { void changeVisibility(field, visibility); }} /> },
      { key: 'sn', label: 'SN 编码规则', children: <SnRulePanel rule={snRule} loading={loading} error={snError} /> },
    ]} /></Card>
  </div>;
}
