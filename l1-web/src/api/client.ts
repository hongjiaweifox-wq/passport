import type { ApiErrorBody, Page } from '../types/common';

const ORG_CODE=import.meta.env.VITE_ORG_CODE||'DEMO-ORG';
export type DemoRole='DATA_EDITOR'|'COMPLIANCE_MAKER'|'COMPLIANCE_CHECKER'|'PASSPORT_OPERATOR'|'ADMIN';
export interface DemoIdentity{userId:string;role:DemoRole;label:string}
export const DEMO_IDENTITIES:DemoIdentity[]=[{userId:'alice',role:'COMPLIANCE_MAKER',label:'Alice · 合规提交人'},{userId:'bob',role:'COMPLIANCE_CHECKER',label:'Bob · 合规审核人'},{userId:'carol',role:'PASSPORT_OPERATOR',label:'Carol · 护照运营'}];
let identity=DEMO_IDENTITIES[0];const listeners=new Set<()=>void>();
export function getDemoIdentity(){return identity}export function setDemoIdentity(value:DemoIdentity){identity=value;listeners.forEach(fn=>fn())}export function subscribeDemoIdentity(fn:()=>void){listeners.add(fn);return()=>listeners.delete(fn)}
export class LayerApiError extends Error{constructor(readonly status:number,readonly body:ApiErrorBody){super(body.message)}}
export async function layerRequest<T>(path:string,init:RequestInit={},anonymous=false):Promise<T>{const headers=new Headers(init.headers);if(!anonymous){headers.set('X-Org-Code',ORG_CODE);headers.set('X-User',identity.userId);headers.set('X-Role',identity.role)}if(!(init.body instanceof FormData)&&!headers.has('Content-Type'))headers.set('Content-Type','application/json');const response=await fetch(path.startsWith('/api/')?path:`/api/v1/${path}`,{...init,headers});if(!response.ok){let body:ApiErrorBody;try{body=await response.json() as ApiErrorBody}catch{body={code:'API_NETWORK_ERROR',message:`Request failed (${response.status})`,traceId:''}}throw new LayerApiError(response.status,body)}if(response.status===304)return undefined as T;const payload=await response.json() as {data:T;traceId:string}|T;return typeof payload==='object'&&payload!==null&&'data'in payload?(payload as {data:T}).data:payload as T}
export const queryString=(values:Record<string,unknown>)=>{const q=new URLSearchParams();Object.entries(values).forEach(([k,v])=>{if(v!==undefined&&v!==null&&v!=='')q.set(k,String(v))});return q.toString()};
export type {Page};
