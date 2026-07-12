package com.example.seedbe.domain.project.dto;

public record ProjectStatusCountResponse(
        long totalCount,
        long inProgressCount,
        long completedCount
) {
}
