import { useEffect, useState } from 'react';
import { Alert, Button, Card, Empty, Input, message, Modal, Select, Space, Table, Tabs, Tag, Typography } from 'antd';
import type { TableProps } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { l4Api, type ReviewTask, type ValidationRun, type ValidationRunDetail } from '../api/l4';
import { ManualRecordModal } from '../components/ManualRecordModal';
import { useDemoIdentity } from '../auth/demoIdentity';
import './LayerPages.css';

export function L4GovernancePage() {
  const identity = useDemoIdentity();
  const [msg, holder] = message.useMessage();
  const [queue, setQueue] = useState<ReviewTask[]>([]);
  const [runs, setRuns] = useState<ValidationRun[]>([]);
  const [status, setStatus] = useState('PENDING_REVIEW');
  const [loading, setLoading] = useState(false);
  const [active, setActive] = useState<ReviewTask>();
  const [reason, setReason] = useState('');
  const [runDetail, setRunDetail] = useState<ValidationRunDetail>();
  const [runLoading, setRunLoading] = useState(false);
  const [manualOpen, setManualOpen] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const q = await l4Api.reviews(status);
      setQueue(q.items);
    } catch (e) { msg.error(e instanceof Error ? e.message : '加载治理数据失败'); }
    finally { setLoading(false); }
  };
  const loadRuns = async () => {
    setRunLoading(true);
    try { setRuns((await l4Api.validationRuns()).items); }
    catch (e) { msg.error(e instanceof Error ? e.message : '加载校验记录失败'); }
    finally { setRunLoading(false); }
  };
  useEffect(() => { void load(); }, [status]);
  useEffect(() => { void loadRuns(); }, []);

  const decide = async (decision: 'APPROVE' | 'REQUEST_CHANGES') => {
    if (!active) return;
    try {
      await l4Api.decide(active.id, decision, reason);
      msg.success(decision === 'APPROVE' ? '已批准并生成发布候选' : '已退回修改');
      setActive(undefined);
      setReason('');
      await load();
    } catch (e) { msg.error(e instanceof Error ? e.message : '审核失败'); }
  };
  const showRun = async (run: ValidationRun) => {
    try { setRunDetail(await l4Api.validationRun(run.id)); }
    catch (e) { msg.error(e instanceof Error ? e.message : '校验详情加载失败'); }
  };

  const reviewColumns: TableProps<ReviewTask>['columns'] = [
    { title: '主体类型', dataIndex: 'subjectType' },
    { title: '主体 ID', dataIndex: 'subjectId', render: v => <Typography.Text code>{v}</Typography.Text> },
    { title: '提交人', dataIndex: 'makerId' },
    { title: '状态', dataIndex: 'status', render: v => <Tag color={v === 'PENDING_REVIEW' ? 'orange' : v === 'APPROVED' ? 'green' : 'red'}>{v}</Tag> },
    { title: '提交时间', dataIndex: 'createdAt', render: v => new Date(v).toLocaleString('zh-CN') },
    { title: '操作', render: (_, r) => r.status === 'PENDING_REVIEW' && ['COMPLIANCE_CHECKER', 'ADMIN'].includes(identity.role) ? <Button type="link" onClick={() => setActive(r)}>审核</Button> : '—' },
  ];
  const runColumns: TableProps<ValidationRun>['columns'] = [
    { title: '完成时间', dataIndex: 'finishedAt', render: v => new Date(v).toLocaleString('zh-CN') },
    { title: '主体类型', dataIndex: 'subjectType' },
    { title: '主体 ID', dataIndex: 'subjectId', render: v => <Typography.Text code>{v}</Typography.Text> },
    { title: '结果', dataIndex: 'status', render: v => <Tag color={v === 'PASS' ? 'green' : v === 'FAIL' ? 'red' : 'orange'}>{v}</Tag> },
    { title: '错误 / 警告', render: (_, r) => `${r.errorCount} / ${r.warningCount}` },
    { title: '输入快照', dataIndex: 'inputHash', render: v => <Typography.Text code copyable>{String(v).slice(0, 14)}…</Typography.Text> },
    { title: '详情', render: (_, r) => <Button type="link" onClick={() => void showRun(r)}>问题列表</Button> },
  ];

  return <div className="data-page layer-page">{holder}
    <div className="page-heading"><div><Typography.Text className="eyebrow">L4 · GOVERNANCE</Typography.Text><Typography.Title level={2}>数据治理与合规审核</Typography.Title><Typography.Paragraph type="secondary">运行规则校验，并由独立 checker 完成 maker-checker 审核。字段定义与 DPP 完整性请见「定义与规范」。</Typography.Paragraph></div><Space><Select value={status} onChange={setStatus} style={{ width: 190 }} options={[{ value: 'PENDING_REVIEW', label: '待审核' }, { value: 'APPROVED', label: '已批准' }, { value: 'CHANGES_REQUESTED', label: '已退回' }]} /><Button onClick={() => { void load(); void loadRuns(); }}>刷新</Button><Button type="primary" icon={<PlusOutlined />} onClick={() => setManualOpen(true)}>补录主数据</Button></Space></div>
    <Card className="registry-card"><Tabs items={[
      { key: 'reviews', label: '审核队列', children: <Table rowKey="id" columns={reviewColumns} dataSource={queue} loading={loading} pagination={{ defaultPageSize: 10, showSizeChanger: true, showTotal: total => `共 ${total} 条` }} locale={{ emptyText: <Empty description="当前没有审核任务"/> }}/> },
      { key: 'runs', label: '校验记录', children: <Table rowKey="id" columns={runColumns} dataSource={runs} loading={runLoading} pagination={{ defaultPageSize: 10, showSizeChanger: true, showTotal: total => `共 ${total} 条` }} locale={{ emptyText: <Empty description="暂无校验记录"/> }}/> },
    ]}/></Card>
    <Modal open={Boolean(active)} title="数据审核决策" onCancel={() => setActive(undefined)} footer={<Space><Button onClick={() => void decide('REQUEST_CHANGES')}>退回修改</Button><Button type="primary" onClick={() => void decide('APPROVE')}>批准</Button></Space>}><p>主体：{active?.subjectType} · {active?.subjectId}</p><Input.TextArea value={reason} onChange={e => setReason(e.target.value)} placeholder="填写审核理由（必填）" rows={4}/><Alert type="info" message="checker 不能与 maker 为同一身份。" style={{ marginTop: 12 }}/></Modal>
    <Modal open={Boolean(runDetail)} title="校验记录详情" onCancel={() => setRunDetail(undefined)} footer={<Button onClick={() => setRunDetail(undefined)}>关闭</Button>} width={680}>
      {runDetail && <><Typography.Paragraph>主体：{runDetail.run.subjectType} · <Typography.Text code>{runDetail.run.subjectId}</Typography.Text></Typography.Paragraph><Typography.Paragraph>结果：<Tag color={runDetail.run.status === 'PASS' ? 'green' : runDetail.run.status === 'FAIL' ? 'red' : 'orange'}>{runDetail.run.status}</Tag> 错误 {runDetail.run.errorCount} · 警告 {runDetail.run.warningCount}</Typography.Paragraph><Table rowKey="id" size="small" pagination={false} dataSource={runDetail.issues} locale={{ emptyText: <Empty description="没有校验问题"/> }} columns={[{ title: '字段', dataIndex: 'fieldCode' }, { title: '级别', dataIndex: 'severity', render: v => <Tag color={v === 'ERROR' ? 'red' : v === 'WARN' ? 'orange' : 'blue'}>{v}</Tag> }, { title: '规则', dataIndex: 'code' }, { title: '说明', dataIndex: 'message' }]}/></>}
    </Modal>
    <ManualRecordModal open={manualOpen} onClose={() => setManualOpen(false)} onCreated={() => { void load(); void loadRuns(); }}/>
  </div>;
}
