import { Descriptions, Drawer, Tag, Typography } from 'antd';
import type { DataSource } from '../types/dataSource';
import { authOptions, deliveryOptions, domainOptions, frequencyOptions, labelFor, sourceTypeOptions, statusOptions } from '../constants/options';

interface Props { source?: DataSource; open: boolean; onClose: () => void }
export function DataSourceDetailDrawer({ source, open, onClose }: Props) {
  if (!source) return <Drawer title="数据源详情" open={open} onClose={onClose} />;
  const status = statusOptions.find((option) => option.value === source.status);
  return <Drawer title="数据源详情" open={open} onClose={onClose} width={560}>
    <Descriptions column={1} bordered size="small">
      <Descriptions.Item label="数据源名称">{source.sourceName}</Descriptions.Item>
      <Descriptions.Item label="数据源代码"><Typography.Text code>{source.sourceCode}</Typography.Text></Descriptions.Item>
      <Descriptions.Item label="来源类型">{labelFor(sourceTypeOptions, source.sourceType)}</Descriptions.Item>
      <Descriptions.Item label="状态"><Tag color={status?.color}>{status?.label}</Tag></Descriptions.Item>
      <Descriptions.Item label="责任团队">{source.ownerTeam}</Descriptions.Item>
      <Descriptions.Item label="联系人">{source.contact || '—'}</Descriptions.Item>
      <Descriptions.Item label="交付方式">{labelFor(deliveryOptions, source.deliveryMethod)}</Descriptions.Item>
      <Descriptions.Item label="更新频率">{labelFor(frequencyOptions, source.frequency)}</Descriptions.Item>
      <Descriptions.Item label="鉴权方式">{labelFor(authOptions, source.authMethod)}</Descriptions.Item>
      <Descriptions.Item label="数据域">{source.dataDomains.map((v) => labelFor(domainOptions, v)).join('、')}</Descriptions.Item>
      <Descriptions.Item label="说明">{source.description || '—'}</Descriptions.Item>
      <Descriptions.Item label="创建时间">{new Date(source.createdAt).toLocaleString('zh-CN')}</Descriptions.Item>
      <Descriptions.Item label="最近更新">{new Date(source.updatedAt).toLocaleString('zh-CN')}</Descriptions.Item>
    </Descriptions>
  </Drawer>;
}
