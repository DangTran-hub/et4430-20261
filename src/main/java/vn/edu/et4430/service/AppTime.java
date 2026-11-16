package vn.edu.et4430.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Toàn hệ thống dùng giờ Việt Nam để nhập và hiển thị thời gian. */
public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private AppTime() {
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now(ZONE);
    }

    public static String display(OffsetDateTime time) {
        return time == null ? "chưa đặt" : time.atZoneSameInstant(ZONE).format(DISPLAY);
    }

    /** Giá trị cho ô input type="datetime-local". */
    public static String input(OffsetDateTime time) {
        return time == null ? "" : time.atZoneSameInstant(ZONE).format(INPUT);
    }

    /** Đọc giá trị từ ô datetime-local (giờ Việt Nam); ô trống thì trả về null. */
    public static OffsetDateTime parseInput(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim()).atZone(ZONE).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            throw new BusinessException("Thời gian không hợp lệ: " + value);
        }
    }
}
