# 电池护照 L1–L5 本地集成原型

本仓库提供统一管理台与单体 Spring Boot 后端，按 L1 数据源登记、L2 CSV 接入、L3 结构化主数据、L4 数据治理、L5 护照服务组织代码与 API，支持从导入到匿名公开查询的演示闭环。

## 模块

- `l1-cloud-data`：唯一 Spring Boot 后端（Java 17、Spring Boot 3、H2），端口 8080，提供 `/api/v1/l1/**` 至 `/api/v1/l5/**`，以及匿名 `/api/v1/public/passports/**`。
- `l1-web`：唯一 React + Ant Design 管理台，端口 5174；包含来源、结构化数据、治理审核、护照发布、定义与规范（DPP 完整性 / 字段字典 / SN 编码规则）页面及 `#/p/{publicCode}` 公开护照页面。
- `docs/方案/`：L1–L6 分层规范与技术方案，以及[数据库表 ER 关系架构](docs/方案/数据库表ER关系架构.html)（对照本地原型与目标生产蓝图）；`docs/需求/`：需求方案。

数据源、导入任务、staging 候选记录及上传 CSV 持久化在 `l1-cloud-data/data/`。开发环境使用本地 H2 文件数据库和本地文件存储。

## 启动

终端 1（唯一后端）：

```bash
cd l1-cloud-data && mvn spring-boot:run
```

终端 2（唯一前端）：

```bash
cd l1-web && npm install && npm run dev
```

打开统一管理台：<http://localhost:5174>。无需启动独立的 L2 服务或第二个前端。

## 演示闭环

1. 在 L1 登记数据源，在 L2 上传 `BATTERY_MODEL`、`BATTERY_UNIT`、`LIFECYCLE_EVENT` CSV；导入完成后候选行仍留在 staging。
2. 在导入任务详情手动“提升到 L3”。行级提升记录保留结果；失败项可通过再次提升重试。
3. 在 L3 查看型号、修订和单体；单体支持设备绑定、追加生命周期事件，并可运行 L4 校验、提交审核。
4. 切换演示身份：Alice（`COMPLIANCE_MAKER`）提交，Bob（`COMPLIANCE_CHECKER`）独立批准或退回，保留 maker-checker 约束。
5. 切换 Carol（`PASSPORT_OPERATOR`）创建/发布 L5 护照。发布需要 READY 审核候选；发布时检查 L3 快照未漂移。
6. 通过护照公开码打开 `#/p/{publicCode}`，或请求 `GET /api/v1/public/passports/{publicCode}`。公开响应仅含 PUBLIC 投影，支持 ETag/304；暂停护照返回 HTTP 410。

## 原型范围与边界

- L2 仅支持 CSV：`BATTERY_MODEL`、`BATTERY_UNIT`、`LIFECYCLE_EVENT`，schema `dpp-schema/1.0`。支持异步任务、结构/语法校验、行级错误、错误 CSV、幂等、重试和 staging 候选记录；扫描结果为模拟 clean，不代表安全扫描或法规合规结论。
- 当前原型使用 H2 + Hibernate `ddl-auto: update`，L3/L4/L5 原型表以 `l3_* / l4_* / l5_*` 命名；目标生产蓝图中的 `bp_*` 表并未迁移，详情见 ER 架构文档。
- L2 CSV 未提供 `issuer_code`，当前 L3 原型使用 `org_code` 作为 issuer 命名空间；SN 按 trim、Unicode NFC、uppercase 规范化，并保留原始值。
- 管理 API 使用 `X-Org-Code`（默认 `DEMO-ORG`）、`X-User`、`X-Role` 演示租户和角色头。它们仅用于本地原型演示，不是身份认证或生产级授权；后端保留基本角色门禁与 maker-checker 检查。
- 尚未接入 OIDC/正式 RBAC、ClamAV、对象存储、MQ/outbox、Flyway/Liquibase、生产级灾备与持久性保证。

## 验证

```bash
cd l1-cloud-data && mvn test
cd ../l1-web && npm run build && npm test
```
