import { useEffect, useState } from 'react';
import type { DataSourceInput } from '../types/dataSource';
import { Form, Input, Modal, Select, Space, Tabs, Typography } from 'antd';
import { authOptions, deliveryOptions, domainOptions, frequencyOptions, sourceTypeOptions, statusOptions } from '../constants/options';

interface Props { open: boolean; initial?: Partial<DataSourceInput>; loading?: boolean; onCancel: () => void; onSubmit: (value: DataSourceInput) => void }

type FormTab = 'access' | 'ownership';
const OWNERSHIP_FIELDS = new Set(['ownerTeam', 'contact', 'frequency', 'dataDomains', 'description']);
const hintStyle = { marginTop: -4, marginBottom: 16, fontSize: 13 };

export function DataSourceFormModal({ open, initial, loading, onCancel, onSubmit }: Props) {
  const [form] = Form.useForm<DataSourceInput>();
  const [tab, setTab] = useState<FormTab>('access');
  const editing = Boolean(initial);

  useEffect(() => { if (open) setTab('access'); }, [open]);

  const accessPane = <>
    <Typography.Paragraph type="secondary" style={hintStyle}>接入配置由研发 / 接入负责人维护，决定该数据源的连接方式和授权状态。</Typography.Paragraph>
    <Space size={16} style={{ display: 'flex' }} align="start">
      <Form.Item name="sourceCode" label="数据源代码" rules={[{ required: true, message: '请填写数据源代码' }, { max: 64, message: '最多 64 个字符' }]} style={{ flex: 1 }}><Input placeholder="如 SUPPLIER-PLM" disabled={editing} /></Form.Item>
      <Form.Item name="sourceName" label="数据源名称" rules={[{ required: true, message: '请填写数据源名称' }, { max: 120 }]} style={{ flex: 2 }}><Input placeholder="请输入数据源名称" /></Form.Item>
    </Space>
    <Space size={16} style={{ display: 'flex' }} align="start">
      <Form.Item name="sourceType" label="来源类型" rules={[{ required: true }]} style={{ flex: 1 }}><Select options={sourceTypeOptions} placeholder="请选择" /></Form.Item>
      <Form.Item name="deliveryMethod" label="交付方式" rules={[{ required: true }]} style={{ flex: 1 }}><Select options={deliveryOptions} placeholder="请选择" /></Form.Item>
    </Space>
    <Space size={16} style={{ display: 'flex' }} align="start">
      <Form.Item name="authMethod" label="鉴权方式" rules={[{ required: true }]} style={{ flex: 1 }}><Select options={authOptions} placeholder="请选择" /></Form.Item>
      <Form.Item name="status" label="状态" style={{ flex: 1 }}><Select options={statusOptions.map(({ value, label }) => ({ value, label }))} /></Form.Item>
      </Space>
    </>;

  const ownershipPane = <>
    <Typography.Paragraph type="secondary" style={hintStyle}>责任归属由业务 owner 维护：每个数据域都必须有明确的责任团队和联系人。</Typography.Paragraph>
    <Space size={16} style={{ display: 'flex' }} align="start">
      <Form.Item name="ownerTeam" label="责任团队" rules={[{ required: true, message: '请填写责任团队' }, { max: 120 }]} style={{ flex: 1 }}><Input placeholder="负责维护该数据的团队" /></Form.Item>
      <Form.Item name="contact" label="联系人邮箱" rules={[{ type: 'email', message: '请输入有效邮箱' }, { max: 160 }]} style={{ flex: 1 }}><Input placeholder="name@example.com" /></Form.Item>
    </Space>
    <Space size={16} style={{ display: 'flex' }} align="start">
      <Form.Item name="frequency" label="更新频率" rules={[{ required: true }]} style={{ flex: 1 }}><Select options={frequencyOptions} placeholder="请选择" /></Form.Item>
      <Form.Item name="dataDomains" label="数据域" rules={[{ required: true, type: 'array', min: 1, message: '至少选择一个数据域' }]} style={{ flex: 2 }}><Select mode="multiple" options={domainOptions} placeholder="选择该来源提供的数据" /></Form.Item>
    </Space>
    <Form.Item name="description" label="说明"><Input.TextArea rows={3} maxLength={1000} showCount placeholder="数据内容、质量要求或接入说明" /></Form.Item>
  </>;

  return <Modal title={editing ? '编辑数据源' : '登记数据源'} open={open} onCancel={onCancel} onOk={() => form.submit()} confirmLoading={loading} okText="保存" cancelText="取消" destroyOnHidden width={760}>
    <Form<DataSourceInput>
      form={form}
      layout="vertical"
      initialValues={initial ?? { dataDomains: [], templateTypes: [], status: 'DRAFT' }}
      onFinish={value => onSubmit({ ...value, templateTypes: initial?.templateTypes ?? [] })}
      onFinishFailed={({ errorFields }) => {
        const firstName = errorFields[0]?.name;
        const field = Array.isArray(firstName) ? String(firstName[0]) : String(firstName ?? '');
        setTab(OWNERSHIP_FIELDS.has(field) ? 'ownership' : 'access');
      }}
    >
      <Tabs
        activeKey={tab}
        onChange={(key) => setTab(key as FormTab)}
        items={[
          { key: 'access', label: '接入配置', forceRender: true, children: accessPane },
          { key: 'ownership', label: '责任归属', forceRender: true, children: ownershipPane },
        ]}
      />
      {editing && <Form.Item name="version" hidden><Input /></Form.Item>}
    </Form>
  </Modal>;
}
