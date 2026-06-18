package com.dz.tavern.service;

import java.time.LocalDate;
import java.util.Map;

public interface ReconcileService {
    Map<String, Object> reconcileDaily(LocalDate date);
}
