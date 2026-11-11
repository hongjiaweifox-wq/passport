package com.tuya.passport.cloud.l1.api.dto;

import java.util.List;

public record PageResponse<T>(List<T> items, long total, int page, int size) {}
