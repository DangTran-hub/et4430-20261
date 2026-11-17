package vn.edu.et4430.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.et4430.entity.AcademicTerm;
import vn.edu.et4430.repository.AcademicTermRepository;

/**
 * Học kỳ và mã kỳ kiểu 20261 = năm học 2026-2027 kỳ 1, 20262 = kỳ 2, 20263 = kỳ hè.
 * "Tạo học kỳ mới" luôn tạo kỳ ngay sau kỳ mới nhất đang có: 1 -> 2 -> hè -> 1 của năm sau.
 */
@Service
public class TermService {

    public static final String PLANNING = "PLANNING";

    /** Học kỳ sẽ được tạo khi bấm nút. */
    public record NextTerm(String code, String academicYear, String termCode, LocalDate start, LocalDate end) {
        public String name() {
            return TermTypes.termName(termCode) + " năm học " + academicYear;
        }
    }

    private final AcademicTermRepository termRepo;

    public TermService(AcademicTermRepository termRepo) {
        this.termRepo = termRepo;
    }

    /** Mã kỳ: 2026-2027 + SEMESTER_1 -> "20261". */
    public static String code(AcademicTerm term) {
        Integer year = TermTypes.startYear(term.getAcademicYear());
        return year == null ? term.getAcademicYear() : year + String.valueOf(order(term.getTermCode()));
    }

    /** Thứ tự trong năm học: kỳ 1 = 1, kỳ 2 = 2, hè = 3. */
    public static int order(String termCode) {
        if (TermTypes.SEMESTER_1.equals(termCode)) {
            return 1;
        }
        if (TermTypes.SEMESTER_2.equals(termCode)) {
            return 2;
        }
        return 3;
    }

    private static String termCodeOf(int order) {
        return switch (order) {
            case 1 -> TermTypes.SEMESTER_1;
            case 2 -> TermTypes.SEMESTER_2;
            default -> TermTypes.SUMMER;
        };
    }

    private static String academicYear(int startYear) {
        return startYear + "-" + (startYear + 1);
    }

    private static final Comparator<AcademicTerm> CHRONOLOGICAL = Comparator
            .comparing((AcademicTerm t) -> {
                Integer y = TermTypes.startYear(t.getAcademicYear());
                return y == null ? 0 : y;
            })
            .thenComparing(t -> order(t.getTermCode()));

    /** Học kỳ đang diễn ra hôm nay; nếu không có thì kỳ mới nhất đã bắt đầu. */
    @Transactional(readOnly = true)
    public Optional<AcademicTerm> currentTerm() {
        LocalDate today = AppTime.now().toLocalDate();
        List<AcademicTerm> terms = termRepo.findAll();
        Optional<AcademicTerm> running = terms.stream()
                .filter(t -> !today.isBefore(t.getTermStartDate()) && !today.isAfter(t.getTermEndDate()))
                .findFirst();
        if (running.isPresent()) {
            return running;
        }
        return terms.stream()
                .filter(t -> !t.getTermStartDate().isAfter(today))
                .max(CHRONOLOGICAL);
    }

    @Transactional(readOnly = true)
    public NextTerm suggestNext() {
        List<AcademicTerm> terms = termRepo.findAll();
        Optional<AcademicTerm> latest = terms.stream().max(CHRONOLOGICAL);

        int startYear;
        int order;
        if (latest.isEmpty()) {
            LocalDate today = AppTime.now().toLocalDate();
            startYear = today.getMonthValue() >= 8 ? today.getYear() : today.getYear() - 1;
            order = today.getMonthValue() >= 8 || today.getMonthValue() == 1 ? 1 : 2;
        } else {
            Integer y = TermTypes.startYear(latest.get().getAcademicYear());
            int lastOrder = order(latest.get().getTermCode());
            startYear = y == null ? AppTime.now().getYear() : y;
            if (lastOrder >= 3) {
                startYear++;
                order = 1;
            } else {
                order = lastOrder + 1;
            }
        }
        String year = academicYear(startYear);
        String termCode = termCodeOf(order);

        // Ngày học: lấy cùng kỳ của năm trước rồi cộng 1 năm; không có thì nối tiếp kỳ trước
        LocalDate start;
        LocalDate end;
        Optional<AcademicTerm> lastYearSame = termRepo.findByAcademicYearAndTermCode(academicYear(startYear - 1), termCode);
        if (lastYearSame.isPresent()) {
            start = lastYearSame.get().getTermStartDate().plusYears(1);
            end = lastYearSame.get().getTermEndDate().plusYears(1);
        } else {
            start = latest.map(t -> t.getTermEndDate().plusWeeks(2))
                    .orElse(order == 1 ? LocalDate.of(startYear, 9, 7) : LocalDate.of(startYear + 1, 2, 16));
            end = start.plusWeeks(order == 3 ? 6 : 20);
        }
        return new NextTerm(startYear + String.valueOf(order), year, termCode, start, end);
    }

    @Transactional
    public AcademicTerm createNext() {
        NextTerm next = suggestNext();
        if (termRepo.findByAcademicYearAndTermCode(next.academicYear(), next.termCode()).isPresent()) {
            throw new BusinessException("Học kỳ " + next.code() + " đã tồn tại.");
        }
        AcademicTerm term = new AcademicTerm();
        term.setAcademicYear(next.academicYear());
        term.setTermCode(next.termCode());
        term.setTermStartDate(next.start());
        term.setTermEndDate(next.end());
        term.setStatus(PLANNING);
        return termRepo.save(term);
    }

    /** PĐT sửa ngày bắt đầu / kết thúc của học kỳ. */
    @Transactional
    public AcademicTerm updateDates(Long termId, String start, String end) {
        AcademicTerm term = termRepo.findById(termId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học kỳ."));
        LocalDate s;
        LocalDate e;
        try {
            s = LocalDate.parse(start);
            e = LocalDate.parse(end);
        } catch (RuntimeException ex) {
            throw new BusinessException("Cần nhập đủ ngày bắt đầu và ngày kết thúc học kỳ.");
        }
        if (!e.isAfter(s)) {
            throw new BusinessException("Ngày kết thúc học kỳ phải sau ngày bắt đầu.");
        }
        term.setTermStartDate(s);
        term.setTermEndDate(e);
        return termRepo.save(term);
    }
}
