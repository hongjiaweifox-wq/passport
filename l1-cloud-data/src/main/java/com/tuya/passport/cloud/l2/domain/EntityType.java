package com.tuya.passport.cloud.l2.domain;

public enum EntityType {
    BATTERY_MODEL("battery_model.csv", "电池型号模板.csv", new String[]{"schema_version", "source_system", "source_record_id", "org_code", "model_code", "revision_code", "manufacturer_name", "battery_category", "chemistry", "rated_capacity_kwh", "rated_voltage_v", "market_code", "effective_from", "sn_product_code", "dpp_attributes"}, new String[]{"模板版本", "来源系统", "来源记录编号", "组织编码", "型号编码", "修订版本", "制造商名称", "电池类别", "化学体系", "额定容量(kWh)", "额定电压(V)", "目标市场", "生效日期", "SN 产品型号代码", "DPP 扩展属性"}),
    BATTERY_UNIT("battery_unit.csv", "电池单体模板.csv", new String[]{"schema_version", "source_system", "source_record_id", "org_code", "model_code", "revision_code", "batch_code", "sn", "manufactured_at", "device_id", "dpp_attributes"}, new String[]{"模板版本", "来源系统", "来源记录编号", "组织编码", "型号编码", "修订版本", "批次编码", "序列号(SN)", "制造时间", "设备 ID", "DPP 扩展属性"}),
    LIFECYCLE_EVENT("lifecycle_event.csv", "生命周期事件模板.csv", new String[]{"schema_version", "source_system", "source_record_id", "org_code", "sn", "event_type", "event_time", "actor_org_code", "reason_code", "evidence_ref"}, new String[]{"模板版本", "来源系统", "来源记录编号", "组织编码", "序列号(SN)", "事件类型", "事件时间", "执行方组织编码", "原因编码", "证据引用"});

    private final String filename;
    private final String downloadFilename;
    private final String[] headers;
    private final String[] templateHeaders;
    EntityType(String filename, String downloadFilename, String[] headers, String[] templateHeaders) {
        this.filename = filename;
        this.downloadFilename = downloadFilename;
        this.headers = headers;
        this.templateHeaders = templateHeaders;
    }
    public String filename() { return filename; }
    public String downloadFilename() { return downloadFilename; }
    public String[] headers() { return headers.clone(); }
    public String[] templateHeaders() { return templateHeaders.clone(); }
    public int legacyHeaderLength() { return this == BATTERY_MODEL ? headers.length - 2 : this == BATTERY_UNIT ? headers.length - 1 : headers.length; }
}
