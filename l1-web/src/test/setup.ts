import '@testing-library/jest-dom/vitest';
Object.defineProperty(window, 'matchMedia', { writable: true, value: (query: string) => ({ matches: false, media: query, onchange: null, addListener: () => {}, removeListener: () => {}, addEventListener: () => {}, removeEventListener: () => {}, dispatchEvent: () => false }) });
class ResizeObserverMock { observe() {} unobserve() {} disconnect() {} }
Object.defineProperty(window, 'ResizeObserver', { writable: true, value: ResizeObserverMock });
Object.defineProperty(URL, 'createObjectURL', { writable: true, value: () => 'blob:mock' });
Object.defineProperty(URL, 'revokeObjectURL', { writable: true, value: () => {} });
const originalGetComputedStyle = window.getComputedStyle.bind(window);
window.getComputedStyle = ((element: Element, pseudoElt?: string | null) => {
  if (pseudoElt) return originalGetComputedStyle(element);
  return originalGetComputedStyle(element);
}) as typeof window.getComputedStyle;
