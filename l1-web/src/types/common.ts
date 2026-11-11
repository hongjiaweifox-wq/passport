export interface Page<T> { items: T[]; total: number; page: number; size: number }
export interface ApiErrorBody { code: string; message: string; traceId: string; fieldErrors?: Record<string, string> }
export type RecordType = 'BATTERY_MODEL' | 'BATTERY_UNIT' | 'LIFECYCLE_EVENT';
