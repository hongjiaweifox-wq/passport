import { useSyncExternalStore } from 'react';
import { getDemoIdentity, subscribeDemoIdentity } from '../api/client';
export function useDemoIdentity(){return useSyncExternalStore(subscribeDemoIdentity,getDemoIdentity,getDemoIdentity)}
export {DEMO_IDENTITIES,getDemoIdentity,setDemoIdentity} from '../api/client';
