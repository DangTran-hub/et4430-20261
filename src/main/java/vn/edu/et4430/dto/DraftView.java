package vn.edu.et4430.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record DraftView(Long id, Long termId, Long sourceCampaignId, String status,
                        List<Long> courseIds, OffsetDateTime registrationOpenAt,
                        OffsetDateTime registrationCloseAt, OffsetDateTime createdAt,
                        OffsetDateTime updatedAt) {
    public DraftView { courseIds = List.copyOf(courseIds); }
}
