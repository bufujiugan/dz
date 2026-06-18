package com.dz.tavern.service;

import java.time.LocalDateTime;

public interface PointsHalvingService {
    void executeIfDue(LocalDateTime now);
}
