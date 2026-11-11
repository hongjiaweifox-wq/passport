import { useEffect, useMemo, useState } from 'react';
import { Result, Spin } from 'antd';
import { QRCodeSVG } from 'qrcode.react';
import { getPublicPassport, type PublicPassport } from '../api/publicPassport';
import { LayerApiError } from '../api/client';
import {
  type Dict, type Lang, firstOf, numeric, statusText, formatDate, isPublicAuthenticated,
  PublicHeader, PublicFooter, usePublicLang,
} from './publicPassportShared';
import './PublicPassportPage.css';

type Segment = { value: number; color: string };

const COPY: Record<Lang, {
  passportId: string; verified: string; modelNumber: string; serialNumber: string;
  economicOperator: string; category: string; weight: string; status: string;
  manufacturedDate: string; manufacturedBy: string; viewMore: string; summaryReport: string;
  originalPower: string; notProvided: string; carbonFootprint: string; carbonTotal: string;
  noCarbon: string; carbonStages: [string, string, string, string];
  recycledContent: string; unverified: string; materialNames: [string, string, string, string];
  noData: string; recycledShare: (value: number) => string; primaryMaterial: (value: number) => string;
  errorTitle: string; errorSuspended: string;
}> = {
  en: {
    passportId: 'Passport ID',
    verified: 'Verified',
    modelNumber: 'Model Number',
    serialNumber: 'Serial Number',
    economicOperator: 'Economic operator',
    category: 'Category',
    weight: 'Weight',
    status: 'Status',
    manufacturedDate: 'Manufactured date',
    manufacturedBy: 'Manufactured by',
    viewMore: 'View more about this passport',
    summaryReport: 'Summary report',
    originalPower: 'Original power',
    notProvided: 'Not provided',
    carbonFootprint: 'Carbon footprint',
    carbonTotal: 'Total',
    noCarbon: 'No carbon footprint data in this passport.',
    carbonStages: ['raw material extraction', 'main production', 'distribution', 'recycling'],
    recycledContent: 'Recycled content share',
    unverified: 'Unverified',
    materialNames: ['NICKEL', 'COBALT', 'LITHIUM', 'LEAD'],
    noData: 'No data provided',
    recycledShare: (value) => `Recycled content share: ${value}%`,
    primaryMaterial: (value) => `Primary material: ${value}%`,
    errorTitle: 'Passport unavailable',
    errorSuspended: 'This passport is suspended or unavailable',
  },
  zh: {
    passportId: '护照标识',
    verified: '已验证',
    modelNumber: '型号',
    serialNumber: '序列号',
    economicOperator: '经济运营者',
    category: '电池类别',
    weight: '重量',
    status: '电池状态',
    manufacturedDate: '制造日期',
    manufacturedBy: '制造商',
    viewMore: '查看护照完整信息',
    summaryReport: '摘要报告',
    originalPower: '原始功率',
    notProvided: '未提供',
    carbonFootprint: '碳足迹',
    carbonTotal: '合计',
    noCarbon: '该护照未提供碳足迹数据。',
    carbonStages: ['原材料开采', '主产品生产', '分销', '回收'],
    recycledContent: '再生材料含量',
    unverified: '未验证',
    materialNames: ['镍', '钴', '锂', '铅'],
    noData: '暂无数据',
    recycledShare: (value) => `再生材料含量：${value}%`,
    primaryMaterial: (value) => `原生材料：${value}%`,
    errorTitle: '护照暂不可用',
    errorSuspended: '护照已暂停或不可公开',
  },
};

function Donut({ segments, center }: { segments: Segment[]; center?: string }) {
  const total = segments.reduce((sum, item) => sum + Math.max(item.value, 0), 0);
  let cursor = 0;
  const stops = total > 0
    ? segments.map((item) => {
        const from = (cursor / total) * 100;
        cursor += Math.max(item.value, 0);
        const to = (cursor / total) * 100;
        return `${item.color} ${from}% ${to}%`;
      }).join(', ')
    : '#d6d9dc 0% 100%';
  return (
    <div className="pp-donut" style={{ background: `conic-gradient(${stops})` }}>
      <div className="pp-donut-hole">{center ?? ''}</div>
    </div>
  );
}

function Fact({ label, value, wide }: { label: string; value: string; wide?: boolean }) {
  return (
    <div className={wide ? 'pp-fact pp-fact-wide' : 'pp-fact'}>
      <span className="pp-label">{label}</span>
      <span className={value ? 'pp-value' : 'pp-value pp-value-empty'}>{value || '—'}</span>
    </div>
  );
}

export function usePassportView(data: PublicPassport | undefined, publicCode: string) {
  return useMemo(() => {
    const attributes: Dict = data?.data ?? {};
    const passportId = firstOf(attributes, ['dpp_identifier', 'unique_battery_identifier']) || publicCode;
    const manufacturer = firstOf(attributes, ['manufacturer_information', 'manufacturer_name', 'manufactured_by']);
    const operator = firstOf(attributes, ['economic_operator_information', 'economic_operator_name', 'economic_operator']) || manufacturer;
    const mass = firstOf(attributes, ['battery_mass', 'battery_weight', 'weight']);
    const massUnit = firstOf(attributes, ['battery_mass_unit', 'mass_unit']) || 'kg';
    const massText = mass ? (/[a-z]/i.test(mass) ? mass : `${mass} ${massUnit}`) : '';

    const carbonStages = ([
      { color: '#2e9e5b', key: 'contribution_of_raw_material_acquisition_and_pre_processing_lifecycle_stage' },
      { color: '#e8894a', key: 'contribution_of_main_product_production_lifecycle_stage' },
      { color: '#c9c9c9', key: 'contribution_of_distribution_lifecycle_stage' },
      { color: '#7f8c9b', key: 'contribution_of_end_of_life_and_recycling_lifecycle_stage' },
    ] as Array<{ color: string; key: string; amount?: number }>)
      .map((stage) => ({ ...stage, amount: numeric(attributes[stage.key]) }))
      .filter((stage): stage is { color: string; key: string; amount: number } => stage.amount !== undefined && stage.amount >= 0);

    const carbonUnit = firstOf(attributes, ['carbon_footprint_unit', 'battery_carbon_footprint_unit']) || 'gCO₂e/kWh';
    const carbonTotal = firstOf(attributes, ['battery_carbon_footprint_per_functional_unit', 'absolute_battery_carbon_footprint']);

    const recycledMaterials = [
      { key: 'nickel_recycled_content_share' },
      { key: 'cobalt_recycled_content_share' },
      { key: 'lithium_recycled_content_share' },
      { key: 'lead_recycled_content_share' },
    ].map((material) => {
      const value = numeric(attributes[material.key]);
      return { ...material, value: value === undefined ? undefined : Math.max(0, Math.min(100, value)) };
    });

    return {
      attributes,
      passportId,
      model: firstOf(attributes, ['model_code', 'battery_model_identifier', 'model_number']),
      serial: firstOf(attributes, ['battery_serial_number', 'serial_number', 'serial']),
      operator,
      category: firstOf(attributes, ['battery_category', 'category']),
      mass: massText,
      batteryStatus: firstOf(attributes, ['battery_status', 'battery_lifecycle_status', 'lifecycle_status']),
      manufacturedDate: formatDate(firstOf(attributes, ['manufacturing_date', 'manufactured_at', 'manufacture_date'])),
      manufacturer,
      facility: firstOf(attributes, ['manufacturing_place', 'facility_id', 'unique_facility_identifier']),
      originalPower: firstOf(attributes, ['original_power_capability', 'original_power', 'maximum_permitted_battery_power']),
      carbonStages,
      carbonUnit,
      carbonTotal,
      recycledMaterials,
    };
  }, [data, publicCode]);
}

export function PublicPassportPage({ publicCode }: { publicCode: string }) {
  const [data, setData] = useState<PublicPassport>();
  const [error, setError] = useState('');
  const [httpStatus, setHttpStatus] = useState<number>();
  const [loading, setLoading] = useState(true);
  const [lang, setLang] = usePublicLang();
  const t = COPY[lang];

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

  const view = usePassportView(data, publicCode);
  const passportUrl = typeof window === 'undefined' ? '' : `${window.location.origin}/#/p/${publicCode}`;
  const openSummary = () => {
    window.location.hash = `/p/${encodeURIComponent(publicCode)}/summary`;
  };
  const openAccount = () => {
    if (isPublicAuthenticated()) {
      openSummary();
      return;
    }
    window.location.hash = `/auth?callback=${encodeURIComponent(`/p/${publicCode}/summary`)}`;
  };

  if (loading) {
    return (
      <div className="public-page pp-state">
        <Spin size="large" />
      </div>
    );
  }
  if (error) {
    return (
      <div className="public-page pp-state">
        <Result status={httpStatus === 410 ? '403' : '404'} title={httpStatus === 410 ? t.errorSuspended : t.errorTitle} subTitle={error} />
      </div>
    );
  }

  const carbonSegments = view.carbonStages.map((stage) => ({ value: stage.amount, color: stage.color }));
  const verified = data?.passportStatus === 'PUBLISHED';

  return (
    <div className="public-page">
      <PublicHeader lang={lang} onLangChange={setLang} onAccount={openAccount} accountLabel={lang === 'zh' ? '登录' : 'Sign in'} />

      <main className="pp-main">
        <div className="pp-top">
          <section className="pp-card pp-summary-card">
            <div className="pp-id-row">
              <div>
                <div className="pp-label">{t.passportId}</div>
                <div className="pp-id">{view.passportId}</div>
              </div>
              {verified ? (
                <span className="pp-verified">
                  <svg width="12" height="12" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                    <circle cx="8" cy="8" r="7" fill="#ffffff" fillOpacity="0.22" />
                    <path d="M4.6 8.2 7 10.5l4.4-4.8" stroke="#ffffff" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  {t.verified}
                </span>
              ) : null}
            </div>

            <div className="pp-facts">
              <Fact label={t.modelNumber} value={view.model} />
              <Fact label={t.serialNumber} value={view.serial} />
              <div className="pp-operator">
                <span className="pp-label">{t.economicOperator}</span>
                <div className="pp-operator-card">{view.operator ? <span>{view.operator}</span> : <span className="pp-muted">—</span>}</div>
              </div>
              <Fact label={t.category} value={view.category} />
              <Fact label={t.weight} value={view.mass} />
              {view.batteryStatus ? (
                <div className="pp-fact">
                  <span className="pp-label">{t.status}</span>
                  <span className="pp-value pp-status"><span className="pp-status-dot" />{statusText(view.batteryStatus, lang)}</span>
                </div>
              ) : (
                <Fact label={t.status} value="" />
              )}
              <Fact label={t.manufacturedDate} value={view.manufacturedDate} />
              <Fact label={t.manufacturedBy} value={view.manufacturer} wide />
            </div>
          </section>

          <aside className="pp-side">
            <div className="pp-qr">
              {passportUrl ? <QRCodeSVG value={passportUrl} size={150} level="M" fgColor="#111111" bgColor="#ffffff" /> : null}
            </div>
            <button type="button" className="pp-more" onClick={openSummary}>
              {t.viewMore}
              <span aria-hidden="true">→</span>
            </button>
          </aside>
        </div>

        <section className="pp-card pp-report">
          <h2 className="pp-report-title">{t.summaryReport}</h2>

          <div className="pp-report-row">
            <div className="pp-report-label">{t.originalPower}</div>
            <div className={view.originalPower ? 'pp-report-value' : 'pp-report-value pp-muted'}>{view.originalPower || t.notProvided}</div>
          </div>

          <div className="pp-report-row">
            <div className="pp-report-label">{t.carbonFootprint}</div>
            {view.carbonStages.length > 0 ? (
              <>
                {view.carbonTotal ? <div className="pp-report-note">{t.carbonTotal}: {view.carbonTotal} {view.carbonUnit}</div> : null}
                <div className="pp-carbon">
                  <Donut segments={carbonSegments} />
                  <ul className="pp-legend">
                    {view.carbonStages.map((stage) => (
                      <li key={stage.key}>
                        <span className="pp-swatch" style={{ background: stage.color }} />
                        {t.carbonStages[[
                          'contribution_of_raw_material_acquisition_and_pre_processing_lifecycle_stage',
                          'contribution_of_main_product_production_lifecycle_stage',
                          'contribution_of_distribution_lifecycle_stage',
                          'contribution_of_end_of_life_and_recycling_lifecycle_stage',
                        ].indexOf(stage.key)]}: {stage.amount} {view.carbonUnit}
                      </li>
                    ))}
                  </ul>
                </div>
              </>
            ) : (
              <div className="pp-report-value pp-muted">{t.noCarbon}</div>
            )}
          </div>

          <div className="pp-report-row">
            <div className="pp-report-label pp-report-label-inline">
              {t.recycledContent}
              <span className="pp-unverified">{t.unverified}</span>
            </div>
            <div className="pp-materials">
              {view.recycledMaterials.map((material, index) => {
                const value = material.value;
                const segments: Segment[] = value === undefined
                  ? []
                  : [{ value, color: '#2e9e5b' }, { value: Math.max(0, 100 - value), color: '#b9bec3' }];
                return (
                  <div className="pp-material" key={material.key}>
                    <div className="pp-material-name">{t.materialNames[index]}</div>
                    <Donut segments={segments} center={value === undefined ? '—' : `${value}%`} />
                    <ul className="pp-legend pp-legend-compact">
                      {value === undefined ? (
                        <li><span className="pp-swatch" style={{ background: '#b9bec3' }} />{t.noData}</li>
                      ) : (
                        <>
                          <li><span className="pp-swatch" style={{ background: '#2e9e5b' }} />{t.recycledShare(value)}</li>
                          <li><span className="pp-swatch" style={{ background: '#b9bec3' }} />{t.primaryMaterial(Math.max(0, 100 - value))}</li>
                        </>
                      )}
                    </ul>
                  </div>
                );
              })}
            </div>
          </div>
        </section>
      </main>

      <PublicFooter lang={lang} schemaVersion={data?.schemaVersion} passportVersion={data?.passportVersion} />
    </div>
  );
}
