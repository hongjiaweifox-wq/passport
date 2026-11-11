import { useEffect, useState } from 'react';
import { Alert, Button, Card, DatePicker, Descriptions, Drawer, Empty, Form, Input, InputNumber, message, Modal, Row, Col, Select, Space, Statistic, Table, Tabs, Tag, Typography } from 'antd';
import { ReloadOutlined, SearchOutlined, PlusOutlined } from '@ant-design/icons';
import type { TableProps } from 'antd';
import { l3Api } from '../api/l3';
import { l4Api, type ValidationResult } from '../api/l4';
import { ManualRecordModal } from '../components/ManualRecordModal';
import type { ModelDetail, ModelView, UnitDetail, UnitView } from '../api/l3';
import './LayerPages.css';

type ModelFormValues = {
  modelCode: string;
  snProductCode?: string;
  manufacturerName: string;
  batteryCategory: string;
  revisionCode: string;
  chemistry: string;
  ratedCapacityKwh: number;
  ratedVoltageV: number;
  effectiveFrom: { format: (pattern: string) => string };
};

export function L3DataPage() {
  const [msg, holder] = message.useMessage();
  const [summary, setSummary] = useState<Record<string, number>>({});
  const [models, setModels] = useState<ModelView[]>([]);
  const [units, setUnits] = useState<UnitView[]>([]);
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState('');
  const [detail, setDetail] = useState<UnitDetail>();
  const [modelDetail, setModelDetail] = useState<ModelDetail>();
  const [validation, setValidation] = useState<ValidationResult>();
  const [modelModal, setModelModal] = useState(false);
  const [revisionModel, setRevisionModel] = useState<ModelView>();
  const [unitAction, setUnitAction] = useState<'binding' | 'event'>();
  const [manualType, setManualType] = useState<'BATTERY_UNIT' | 'LIFECYCLE_EVENT'>();
  const [modelForm] = Form.useForm<ModelFormValues>();
  const [unitForm] = Form.useForm<Record<string, unknown>>();

  const load = async () => {
    setLoading(true);
    try {
      const [s, m, u] = await Promise.all([l3Api.summary(), l3Api.models(query), l3Api.units(query)]);
      setSummary(s);
      setModels(m.items);
      setUnits(u.items);
    } catch (e) {
      msg.error(e instanceof Error ? e.message : '加载 L3 数据失败');
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { void load(); }, []);

  const showModel = async (id: string) => {
    try { setModelDetail(await l3Api.model(id)); }
    catch (e) { msg.error(e instanceof Error ? e.message : '型号详情加载失败'); }
  };
  const showUnit = async (id: string) => {
    try { setDetail(await l3Api.unit(id)); setValidation(undefined); }
    catch (e) { msg.error(e instanceof Error ? e.message : '单体详情加载失败'); }
  };
  const validate = async (type: string, id: string) => {
    try {
      const value = await l4Api.validate(type, id);
      if (type === 'BATTERY_UNIT') setValidation(value);
      msg.success(`校验结果：${value.status}`);
    } catch (e) { msg.error(e instanceof Error ? e.message : '校验失败'); }
  };
  const submit = async (type: string, id: string) => {
    try { const task = await l4Api.submit(type, id); msg.success(`已提交审核：${task.id}`); }
    catch (e) { msg.error(e instanceof Error ? e.message : '提交审核失败'); }
  };
  const saveModel = async (values: ModelFormValues) => {
    try {
      const payload = { ...values, effectiveFrom: values.effectiveFrom.format('YYYY-MM-DD') };
      if (revisionModel) await l3Api.appendRevision(revisionModel.id, payload);
      else await l3Api.createModel(payload);
      msg.success(revisionModel ? '型号修订已追加' : '型号与初始修订已创建');
      setModelModal(false);
      setRevisionModel(undefined);
      modelForm.resetFields();
      await load();
      if (revisionModel) await showModel(revisionModel.id);
    } catch (e) { msg.error(e instanceof Error ? e.message : '保存型号失败'); }
  };
  const saveUnitAction = async (values: Record<string, unknown>) => {
    if (!detail || !unitAction) return;
    try {
      if (unitAction === 'binding') {
        await l3Api.bind(detail.unit.id, { deviceId: String(values.deviceId), changeReason: String(values.changeReason || '') });
      } else {
        const selected = values.eventTime as { toISOString: () => string };
        await l3Api.event(detail.unit.id, {
          eventType: values.eventType,
          eventTime: selected.toISOString(),
          actorOrgCode: values.actorOrgCode,
          reasonCode: values.reasonCode,
          evidenceRef: values.evidenceRef,
        });
      }
      msg.success(unitAction === 'binding' ? '设备绑定已更新' : '生命周期事件已追加');
      setUnitAction(undefined);
      unitForm.resetFields();
      await showUnit(detail.unit.id);
      await load();
    } catch (e) { msg.error(e instanceof Error ? e.message : '保存失败'); }
  };

  const modelCols: TableProps<ModelView>['columns'] = [
    { title: '型号编码', dataIndex: 'modelCode', render: (v, r) => <Button type="link" onClick={() => void showModel(r.id)}>{v}</Button> },
    { title: '制造商', dataIndex: 'manufacturerName' },
    { title: '类别', dataIndex: 'batteryCategory' },
    { title: '修订数', dataIndex: 'revisionCount' },
    { title: '操作', render: (_, r) => <Button type="link" onClick={() => { setRevisionModel(r); modelForm.setFieldsValue({ modelCode: r.modelCode, manufacturerName: r.manufacturerName, batteryCategory: r.batteryCategory }); setModelModal(true); }}>追加修订</Button> },
  ];
  const unitCols: TableProps<UnitView>['columns'] = [
    { title: '序列号', dataIndex: 'snRaw' },
    { title: '型号 / 修订', render: (_, r) => <>{r.modelCode} / {r.revisionCode}</> },
    { title: '状态', dataIndex: 'status', render: v => <Tag color={v === 'ACTIVE' ? 'green' : 'default'}>{v}</Tag> },
    { title: '操作', render: (_, r) => <Button type="link" onClick={() => void showUnit(r.id)}>详情与治理</Button> },
  ];
  const revisionCols: TableProps<ModelDetail['revisions'][number]>['columns'] = [
    { title: '修订', dataIndex: 'revisionCode' },
    { title: '容量 (kWh)', dataIndex: 'ratedCapacityKwh' },
    { title: '电压 (V)', dataIndex: 'ratedVoltageV' },
    { title: '化学体系', dataIndex: 'chemistry' },
    { title: '生效日期', dataIndex: 'effectiveFrom' },
    { title: '校验 / 审核', render: (_, r) => <Space><Button type="link" onClick={() => void validate('BATTERY_MODEL_REVISION', r.id)}>校验</Button><Button type="link" onClick={() => void submit('BATTERY_MODEL_REVISION', r.id)}>提交审核</Button></Space> },
  ];

  return <div className="data-page layer-page">{holder}
    <div className="page-heading"><div><Typography.Text className="eyebrow">L3 · STRUCTURED DATA</Typography.Text><Typography.Title level={2}>结构化主数据</Typography.Title><Typography.Paragraph type="secondary">管理型号、修订、电池单体及其生命周期。</Typography.Paragraph></div><Space><Button icon={<ReloadOutlined />} onClick={() => void load()}>刷新</Button><Button type="primary" icon={<PlusOutlined />} onClick={() => { setRevisionModel(undefined); modelForm.resetFields(); setModelModal(true); }}>新建型号</Button><Button onClick={() => setManualType('BATTERY_UNIT')}>新增单体</Button><Button onClick={() => setManualType('LIFECYCLE_EVENT')}>新增事件</Button></Space></div>
    <Row gutter={[14, 14]} className="summary-row">{[['型号', summary.models], ['型号修订', summary.revisions], ['电池单体', summary.batteryUnits], ['生命周期事件', summary.events]].map(([name, value]) => <Col xs={12} md={6} key={String(name)}><Card className="stat-card"><Statistic title={String(name)} value={Number(value || 0)} /></Card></Col>)}</Row>
    <Card className="registry-card"><Space className="filter-bar"><Input value={query} onChange={e => setQuery(e.target.value)} onPressEnter={() => void load()} prefix={<SearchOutlined />} placeholder="搜索型号或 SN"/><Button type="primary" onClick={() => void load()}>查询</Button></Space><Tabs items={[
      { key: 'models', label: `电池型号 (${models.length})`, children: <Table rowKey="id" columns={modelCols} dataSource={models} loading={loading} pagination={false}/> },
      { key: 'units', label: `电池单体 (${units.length})`, children: <Table rowKey="id" columns={unitCols} dataSource={units} loading={loading} pagination={false}/> },
    ]}/></Card>
    <Drawer open={Boolean(detail)} onClose={() => { setDetail(undefined); setValidation(undefined); }} width={680} title="电池单体详情" extra={<Space><Button onClick={() => detail && void validate('BATTERY_UNIT', detail.unit.id)}>执行校验</Button><Button onClick={() => detail && void submit('BATTERY_UNIT', detail.unit.id)}>提交审核</Button></Space>}>
      {detail && <><Descriptions bordered size="small" column={1} items={[{ key: 'sn', label: '序列号', children: detail.unit.snRaw }, { key: 'model', label: '型号 / 修订', children: `${detail.unit.modelCode} / ${detail.unit.revisionCode}` }, { key: 'status', label: '状态', children: detail.unit.status }, { key: 'manufactured', label: '制造时间', children: detail.unit.manufacturedAt || '—' }]}/>
        <Space style={{ marginTop: 16 }}><Button onClick={() => setUnitAction('binding')}>绑定 / 更换设备</Button><Button onClick={() => setUnitAction('event')}>追加生命周期事件</Button></Space>
        <Typography.Title level={5}>设备绑定历史</Typography.Title>{detail.bindings.length ? detail.bindings.map(b => <p key={b.id}>{b.deviceId} · {b.boundAt} {b.unboundAt ? '（已解绑）' : '（当前）'}</p>) : <Empty description="暂无绑定"/>}
        <Typography.Title level={5}>生命周期事件</Typography.Title>{detail.events.length ? detail.events.map(e => <p key={e.id}>{e.eventType} · {e.eventTime} · {e.reasonCode || '—'}</p>) : <Empty description="暂无事件"/>}
        {validation && <Alert style={{ marginTop: 16 }} type={validation.status === 'FAIL' ? 'error' : validation.status === 'PASS' ? 'success' : 'warning'} message={`校验：${validation.status}`} description={validation.issues.map(i => `${i.severity} ${i.code}: ${i.message}`).join('；') || '未发现问题'}/>}
      </>}
    </Drawer>
    <Drawer open={Boolean(modelDetail)} onClose={() => setModelDetail(undefined)} width={760} title={modelDetail ? `型号详情 · ${modelDetail.model.modelCode}` : '型号详情'} extra={modelDetail && <Button onClick={() => { setRevisionModel(modelDetail.model); modelForm.setFieldsValue({ modelCode: modelDetail.model.modelCode, manufacturerName: modelDetail.model.manufacturerName, batteryCategory: modelDetail.model.batteryCategory }); setModelModal(true); }}>追加修订</Button>}>
      {modelDetail && <><Descriptions bordered size="small" column={1} items={[{ key: 'manufacturer', label: '制造商', children: modelDetail.model.manufacturerName }, { key: 'category', label: '类别', children: modelDetail.model.batteryCategory }]}/><Typography.Title level={5} style={{ marginTop: 18 }}>型号修订</Typography.Title><Table rowKey="id" columns={revisionCols} dataSource={modelDetail.revisions} pagination={false}/></>}
    </Drawer>
    <Modal open={modelModal} title={revisionModel ? `追加修订 · ${revisionModel.modelCode}` : '新建型号与初始修订'} onCancel={() => { setModelModal(false); setRevisionModel(undefined); }} onOk={() => void modelForm.submit()} okText="保存">
      <Form form={modelForm} layout="vertical" onFinish={values => void saveModel(values)}><Form.Item name="modelCode" label="型号编码" rules={[{ required: true }]}><Input disabled={Boolean(revisionModel)}/></Form.Item><Form.Item name="manufacturerName" label="制造商" rules={[{ required: true }]}><Input/></Form.Item><Form.Item name="batteryCategory" label="电池类别" rules={[{ required: true }]}><Select options={['EV', 'INDUSTRIAL', 'PORTABLE', 'STATIONARY'].map(v => ({ value: v, label: v }))}/></Form.Item><Form.Item name="snProductCode" label="SN 产品型号代码"><Input maxLength={1} placeholder="A–R；用于从 SN 自动匹配型号"/></Form.Item><Form.Item name="revisionCode" label="修订编码" rules={[{ required: true }]}><Input/></Form.Item><Form.Item name="chemistry" label="化学体系" rules={[{ required: true }]}><Input placeholder="例如 LFP"/></Form.Item><Form.Item name="ratedCapacityKwh" label="额定容量 (kWh)" rules={[{ required: true }]}><InputNumber min={0.0001} style={{ width: '100%' }}/></Form.Item><Form.Item name="ratedVoltageV" label="额定电压 (V)" rules={[{ required: true }]}><InputNumber min={0.0001} style={{ width: '100%' }}/></Form.Item><Form.Item name="effectiveFrom" label="生效日期" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }}/></Form.Item></Form>
    </Modal>
    <Modal open={Boolean(unitAction)} title={unitAction === 'binding' ? '绑定设备' : '追加生命周期事件'} onCancel={() => { setUnitAction(undefined); unitForm.resetFields(); }} onOk={() => void unitForm.submit()} okText="保存">
      <Form form={unitForm} layout="vertical" onFinish={values => void saveUnitAction(values)}>
        {unitAction === 'binding' ? <><Form.Item name="deviceId" label="设备 ID" rules={[{ required: true }]}><Input/></Form.Item><Form.Item name="changeReason" label="变更原因"><Input.TextArea rows={2}/></Form.Item></> : <><Form.Item name="eventType" label="事件类型" rules={[{ required: true }]}><Select options={['COMMISSIONED', 'TRANSFERRED', 'REPAIRED', 'REUSED', 'RECYCLED', 'OTHER'].map(v => ({ value: v, label: v }))}/></Form.Item><Form.Item name="eventTime" label="事件时间" rules={[{ required: true }]}><DatePicker showTime style={{ width: '100%' }}/></Form.Item><Form.Item name="actorOrgCode" label="责任组织"><Input/></Form.Item><Form.Item name="reasonCode" label="原因代码"><Input/></Form.Item><Form.Item name="evidenceRef" label="证据引用"><Input/></Form.Item></>}
      </Form>
    </Modal>
    <ManualRecordModal open={Boolean(manualType)} initialType={manualType ?? 'BATTERY_UNIT'} lockType onClose={() => setManualType(undefined)} onCreated={() => { void load(); }}/>
  </div>;
}
