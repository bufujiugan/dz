package com.dz.tavern.common.model;

import java.util.List;

public record PageResult<T>(long current, long size, long total, List<T> records) {
}
