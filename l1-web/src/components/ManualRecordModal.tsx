import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Descriptions, Form, Input, InputNumber, message, Modal, Select, Space, Spin, Typography } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { LayerApiError } from '../api/client';
import { l3Api, type SnDecoded, type UnitCreated } from '../api/l3';
import { l4Api, type FieldDefinition } from '../api/l4';
import type { RecordType } from '../types/common';

interface Props { open: boolean; onClose: () => void; onCreated: () => void; initialType?: RecordType; lockType?: boolean }
type DppEntry = { code?: string; value?: string };
type FormValues = Record<string, any>;
const typeOptions: Array<{ value: RecordType; label: string }> = [
  { value: 'BATTERY_MODEL', label: '电池型号 / 修订' },
  { value: 'BATTERY_UNIT', label: '电池单体 / SN' },
  { value: 'LIFECYCLE_EVENT', label: '生命周期事件' },
];
const errorLabels: Record<string, string> = {
  L3_SN_EXISTS: '该序列号已存在，请检查后再录入。',
  L3_MODEL_NOT_FOUND: '未找到匹配型号，请先录入带 SN 产品型号代码的型号。',
  L3_MODEL_REVISION_NOT_FOUND: '没有可用的型号修订，请先建立型号和修订。',
  L3_SN_MODEL_MISMATCH: '填写的型号与 SN 中解析出的型号不一致。',
  L3_REVISION_EXISTS: '该修订编码已存在。',
  INGEST_UNKNOWN_ATTRIBUTE: 'DPP 扩展属性编码不在字典中。',
  INGEST_RESERVED_ATTRIBUTE: '该字段为专用或系统生成字段，不能作为扩展属性填写。',
};

function dppMap(entries?: DppEntry[]): Record<string, string> | undefined {
  const result: Record<string, string> = {};
  for (const entry of entries ?? []) if (entry.code && entry.value !== undefined && entry.value !== '') result[entry.code] = entry.value;
  return Object.keys(result).length ? result : undefined;
}

export function ManualRecordModal({ open, onClose, onCreated, initialType = 'BATTERY_MODEL', lockType = false }: Props) {
  const [form] = Form.useForm<FormValues>();
  const [type, setType] = useState<RecordType>('BATTERY_MODEL');
  const [fields, setFields] = useState<FieldDefinition[]>([]);
  const [fieldsLoading, setFieldsLoading] = useState(false);
  const [decoding, setDecoding] = useState(false);
  const [decoded, setDecoded] = useState<SnDecoded>();
  const [decodeError, setDecodeError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [createdUnit, setCreatedUnit] = useState<UnitCreated>();
  const [createdMessage, setCreatedMessage] = useState('');
  const [messageApi, holder] = message.useMessage();
  const sn = Form.useWatch('sn', form);
  const genericFields = useMemo(() => fields.filter(f => f.implementationStatus === 'GENERIC'), [fields]);

  useEffect(() => {
    if (!open) return;
    setType(initialType); setDecoded(undefined); setDecodeError(''); setCreatedUnit(undefined); setCreatedMessage(''); form.resetFields();
    setFieldsLoading(true);
    void l4Api.fields().then(setFields).catch(() => setFields([])).finally(() => setFieldsLoading(false));
  }, [open, form, initialType]);

  useEffect(() => {
    if (!open || type !== 'BATTERY_UNIT') { setDecoded(undefined); setDecodeError(''); return; }
    if (!sn?.trim()) {
      setDecoded(undefined); setDecodeError(''); setDecoding(false);
      form.setFieldsValue({ modelCode: undefined, revisionCode: undefined, manufacturedAt: undefined, batchCode: undefined });
      return;
    }
    let cancelled = false;
    setDecoded(undefined);
    setDecodeError('');
    setDecoding(false);
    form.setFieldsValue({ modelCode: undefined, revisionCode: undefined, manufacturedAt: undefined, batchCode: undefined });
    const timer = window.setTimeout(() => {
      setDecoding(true); setDecodeError('');
      void l3Api.decodeSn(String(sn).trim()).then(result => {
        if (cancelled) return;
        setDecoded(result.valid ? result : undefined);
        if (result.valid && result.modelMatch?.status === 'MATCHED') {
          form.setFieldsValue({ modelCode: result.modelMatch.modelCode, revisionCode: result.modelMatch.revisionCode });
        } else {
          form.setFieldsValue({ modelCode: undefined, revisionCode: undefined });
        }
        if (result.valid && result.manufacturedDate) {
          form.setFieldsValue({ manufacturedAt: dayjs(`${result.manufacturedDate}T00:00:00`) });
        }
        if (result.valid && result.batchCode) {
          form.setFieldsValue({ batchCode: result.batchCode });
        }
        if (!result.valid) {
          form.setFieldsValue({ modelCode: undefined, revisionCode: undefined, manufacturedAt: undefined });
          setDecodeError('非标准 Tuya 整机 SN，请填写型号与修订。');
        }
      }).catch(e => { if (!cancelled) setDecodeError(e instanceof Error ? e.message : 'SN 解析失败'); })
        .finally(() => { if (!cancelled) setDecoding(false); });
    }, 400);
    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [open, type, sn, form]);

  const submit = async (values: FormValues) => {
    setSubmitting(true); setCreatedUnit(undefined);
    try {
      const dppAttributes = dppMap(values.dppAttributes);
      if (type === 'BATTERY_MODEL') {
        const payload = { modelCode: values.modelCode, revisionCode: values.revisionCode, manufacturerName: values.manufacturerName, batteryCategory: values.batteryCategory, chemistry: values.chemistry, ratedCapacityKwh: values.ratedCapacityKwh, ratedVoltageV: values.ratedVoltageV, effectiveFrom: values.effectiveFrom.format('YYYY-MM-DD'), snProductCode: values.snProductCode, dppAttributes };
        await l3Api.createModel(payload);
        setCreatedMessage('型号与修订已创建');
        messageApi.success('型号与修订已写入 L3');
        onCreated();
      } else if (type === 'BATTERY_UNIT') {
        const manufacturedAt = values.manufacturedAt?.format?.() ?? values.manufacturedAt?.toISOString?.();
        const result = await l3Api.createUnit({ sn: values.sn, modelCode: values.modelCode, revisionCode: values.revisionCode, batchCode: values.batchCode, manufacturedAt, deviceId: values.deviceId, dppAttributes });
        setCreatedUnit(result);
        setCreatedMessage('电池单体已创建');
        messageApi.success('电池单体已写入 L3');
        onCreated();
      } else {
        await l3Api.createEvent({ sn: values.sn, eventType: values.eventType, eventTime: values.eventTime.toISOString(), actorOrgCode: values.actorOrgCode, reasonCode: values.reasonCode, evidenceRef: values.evidenceRef });
        messageApi.success('生命周期事件已写入 L3');
        setCreatedMessage('生命周期事件已创建');
        onCreated();
      }
    } catch (e) {
      if (e instanceof LayerApiError) messageApi.error(errorLabels[e.body.code] ?? `${e.body.message} (${e.body.code})`);
      else messageApi.error(e instanceof Error ? e.message : '手动录入失败');
    } finally { setSubmitting(false); }
  };

  const dppEditor = <Form.List name="dppAttributes">
    {(items, { add, remove }) => <>
      <Typography.Text strong>DPP 扩展属性（选填）</Typography.Text>
      {items.map(({ key, name, ...rest }) => <Space key={key} align="baseline" style={{ display: 'flex', marginTop: 8 }}>
        <Form.Item {...rest} name={[name, 'code']} rules={[{ required: true, message: '请选择 DPP 属性' }]} style={{ width: 270 }}>
          <Select showSearch loading={fieldsLoading} placeholder="选择 GENERIC 字段" optionFilterProp="label" options={genericFields.map(f => ({ value: f.fieldCode, label: `${f.displayNameZh ?? f.fieldCode} (${f.fieldCode})` }))}/>
        </Form.Item>
        <Form.Item {...rest} name={[name, 'value']} rules={[{ required: true, message: '请输入属性值' }]} style={{ flex: 1, minWidth: 150 }}><Input placeholder="属性值"/></Form.Item>
        <Button type="text" danger icon={<DeleteOutlined/>} onClick={() => remove(name)} aria-label="删除 DPP 属性"/>
      </Space>)}
      <Button type="dashed" icon={<PlusOutlined/>} onClick={() => add()} block>添加 DPP 属性</Button>
    </>}
  </Form.List>;

  const renderFields = () => {
    if (type === 'BATTERY_MODEL') return <>
      <Form.Item name="modelCode" label="型号编码" rules={[{ required: true }]}><Input maxLength={128}/></Form.Item>
      <Form.Item name="revisionCode" label="修订编码" rules={[{ required: true }]}><Input maxLength={128}/></Form.Item>
      <Form.Item name="manufacturerName" label="制造商名称" rules={[{ required: true }]}><Input/></Form.Item>
      <Form.Item name="batteryCategory" label="电池类别" rules={[{ required: true }]}><Select options={['PORTABLE','LMT','EV','INDUSTRIAL','STATIONARY','SLI'].map(v => ({ value: v, label: v }))}/></Form.Item>
      <Form.Item name="chemistry" label="化学体系" rules={[{ required: true }]}><Input placeholder="例如 LFP"/></Form.Item>
      <Form.Item name="ratedCapacityKwh" label="额定容量 (kWh)" rules={[{ required: true }]}><InputNumber min={0.0001} style={{ width: '100%' }}/></Form.Item>
      <Form.Item name="ratedVoltageV" label="额定电压 (V)"><InputNumber min={0.0001} style={{ width: '100%' }}/></Form.Item>
      <Form.Item name="effectiveFrom" label="生效日期" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }}/></Form.Item>
      <Form.Item name="snProductCode" label="SN 产品型号代码"><Select allowClear options={['A','B','C','D','E','F','G','H','I','J','K','L','M','N','P','Q','R'].map((v: string) => ({ value: v, label: v }))}/></Form.Item>
      {dppEditor}
    </>;
    if (type === 'BATTERY_UNIT') return <>
      <Form.Item name="sn" label="序列号 SN" rules={[{ required: true }]}><Input maxLength={128} placeholder="Tuya 16 位整机 SN"/></Form.Item>
      <Spin spinning={decoding}>
        <div aria-live="polite">
          {decoded && <>
            <Alert type="success" showIcon message="SN 解析成功" description={<Descriptions size="small" column={1} items={[
              { key: 'product', label: '产品型号', children: `${decoded.productName} (${decoded.productCode})` },
              { key: 'factory', label: '工厂', children: `${decoded.factoryName} (${decoded.factoryCode})` },
              { key: 'brand', label: '电芯品牌', children: `${decoded.cellBrandName} (${decoded.cellBrandCode})` },
              { key: 'date', label: '制造日期', children: decoded.manufacturedDate },
              { key: 'batch', label: '派生批次', children: decoded.batchCode },
            ]} />} />
            {decoded.modelMatch?.status === 'MATCHED'
              ? <Alert style={{ marginTop: 8 }} type="success" showIcon message={`已匹配型号 ${decoded.modelMatch.modelCode} / 修订 ${decoded.modelMatch.revisionCode}`} description="型号和修订已自动回填；如有需要可手动修改。" />
              : <Alert style={{ marginTop: 8 }} type="warning" showIcon message={decoded.modelMatch?.status === 'AMBIGUOUS' ? 'SN 产品代码对应多个型号，无法自动匹配' : decoded.modelMatch?.status === 'NO_ACTIVE_REVISION' ? `已找到型号 ${decoded.modelMatch.modelCode}，但没有有效修订` : 'SN 已解析，但尚未配置对应的型号和修订'} description={decoded.modelMatch?.status === 'AMBIGUOUS' ? '请先在型号资料中确保 SN 产品代码唯一，再录入单体。' : decoded.modelMatch?.status === 'NO_ACTIVE_REVISION' ? '请先为该型号创建有效修订后再录入单体。' : `解析到产品代码 ${decoded.productCode}（${decoded.productName}）。请先创建并绑定该 SN 产品代码的型号及有效修订；仅解析 SN 不会自动创建型号资料。`} />}
          </>}
          {decodeError && <Alert style={{ marginTop: 8 }} type="info" showIcon message={decodeError} />}
        </div>
      </Spin>
      <Form.Item name="modelCode" label="型号编码（可选）"><Input placeholder="可留空：从 SN 产品代码自动匹配"/></Form.Item>
      <Form.Item name="revisionCode" label="修订编码（可选）"><Input placeholder="可留空：默认使用最新修订"/></Form.Item>
      <Form.Item name="batchCode" label="批次编码（可选）"><Input/></Form.Item>
            <Form.Item name="manufacturedAt" label="制造时间（可选）" extra={decoded?.manufacturedDate ? `已根据 SN 回填 ${decoded.manufacturedDate}，可手动修改` : undefined}><DatePicker showTime style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="deviceId" label="设备 ID（可选）"><Input/></Form.Item>
      {dppEditor}
    </>;
    return <>
      <Form.Item name="sn" label="序列号 SN" rules={[{ required: true }]}><Input maxLength={128}/></Form.Item>
      <Form.Item name="eventType" label="事件类型" rules={[{ required: true }]}><Select options={['COMMISSIONED','REPAIRED','REPLACED','REPURPOSED','REMANUFACTURED','RECYCLED'].map(v => ({ value: v, label: v }))}/></Form.Item>
      <Form.Item name="eventTime" label="事件时间" rules={[{ required: true }]}><DatePicker showTime style={{ width: '100%' }}/></Form.Item>
      <Form.Item name="actorOrgCode" label="执行方组织编码" rules={[{ required: true }]}><Input/></Form.Item>
      <Form.Item name="reasonCode" label="原因编码"><Input/></Form.Item>
      <Form.Item name="evidenceRef" label="证据引用"><Input/></Form.Item>
    </>;
  };

  return <Modal width={720} title={lockType && type === 'BATTERY_UNIT' ? '新增电池单体' : lockType && type === 'LIFECYCLE_EVENT' ? '新增生命周期事件' : '手动录入单条数据'} open={open} onCancel={onClose} onOk={() => form.submit()} okText="保存" cancelText="关闭" confirmLoading={submitting} destroyOnHidden>
    {holder}
    {createdUnit ? <><Alert type="success" showIcon message="电池单体已创建" description={<><div>自动回填 {Object.keys(createdUnit.autoFilled ?? {}).length} 项：{Object.entries(createdUnit.autoFilled ?? {}).map(([k,v]) => `${k}=${String(v)}`).join('，') || '无'}</div>{Object.keys(createdUnit.conflicts ?? {}).length > 0 && <div>冲突：{Object.entries(createdUnit.conflicts ?? {}).map(([k,v]) => `${k} 输入=${v.csvValue} / SN=${v.snValue}`).join('；')}</div>}</>}/><Button type="link" onClick={() => { window.location.hash = '/l3'; onClose(); }}>查看结构化数据</Button></> : createdMessage ? <><Alert type="success" showIcon message={createdMessage}/><Button type="link" onClick={() => { window.location.hash = '/l3'; onClose(); }}>查看结构化数据</Button></> : <>
      <Form form={form} layout="vertical" onFinish={values => void submit(values)}>
        {!lockType && <Form.Item label="记录类型"><Select value={type} options={typeOptions} onChange={value => { setType(value); form.resetFields(); setCreatedUnit(undefined); }}/></Form.Item>}
        {renderFields()}
      </Form>
      <Typography.Paragraph type="secondary" style={{ marginTop: 8 }}>此操作直接写入 L3，保存后立即出现在结构化数据中；系统会校验字段，并保留来源为手动录入。</Typography.Paragraph>
    </>}
  </Modal>;
}
