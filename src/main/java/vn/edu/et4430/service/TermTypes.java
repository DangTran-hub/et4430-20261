package vn.edu.et4430.service;

import vn.edu.et4430.entity.AcademicTerm;

/**
 * Loại học kỳ được xác định bằng academic_term.term_code:
 * SEMESTER_1 = kỳ lẻ, SEMESTER_2 = kỳ chẵn, SUMMER = kỳ hè.
 * course_term_rule.term_code dùng cùng các giá trị này.
 */
public final class TermTypes {

    public static final String SEMESTER_1 = "SEMESTER_1";
    public static final String SEMESTER_2 = "SEMESTER_2";
    public static final String SUMMER = "SUMMER";

    private TermTypes() {
    }

    public static String termName(String termCode) {
        if (termCode == null) {
            return "";
        }
        return switch (termCode) {
            case SEMESTER_1 -> "Học kỳ 1";
            case SEMESTER_2 -> "Học kỳ 2";
            case SUMMER -> "Học kỳ hè";
            default -> termCode;
        };
    }

    public static String typeName(String termCode) {
        if (termCode == null) {
            return "";
        }
        return switch (termCode) {
            case SEMESTER_1 -> "kỳ lẻ";
            case SEMESTER_2 -> "kỳ chẵn";
            case SUMMER -> "kỳ hè";
            default -> termCode;
        };
    }

    public static String fullName(AcademicTerm term) {
        return termName(term.getTermCode()) + " năm học " + term.getAcademicYear();
    }

    /** Năm bắt đầu của năm học "2026-2027" -> 2026; không đọc được thì trả về null. */
    public static Integer startYear(String academicYear) {
        if (academicYear == null || academicYear.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(academicYear.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
