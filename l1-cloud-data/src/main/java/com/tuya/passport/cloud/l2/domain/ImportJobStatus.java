package com.tuya.passport.cloud.l2.domain;

public enum ImportJobStatus {
    UPLOADED, SCANNING, PARSING, VALIDATING, PUBLISHING, COMPLETED, PARTIAL, FAILED;
    public boolean terminal() { return this == COMPLETED || this == PARTIAL || this == FAILED; }
    public boolean processing() { return !terminal(); }
}
