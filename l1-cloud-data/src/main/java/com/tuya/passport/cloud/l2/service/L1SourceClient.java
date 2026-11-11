package com.tuya.passport.cloud.l2.service;

import java.util.List;

public interface L1SourceClient {
    List<L1Source> list(String orgCode);
    L1Source requireByCode(String orgCode, String code);
}
