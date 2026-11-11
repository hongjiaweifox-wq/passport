import { layerRequest } from './client';
export interface PublicPassport{schemaVersion:string;passportStatus:string;passportVersion:number;updatedAt:string;data:Record<string,unknown>}
export const getPublicPassport=(code:string,etag?:string)=>layerRequest<PublicPassport>(`/api/v1/public/passports/${encodeURIComponent(code)}`,{headers:etag?{'If-None-Match':etag}:undefined},true);
