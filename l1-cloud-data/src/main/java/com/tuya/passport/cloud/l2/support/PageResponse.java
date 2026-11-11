package com.tuya.passport.cloud.l2.support;

import java.util.List;
public record PageResponse<T>(List<T> items, long total, int page, int size) {}
