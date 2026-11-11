export type SourceType = 'SUPPLIER' | 'ERP_PLM' | 'TEST_COMPLIANCE' | 'TUYA_IOT' | 'SERVICE_PARTNER';
export type DeliveryMethod = 'FILE_TEMPLATE' | 'API' | 'MANUAL' | 'IOT_API';
export type Frequency = 'ON_CHANGE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'AD_HOC';
export type AuthMethod = 'OIDC_CC' | 'SIGNED_WEBHOOK' | 'API_KEY' | 'MANUAL';
export type SourceStatus = 'DRAFT' | 'ACTIVE' | 'SUSPENDED';
export type DataDomain = 'MODEL' | 'UNIT' | 'LIFECYCLE' | 'EVIDENCE' | 'IOT_TELEMETRY' | 'RECYCLING';
export type TemplateType = 'BATTERY_MODEL' | 'BATTERY_UNIT' | 'LIFECYCLE_EVENT';
export interface DataSource {
  id: string; orgCode: string; sourceCode: string; sourceName: string; sourceType: SourceType;
  ownerTeam: string; contact: string | null; dataDomains: DataDomain[]; deliveryMethod: DeliveryMethod;
  frequency: Frequency; authMethod: AuthMethod; templateTypes: TemplateType[]; status: SourceStatus;
  description: string | null; version: number; createdAt: string; updatedAt: string;
}
export interface DataSourceInput {
  sourceCode: string; sourceName: string; sourceType: SourceType; ownerTeam: string; contact?: string;
  dataDomains: DataDomain[]; deliveryMethod: DeliveryMethod; frequency: Frequency; authMethod: AuthMethod;
  templateTypes: TemplateType[]; status?: SourceStatus; description?: string; version?: number;
}
export interface PageResult<T> { items: T[]; total: number; page: number; size: number }
export interface Summary { total: number; active: number; draft: number; suspended: number }
