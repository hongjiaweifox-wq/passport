import type { DataDomain, SourceStatus, SourceType, DeliveryMethod, Frequency, AuthMethod } from '../types/dataSource';

export const sourceTypeOptions: { value: SourceType; label: string }[] = [
  { value: 'SUPPLIER', label: '电池制造商 / 供应商' },
  { value: 'ERP_PLM', label: '品牌 ERP / PLM' },
  { value: 'TEST_COMPLIANCE', label: '测试与合规资料' },
  { value: 'TUYA_IOT', label: '涂鸦 IoT / 设备云' },
  { value: 'SERVICE_PARTNER', label: '服务 / 维修 / 回收伙伴' },
];
export const deliveryOptions: { value: DeliveryMethod; label: string }[] = [
  { value: 'FILE_TEMPLATE', label: '模板文件' }, { value: 'API', label: '业务 API' },
  { value: 'MANUAL', label: '人工填报' }, { value: 'IOT_API', label: 'IoT API' },
];
export const frequencyOptions: { value: Frequency; label: string }[] = [
  { value: 'ON_CHANGE', label: '变更时' }, { value: 'DAILY', label: '每日' },
  { value: 'WEEKLY', label: '每周' }, { value: 'MONTHLY', label: '每月' }, { value: 'AD_HOC', label: '不定期' },
];
export const authOptions: { value: AuthMethod; label: string }[] = [
  { value: 'OIDC_CC', label: 'OAuth2 Client Credentials' }, { value: 'SIGNED_WEBHOOK', label: '签名回调' },
  { value: 'API_KEY', label: 'API Key（演示）' }, { value: 'MANUAL', label: '人工提交' },
];
export const statusOptions: { value: SourceStatus; label: string; color: string }[] = [
  { value: 'ACTIVE', label: '已启用', color: 'green' }, { value: 'DRAFT', label: '草稿', color: 'gold' },
  { value: 'SUSPENDED', label: '已暂停', color: 'default' },
];
export const domainOptions: { value: DataDomain; label: string }[] = [
  { value: 'MODEL', label: '电池型号' }, { value: 'UNIT', label: '电池单体 / SN' },
  { value: 'LIFECYCLE', label: '生命周期事件' }, { value: 'EVIDENCE', label: '合规证据' },
  { value: 'IOT_TELEMETRY', label: 'IoT 运行数据' }, { value: 'RECYCLING', label: '回收 / 循环利用' },
];
export const labelFor = <T extends string>(options: { value: T; label: string }[], value: string) =>
  options.find((option) => option.value === value)?.label ?? value;
