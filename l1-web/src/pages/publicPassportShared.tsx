import { useCallback, useState } from 'react';

export type Dict = Record<string, unknown>;

export type Lang = 'en' | 'zh';

const STORAGE_KEY = 'passport-public-language';

export const initialLanguage = (): Lang => {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored === 'zh' || stored === 'en') return stored;
  } catch {
    // ignore storage access errors
  }
  return 'en';
};

export function usePublicLang(): [Lang, (next: Lang) => void] {
  const [lang, setLang] = useState<Lang>(initialLanguage);
  const change = useCallback((next: Lang) => {
    setLang(next);
    try {
      window.localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // ignore storage access errors
    }
  }, []);
  return [lang, change];
}

export const BRAND: Record<Lang, { first: string; second: string }> = {
  en: { first: 'Battery', second: 'Passport' },
  zh: { first: '电池', second: '护照' },
};

export const LANGUAGE_LABEL: Record<Lang, string> = { en: 'EN', zh: '中文' };

const AUTH_KEY = 'passport-public-authenticated';
let memoryAuth = false;

export function isPublicAuthenticated(): boolean {
  if (memoryAuth) return true;
  try {
    return window.localStorage.getItem(AUTH_KEY) === '1';
  } catch {
    return false;
  }
}

export function authenticatePublic(): void {
  memoryAuth = true;
  try {
    window.localStorage.setItem(AUTH_KEY, '1');
  } catch {
    // ignore storage access errors
  }
}

export const DISCLAIMER: Record<Lang, string> = {
  en: 'Disclaimer: In this battery passport demonstrator, sample data may be provided for demonstration purposes only. Therefore, none of the operators or partners can be held liable for any errors or omissions in the data provided, and no representation or warranty, express or implied, is made as to the accuracy, adequacy, completeness, legality, reliability or usefulness of the data. Data is subject to change without notice.',
  zh: '免责声明：本电池护照演示系统可能使用示例数据，仅用于演示目的。因此，运营方或合作方不对所提供数据中的任何错误或遗漏承担责任，也不对数据的准确性、充分性、完整性、合法性、可靠性或可用性作出任何明示或暗示的陈述或保证。数据可能随时变更，恕不另行通知。',
};

export function collectText(value: unknown): string {
  if (value === null || value === undefined || value === '') return '';
  if (Array.isArray(value)) return value.map(collectText).filter(Boolean).join(', ');
  if (typeof value === 'object') {
    const obj = value as Dict;
    const preferred = obj.name ?? obj.legal_name ?? obj.company_name ?? obj.value ?? obj.label;
    if (preferred !== undefined) return collectText(preferred);
    return JSON.stringify(value);
  }
  return String(value);
}

export function firstOf(data: Dict, keys: string[]): string {
  for (const key of keys) {
    const text = collectText(data[key]);
    if (text) return text;
  }
  return '';
}

export function numeric(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[^0-9.+-]/g, ''));
    if (value.trim() !== '' && Number.isFinite(parsed)) return parsed;
  }
  return undefined;
}

export function humanize(key: string): string {
  const words = key.replace(/[_-]+/g, ' ').trim();
  return words.charAt(0).toUpperCase() + words.slice(1);
}

export function formatDate(value: string): string {
  return value.length >= 10 ? value.slice(0, 10) : value;
}

export function statusText(value: string, lang: Lang): string {
  if (lang !== 'zh' || !value) return value;
  const normalized = value.trim().toLowerCase();
  const labels: Record<string, string> = {
    original: '原始', repurposed: '梯次利用', reused: '再利用', remanufactured: '再制造', waste: '废弃', in_use: '使用中',
  };
  return labels[normalized] ?? value;
}

export function BatteryMark({ dark = false }: { dark?: boolean }) {
  const color = dark ? '#ffffff' : '#00a878';
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="2.5" y="6" width="16.5" height="12" rx="2.5" stroke={color} strokeWidth="1.7" />
      <rect x="21" y="9.2" width="1.8" height="5.6" rx="0.9" fill={color} />
      <rect x="5" y="9.4" width="3" height="5.2" rx="0.8" fill={color} opacity="0.55" />
      <rect x="9.4" y="9.4" width="3" height="5.2" rx="0.8" fill={color} opacity="0.8" />
      <rect x="13.8" y="9.4" width="2.6" height="5.2" rx="0.8" fill={color} />
    </svg>
  );
}

export function PublicLanguageSwitch({
  lang,
  onChange,
  dark = false,
  label,
}: {
  lang: Lang;
  onChange: (next: Lang) => void;
  dark?: boolean;
  label: string;
}) {
  return (
    <div className={dark ? 'pp-lang pp-lang-dark' : 'pp-lang'} role="group" aria-label={label}>
      {(['en', 'zh'] as const).map((code) => (
        <button
          key={code}
          type="button"
          className={lang === code ? 'is-active' : ''}
          aria-pressed={lang === code}
          onClick={() => onChange(code)}
        >
          {LANGUAGE_LABEL[code]}
        </button>
      ))}
    </div>
  );
}

function AccountIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="8" r="3.6" stroke="currentColor" strokeWidth="1.7" />
      <path d="M4.8 19.2c.9-3.2 3.6-5 7.2-5s6.3 1.8 7.2 5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  );
}

export function PublicHeader({
  lang,
  onLangChange,
  dark = false,
  onBrandClick,
  onAccount,
  accountLabel,
}: {
  lang: Lang;
  onLangChange: (next: Lang) => void;
  dark?: boolean;
  onBrandClick?: () => void;
  onAccount?: () => void;
  accountLabel?: string;
}) {
  const brand = BRAND[lang];
  return (
    <header className={dark ? 'pp-header pp-header-dark' : 'pp-header'}>
      <div className="pp-brand" onClick={onBrandClick} role={onBrandClick ? 'link' : undefined} tabIndex={onBrandClick ? 0 : undefined}>
        <BatteryMark dark={dark} />
        <span className="pp-brand-copy">
          {brand.first}
          <br />
          {brand.second}
        </span>
      </div>
      <div className="pp-header-meta">
        {onAccount ? (
          <button type="button" className={dark ? 'pp-account pp-account-dark' : 'pp-account'} onClick={onAccount}>
            <AccountIcon />
            {accountLabel ?? (lang === 'zh' ? '登录' : 'Sign in')}
          </button>
        ) : null}
        <PublicLanguageSwitch lang={lang} onChange={onLangChange} dark={dark} label={lang === 'zh' ? '语言' : 'Language'} />
      </div>
    </header>
  );
}

export function PublicFooter({
  lang,
  schemaVersion,
  passportVersion,
}: {
  lang: Lang;
  schemaVersion?: string;
  passportVersion?: number;
}) {
  return (
    <footer className="pp-footer">
      <p className="pp-disclaimer">{DISCLAIMER[lang]}</p>
      <div className="pp-footer-row">
        <span>© 2026 Passport Cloud</span>
        <span className="pp-version">
          {schemaVersion ?? ''}
          {passportVersion !== undefined ? ` · v${passportVersion}` : ''}
        </span>
      </div>
    </footer>
  );
}
