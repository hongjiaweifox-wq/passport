package com.tuya.passport.cloud.l2.service;

import java.util.Set;

public record L1Source(String id, String orgCode, String sourceCode, String sourceName, String sourceType, String status, Set<String> templateTypes) {}
