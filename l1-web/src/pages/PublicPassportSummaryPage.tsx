import { useEffect, useState } from 'react';
import { Result, Spin } from 'antd';
import { getPublicPassport, type PublicPassport } from '../api/publicPassport';
import { LayerApiError } from '../api/client';
import {
  type Lang, firstOf, statusText,
  PublicHeader, PublicFooter, usePublicLang,
} from './publicPassportShared';
import { usePassportView } from './PublicPassportPage';
import './PublicPassportPage.css';

type TabKey = 'general' | 'materials' | 'performance' | 'compliance' | 'supply' | 'circularity' | 'carbon';

const COPY: Record<Lang, {
  back: string;
  illustrationNote: string;
  economicOperator: string;
  modelNumber: string;
  serialNumber: string;
  tabs: Record<TabKey, string>;
  descriptions: Record<TabKey, string>;
  labels: Record<string, string>;
  carbonStages: [string, string, string, string];
  noData: string;
  verified: string;
  errorTitle: string;
  errorSuspended: string;
  passportId: string;
}> = {
  en: {
    back: 'Back to summary',
    illustrationNote: 'Image is not the actual battery, and is for illustrative purposes only',
    economicOperator: 'Economic operator',
    modelNumber: 'Model Number',
    serialNumber: 'Serial Number',
    passportId: 'Passport ID',
    verified: 'Verified',
    tabs: {
      general: 'General', materials: 'Material composition', performance: 'Performance',
      compliance: 'Compliance', supply: 'Supply chain', circularity: 'Circularity', carbon: 'Carbon Footprint',
    },
    descriptions: {
      general: 'Generic information about the battery',
      materials: 'Chemistry and materials used in the battery',
      performance: 'Electrical and power performance',
      compliance: 'Regulatory and passport compliance details',
      supply: 'Operators and manufacturing locations',
      circularity: 'Recycled content and material circulation',
      carbon: 'Life-cycle carbon footprint by stage',
    },
    labels: {
      name: 'Name', manufacturedDate: 'Manufactured date', facility: 'Facility ID', manufacturedBy: 'Manufactured by',
      category: 'Category', status: 'Status', weight: 'Weight',
      chemistry: 'Chemistry', criticalMaterials: 'Critical raw materials', cathodeAnode: 'Cathode / anode / electrolyte materials',
      hazardous: 'Hazardous substances', ratedCapacity: 'Rated capacity', ratedVoltage: 'Rated voltage',
      originalPower: 'Original power', remainingPower: 'Remaining power', powerFade: 'Power fade', maxPower: 'Maximum permitted power',
      marketCode: 'Market code', effectiveFrom: 'Effective from', revisionCode: 'Revision code', schema: 'Schema',
      passportVersion: 'Passport version', passportStatus: 'Passport status', manufacturer: 'Manufacturer',
      economicOperator: 'Economic operator', manufacturingPlace: 'Manufacturing place', recycled: 'Recycled content share',
      total: 'Total', composition: 'Composition',
    },
    carbonStages: ['Raw material extraction', 'Main production', 'Distribution', 'Recycling'],
    noData: 'No information provided in this passport.',
    errorTitle: 'Passport unavailable',
    errorSuspended: 'This passport is suspended or unavailable',
  },
  zh: {
    back: '返回摘要',
    illustrationNote: '图片并非实际电池，仅用于示意',
    economicOperator: '经济运营者',
    modelNumber: '型号',
    serialNumber: '序列号',
    passportId: '护照标识',
    verified: '已验证',
    tabs: {
      general: '基本信息', materials: '材料构成', performance: '性能', compliance: '合规',
      supply: '供应链', circularity: '循环利用', carbon: '碳足迹',
    },
    descriptions: {
      general: '电池的基本信息', materials: '电池的化学体系与材料构成', performance: '电气与功率性能',
      compliance: '法规与护照合规信息', supply: '运营方与制造地点', circularity: '再生材料含量与循环利用',
      carbon: '按生命周期阶段统计的碳足迹',
    },
    labels: {
      name: '名称', manufacturedDate: '制造日期', facility: '设施标识', manufacturedBy: '制造商',
      category: '电池类别', status: '状态', weight: '重量',
      chemistry: '化学体系', criticalMaterials: '关键原材料', cathodeAnode: '正极 / 负极 / 电解液材料',
      hazardous: '有害物质', ratedCapacity: '额定容量', ratedVoltage: '额定电压',
      originalPower: '原始功率', remainingPower: '剩余功率', powerFade: '功率衰减', maxPower: '最大允许功率',
      marketCode: '市场代码', effectiveFrom: '生效日期', revisionCode: '修订版本', schema: '数据规范',
      passportVersion: '护照版本', passportStatus: '护照状态', manufacturer: '制造商',
      economicOperator: '经济运营者', manufacturingPlace: '制造地点', recycled: '再生材料含量',
      total: '合计', composition: '成分',
    },
    carbonStages: ['原材料开采', '主产品生产', '分销', '回收'],
    noData: '该护照未提供相关信息。',
    errorTitle: '护照暂不可用',
    errorSuspended: '护照已暂停或不可公开',
  },
};

const TAB_ORDER: TabKey[] = ['general', 'materials', 'performance', 'compliance', 'supply', 'circularity', 'carbon'];

function BatteryIllustration() {
  return (
    <svg className="ps-illustration" viewBox="0 0 240 130" role="img" aria-label="Battery illustration">
      <rect x="22" y="46" width="196" height="64" rx="10" fill="#e9edf0" stroke="#b6bec4" />
      <rect x="34" y="56" width="40" height="44" rx="4" fill="#d3d9dd" stroke="#aeb7bd" />
      <rect x="80" y="56" width="40" height="44" rx="4" fill="#d3d9dd" stroke="#aeb7bd" />
      <rect x="126" y="56" width="40" height="44" rx="4" fill="#d3d9dd" stroke="#aeb7bd" />
      <rect x="172" y="56" width="36" height="44" rx="4" fill="#d3d9dd" stroke="#aeb7bd" />
      <rect x="36" y="32" width="18" height="14" rx="3" fill="#aab3b9" />
      <rect x="186" y="32" width="18" height="14" rx="3" fill="#aab3b9" />
      <path d="M22 78h196" stroke="#c4cbd0" />
      <circle cx="54" cy="78" r="3" fill="#97a1a8" />
      <circle cx="100" cy="78" r="3" fill="#97a1a8" />
      <circle cx="146" cy="78" r="3" fill="#97a1a8" />
      <circle cx="190" cy="78" r="3" fill="#97a1a8" />
    </svg>
  );
}

export function PublicPassportSummaryPage({ publicCode }: { publicCode: string }) {
  const [data, setData] = useState<PublicPassport>();
  const [error, setError] = useState('');
  const [httpStatus, setHttpStatus] = useState<number>();
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<TabKey>('general');
  const [lang, setLang] = usePublicLang();
  const t = COPY[lang];
  const view = usePassportView(data, publicCode);

  useEffect(() => {
    setLoading(true);
    setError('');
    setHttpStatus(undefined);
    void getPublicPassport(publicCode)
      .then(setData)
      .catch((e) => {
        setError(e instanceof Error ? e.message : '护照不可用');
        if (e instanceof LayerApiError) setHttpStatus(e.status);
      })
      .finally(() => setLoading(false));
  }, [publicCode]);

  const goBack = () => { window.location.hash = `/p/${encodeURIComponent(publicCode)}`; };

  if (loading) {
    return <div className="public-page pp-state"><Spin size="large" /></div>;
  }
  if (error) {
    return (
      <div className="public-page pp-state">
        <Result status={httpStatus === 410 ? '403' : '404'} title={httpStatus === 410 ? t.errorSuspended : t.errorTitle} subTitle={error} />
      </div>
    );
  }

  const a = view.attributes;
  const fieldsFor = (key: TabKey): Array<{ label: string; value: string; status?: boolean }> => {
    switch (key) {
      case 'general':
        return [
          { label: t.labels.name, value: view.model || view.passportId },
          { label: t.labels.manufacturedDate, value: view.manufacturedDate },
          { label: t.labels.facility, value: view.facility },
          { label: t.labels.manufacturedBy, value: view.manufacturer },
          { label: t.labels.category, value: view.category },
          { label: t.labels.status, value: statusText(view.batteryStatus, lang), status: true },
          { label: t.labels.weight, value: view.mass },
        ];
      case 'materials':
        return [
          { label: t.labels.chemistry, value: firstOf(a, ['chemistry', 'materials_battery_chemistry']) },
          { label: t.labels.criticalMaterials, value: firstOf(a, ['critical_raw_materials']) },
          { label: t.labels.cathodeAnode, value: firstOf(a, ['materials_used_in_cathode_anode_and_electrolyte']) },
          { label: t.labels.hazardous, value: firstOf(a, ['hazardous_substances_and_their_impact', 'hazardous_substances']) },
          { label: t.labels.composition, value: firstOf(a, ['material_composition', 'materials_composition']) },
        ];
      case 'performance':
        return [
          { label: t.labels.ratedCapacity, value: firstOf(a, ['rated_capacity_kwh']) },
          { label: t.labels.ratedVoltage, value: firstOf(a, ['rated_voltage_v']) },
          { label: t.labels.originalPower, value: view.originalPower },
          { label: t.labels.remainingPower, value: firstOf(a, ['remaining_power_capability']) },
          { label: t.labels.powerFade, value: firstOf(a, ['power_fade']) },
          { label: t.labels.maxPower, value: firstOf(a, ['maximum_permitted_battery_power']) },
        ];
      case 'compliance':
        return [
          { label: t.labels.passportStatus, value: String(data?.passportStatus ?? '') },
          { label: t.labels.passportVersion, value: String(data?.passportVersion ?? '') },
          { label: t.labels.schema, value: data?.schemaVersion ?? '' },
          { label: t.labels.marketCode, value: firstOf(a, ['market_code']) },
          { label: t.labels.effectiveFrom, value: firstOf(a, ['effective_from']) },
          { label: t.labels.revisionCode, value: firstOf(a, ['revision_code']) },
        ];
      case 'supply':
        return [
          { label: t.labels.manufacturer, value: view.manufacturer },
          { label: t.labels.economicOperator, value: view.operator },
          { label: t.labels.manufacturingPlace, value: view.facility },
          { label: t.labels.marketCode, value: firstOf(a, ['market_code']) },
        ];
      case 'circularity':
        return view.recycledMaterials.map((material, index) => ({
          label: `${t.labels.recycled} · ${['NICKEL', 'COBALT', 'LITHIUM', 'LEAD'][index]}`,
          value: material.value === undefined ? '' : `${material.value}%`,
        }));
      case 'carbon': {
        const fields = view.carbonStages.map((stage, index) => ({
          label: t.carbonStages[index],
          value: `${stage.amount} ${view.carbonUnit}`,
        }));
        if (view.carbonTotal) fields.unshift({ label: t.labels.total, value: `${view.carbonTotal} ${view.carbonUnit}` });
        return fields;
      }
      default:
        return [];
    }
  };

  const fields = fieldsFor(tab);
  const visibleFields = tab === 'general' ? fields : fields.filter((field) => field.value);

  return (
    <div className="public-page">
      <PublicHeader lang={lang} onLangChange={setLang} />

      <main className="pp-main ps-main">
        <div className="ps-top">
          <section className="pp-card ps-id-card">
            <div className="ps-id-head">
              <div>
                <div className="pp-label">{t.passportId}</div>
                <div className="ps-id">{view.passportId}</div>
              </div>
              {data?.passportStatus === 'PUBLISHED' ? <span className="pp-verified">{t.verified}</span> : null}
            </div>
            <div className="pp-label">{t.modelNumber}</div>
            <div className="ps-id-field">{view.model || '—'}</div>
            <div className="pp-label">{t.serialNumber}</div>
            <div className="ps-id-field">{view.serial || '—'}</div>
          </section>

          <figure className="ps-visual">
            <BatteryIllustration />
            <figcaption>{t.illustrationNote}</figcaption>
          </figure>

          <section className="ps-operator">
            <div className="pp-label ps-operator-label">{t.economicOperator}</div>
            <div className="ps-operator-card">{view.operator || '—'}</div>
          </section>
        </div>

        <button type="button" className="pp-back" onClick={goBack}>
          <span aria-hidden="true">←</span> {t.back}
        </button>

        <div className="ps-tabs" role="tablist" aria-label={t.tabs.general}>
          {TAB_ORDER.map((key) => (
            <button
              key={key}
              type="button"
              role="tab"
              aria-selected={tab === key}
              className={tab === key ? 'is-active' : ''}
              onClick={() => setTab(key)}
            >
              {t.tabs[key]}
            </button>
          ))}
        </div>

        <section className="pp-card ps-panel" role="tabpanel">
          <div className="ps-panel-head">
            <h2>{t.tabs[tab]}</h2>
            <p>{t.descriptions[tab]}</p>
          </div>
          {visibleFields.length ? (
            <dl className="ps-fields">
              {visibleFields.map((field) => (
                <div className="ps-field" key={field.label}>
                  <dt>{field.label}</dt>
                  <dd className={field.status && field.value ? 'ps-field-status' : undefined}>
                    {field.status && field.value ? <span className="pp-status-dot" /> : null}
                    {field.value || '—'}
                  </dd>
                </div>
              ))}
            </dl>
          ) : (
            <p className="ps-empty">{t.noData}</p>
          )}
        </section>
      </main>

      <PublicFooter lang={lang} schemaVersion={data?.schemaVersion} passportVersion={data?.passportVersion} />
    </div>
  );
}
