package com.tuya.passport.cloud.l1.domain;

public enum TemplateType {
    BATTERY_MODEL("battery_model.csv", "电池型号模板.csv"),
    BATTERY_UNIT("battery_unit.csv", "电池单体模板.csv"),
    LIFECYCLE_EVENT("lifecycle_event.csv", "生命周期事件模板.csv");

    private final String filename;
    private final String downloadFilename;
    TemplateType(String filename, String downloadFilename) { this.filename = filename; this.downloadFilename = downloadFilename; }
    public String filename() { return filename; }
    public String downloadFilename() { return downloadFilename; }
}
