package vn.edu.et4430.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Full replacement of the editable draft fields. Null dates are allowed in a draft. */
public record DraftSelection(List<Long> courseIds, OffsetDateTime registrationOpenAt,
                             OffsetDateTime registrationCloseAt) {
    public DraftSelection {
        courseIds = courseIds == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(courseIds));
    }
}
