import { type Lang, PublicHeader, authenticatePublic, usePublicLang } from './publicPassportShared';
import './PublicPassportPage.css';

const COPY: Record<Lang, {
  title: string;
  credentials: string;
  registered: string;
  note: string;
}> = {
  en: {
    title: 'Authenticate with any of the below options',
    credentials: 'With verifiable credentials',
    registered: 'As a registered user',
    note: 'Demo environment: choosing either option signs you in for this demonstration.',
  },
  zh: {
    title: '请选择以下任一方式完成登录',
    credentials: '使用可验证凭证',
    registered: '以注册用户身份',
    note: '演示环境：选择任一方式即可登录。',
  },
};

function WalletIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="2.5" y="5" width="19" height="14" rx="2.5" stroke="currentColor" strokeWidth="1.6" />
      <path d="M2.5 9.5h19" stroke="currentColor" strokeWidth="1.6" />
      <rect x="14" y="12.5" width="7.5" height="4" rx="1.4" fill="currentColor" opacity="0.85" />
    </svg>
  );
}

function IdIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="2.5" y="4.5" width="19" height="15" rx="2.5" stroke="currentColor" strokeWidth="1.6" />
      <circle cx="8" cy="10.5" r="2" fill="currentColor" />
      <path d="M5 16c.6-1.6 1.7-2.4 3-2.4s2.4.8 3 2.4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M14 9.5h4.5M14 13h4.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

export function PublicAuthPage({ callback }: { callback: string }) {
  const [lang, setLang] = usePublicLang();
  const t = COPY[lang];

  const complete = () => {
    authenticatePublic();
    const target = callback.trim();
    if (!target) {
      window.location.hash = '/sources';
      return;
    }
    const normalized = target.startsWith('#') ? target.slice(1) : target;
    window.location.hash = normalized.startsWith('/') ? normalized : `/${normalized}`;
  };

  return (
    <div className="public-page pp-auth">
      <PublicHeader lang={lang} onLangChange={setLang} dark />
      <main className="pp-auth-main">
        <p className="pp-auth-title">{t.title}</p>
        <div className="pp-auth-options">
          <button type="button" className="pp-auth-option" onClick={complete}>
            <WalletIcon />
            <span>{t.credentials}</span>
          </button>
          <button type="button" className="pp-auth-option" onClick={complete}>
            <IdIcon />
            <span>{t.registered}</span>
          </button>
        </div>
        <p className="pp-auth-note">{t.note}</p>
      </main>
    </div>
  );
}
