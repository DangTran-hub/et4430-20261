package vn.edu.et4430.service;

/**
 * Quy định theo mức cảnh báo học tập (student_profile.academic_warning_level).
 * Khớp với quy tắc của bản desktop: mức 0 được 12..24 TC, mức 1 tối đa 14 TC,
 * mức 2 tối đa 10 TC, mức 3 không được đăng ký.
 */
public final class WarningPolicy {

    private WarningPolicy() {
    }

    public static int maxCredits(int level) {
        return switch (level) {
            case 1 -> 14;
            case 2 -> 10;
            case 3 -> 0;
            default -> 24;
        };
    }

    public static int minCredits(int level) {
        return level == 0 ? 12 : 0;
    }

    public static boolean canRegister(int level) {
        return level < 3;
    }

    public static String label(int level) {
        return switch (level) {
            case 0 -> "Bình thường";
            case 1 -> "Cảnh báo mức 1";
            case 2 -> "Cảnh báo mức 2";
            case 3 -> "Cảnh báo mức 3";
            default -> "Không xác định";
        };
    }

    public static String description(int level) {
        return switch (level) {
            case 0 -> "Được đăng ký từ 12 đến 24 tín chỉ.";
            case 1 -> "Chỉ được đăng ký tối đa 14 tín chỉ.";
            case 2 -> "Chỉ được đăng ký tối đa 10 tín chỉ.";
            case 3 -> "Không được đăng ký học phần trong kỳ này. Liên hệ Phòng Đào tạo.";
            default -> "";
        };
    }
}
