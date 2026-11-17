package vn.edu.et4430.dto;

import java.math.BigDecimal;
import java.util.List;

public record DraftPreparation(Long termId, String academicYear, String termCode,
                               SemesterType semesterType, Long sourceCampaignId,
                               List<CourseOption> eligibleCourses, List<Long> selectedCourseIds,
                               List<String> messages) {
    public enum SemesterType { ODD, EVEN, SUMMER }
    public record CourseOption(Long id, String code, String name, BigDecimal credits) {}

    public DraftPreparation {
        eligibleCourses = List.copyOf(eligibleCourses);
        selectedCourseIds = List.copyOf(selectedCourseIds);
        messages = List.copyOf(messages);
    }
}
