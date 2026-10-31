package com.successhub.appdomain.dto.gamification;

import java.util.List;
import java.util.UUID;

public record ScheduleTasksRequest(List<UUID> taskIds) {}
