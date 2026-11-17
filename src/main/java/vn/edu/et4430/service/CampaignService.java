package vn.edu.et4430.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.et4430.entity.AcademicTerm;
import vn.edu.et4430.entity.CampaignCourse;
import vn.edu.et4430.entity.Course;
import vn.edu.et4430.entity.CourseTermRule;
import vn.edu.et4430.entity.Notification;
import vn.edu.et4430.entity.RegistrationCampaign;
import vn.edu.et4430.entity.UserAccount;
import vn.edu.et4430.repository.AcademicTermRepository;
import vn.edu.et4430.repository.CampaignCourseRepository;
import vn.edu.et4430.repository.CourseRepository;
import vn.edu.et4430.repository.CourseTermRuleRepository;
import vn.edu.et4430.repository.NotificationRepository;
import vn.edu.et4430.repository.RegistrationCampaignRepository;
import vn.edu.et4430.repository.UserAccountRepository;

/**
 * UC1 – Thông báo kế hoạch đăng ký học phần.
 * Vòng đời đợt: DRAFT (PĐT chuẩn bị, sửa) -> PUBLISHED (công bố + tạo thông báo).
 * Bản nháp có thể hủy (CANCELLED) để chuẩn bị lại.
 */
@Service
public class CampaignService {

    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String CANCELLED = "CANCELLED";

    public static final String OFFERING_OPEN = "OPEN";
    public static final String OFFERING_REMOVED = "REMOVED";

    /** Kết quả bước chuẩn bị: đợt nháp vừa tạo và câu giải thích cho PĐT. */
    public record PrepareResult(RegistrationCampaign campaign, String message) {
    }

    private final AcademicTermRepository termRepo;
    private final RegistrationCampaignRepository campaignRepo;
    private final CampaignCourseRepository campaignCourseRepo;
    private final CourseRepository courseRepo;
    private final CourseTermRuleRepository ruleRepo;
    private final NotificationRepository notificationRepo;
    private final UserAccountRepository userRepo;

    public CampaignService(AcademicTermRepository termRepo,
                           RegistrationCampaignRepository campaignRepo,
                           CampaignCourseRepository campaignCourseRepo,
                           CourseRepository courseRepo,
                           CourseTermRuleRepository ruleRepo,
                           NotificationRepository notificationRepo,
                           UserAccountRepository userRepo) {
        this.termRepo = termRepo;
        this.campaignRepo = campaignRepo;
        this.campaignCourseRepo = campaignCourseRepo;
        this.courseRepo = courseRepo;
        this.ruleRepo = ruleRepo;
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
    }

    /** UC1 bước 2–3 (và các nhánh 3a, 3b, 4a): tạo đợt nháp cho học kỳ được chọn. */
    @Transactional
    public PrepareResult prepare(Long termId, String pdtUsername) {
        AcademicTerm term = termRepo.findById(termId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học kỳ đã chọn."));

        // 4a. Đợt đăng ký đã tồn tại
        if (campaignRepo.existsByTerm_IdAndStatusNot(termId, CANCELLED)) {
            throw new BusinessException("Đợt đăng ký cho " + TermTypes.fullName(term)
                    + " đã tồn tại. Không tạo thêm đợt trùng lặp.");
        }

        UserAccount pdt = findUser(pdtUsername);

        // 3. Tìm đợt trước cùng loại học kỳ để kế thừa
        List<RegistrationCampaign> previous = campaignRepo.findPreviousOfSameTermType(
                term.getTermCode(), term.getAcademicYear(), PUBLISHED);
        RegistrationCampaign source = previous.isEmpty() ? null : previous.get(0);

        // Học phần đang có quy tắc mở cho loại học kỳ này
        Map<Long, Course> ruleCourses = new LinkedHashMap<>();
        for (CourseTermRule rule : ruleRepo.findByTermCodeAndActiveTrue(term.getTermCode())) {
            Course course = rule.getCourse();
            if (course.isActive()) {
                ruleCourses.putIfAbsent(course.getId(), course);
            }
        }

        RegistrationCampaign campaign = new RegistrationCampaign();
        campaign.setTerm(term);
        campaign.setStatus(DRAFT);
        campaign.setCreatedBy(pdt);
        campaign.setCopiedFromCampaign(source);

        List<CampaignCourse> newCourses = new ArrayList<>();
        String message;

        if (source != null) {
            // Kế thừa: thời gian đăng ký dời theo số năm, danh sách học phần của đợt trước
            int years = yearsBetween(source.getTerm(), term);
            if (source.getRegistrationOpenAt() != null) {
                campaign.setRegistrationOpenAt(source.getRegistrationOpenAt().plusYears(years));
            }
            if (source.getRegistrationCloseAt() != null) {
                campaign.setRegistrationCloseAt(source.getRegistrationCloseAt().plusYears(years));
            }
            int inherited = 0;
            for (CampaignCourse old : campaignCourseRepo.findByCampaign_IdOrderByCourse_CourseCodeAsc(source.getId())) {
                if (!OFFERING_OPEN.equals(old.getOfferingStatus()) || !old.getCourse().isActive()) {
                    continue;
                }
                CampaignCourse cc = new CampaignCourse();
                cc.setCampaign(campaign);
                cc.setCourse(old.getCourse());
                cc.setCopiedFromCampaignCourse(old);
                cc.setOfferingStatus(OFFERING_OPEN);
                newCourses.add(cc);
                ruleCourses.remove(old.getCourse().getId());
                inherited++;
            }
            // Học phần mới có quy tắc mở nhưng đợt trước chưa có
            for (Course course : ruleCourses.values()) {
                newCourses.add(newCampaignCourse(campaign, course));
            }
            message = "Đã kế thừa " + inherited + " học phần và thời gian đăng ký từ đợt "
                    + TermTypes.fullName(source.getTerm())
                    + (ruleCourses.isEmpty() ? "." : "; bổ sung " + ruleCourses.size()
                    + " học phần mới theo quy tắc mở " + TermTypes.typeName(term.getTermCode()) + ".")
                    + " Hãy kiểm tra lại thời gian đăng ký trước khi công bố.";
        } else {
            // 3a. Không có đợt trước cùng loại học kỳ -> chọn theo quy tắc mở học phần
            for (Course course : ruleCourses.values()) {
                newCourses.add(newCampaignCourse(campaign, course));
            }
            message = "Chưa có đợt đăng ký " + TermTypes.typeName(term.getTermCode())
                    + " trước đó để kế thừa. Hệ thống đã chọn " + ruleCourses.size()
                    + " học phần theo quy tắc mở; vui lòng nhập thời gian đăng ký.";
        }

        // 3b. Không có học phần phù hợp -> không tạo đợt
        if (newCourses.isEmpty()) {
            throw new BusinessException("Không có học phần phù hợp với " + TermTypes.typeName(term.getTermCode())
                    + ". Vui lòng kiểm tra lại danh mục học phần và cấu hình học kỳ mở.");
        }

        campaignRepo.save(campaign);
        campaignCourseRepo.saveAll(newCourses);
        return new PrepareResult(campaign, message);
    }

    /**
     * UC1 bước 4: sửa thời gian đăng ký. Đợt nháp sửa tự do; đợt đã công bố vẫn sửa được
     * (ví dụ gia hạn) và thông báo gửi sinh viên được cập nhật theo thời gian mới.
     */
    @Transactional
    public void updateDates(Long campaignId, String openAt, String closeAt) {
        RegistrationCampaign campaign = get(campaignId);
        if (CANCELLED.equals(campaign.getStatus())) {
            throw new BusinessException("Đợt đăng ký đã hủy, không thể sửa.");
        }
        boolean published = PUBLISHED.equals(campaign.getStatus());
        OffsetDateTime open = AppTime.parseInput(openAt);
        OffsetDateTime close = AppTime.parseInput(closeAt);
        if (published && (open == null || close == null)) {
            throw new BusinessException("Đợt đã công bố phải có đủ thời gian bắt đầu và kết thúc.");
        }
        if (open != null && close != null && !close.isAfter(open)) {
            throw new BusinessException("Thời gian kết thúc đăng ký phải sau thời gian bắt đầu.");
        }
        campaign.setRegistrationOpenAt(open);
        campaign.setRegistrationCloseAt(close);
        touch(campaign);
        if (published) {
            notificationRepo.findByCampaign_Id(campaignId).ifPresent(n -> {
                n.setBody(notificationBody(campaign.getTerm(), open, close)
                        + " (Cập nhật thời gian ngày " + AppTime.display(AppTime.now()) + ".)");
                notificationRepo.save(n);
            });
        }
    }

    /** UC1 bước 4: bỏ / mở lại một học phần trong đợt nháp. */
    @Transactional
    public void toggleCourse(Long campaignId, Long campaignCourseId) {
        RegistrationCampaign campaign = getDraft(campaignId);
        CampaignCourse cc = campaignCourseRepo.findById(campaignCourseId)
                .filter(c -> c.getCampaign().getId().equals(campaign.getId()))
                .orElseThrow(() -> new BusinessException("Không tìm thấy học phần trong đợt này."));
        cc.setOfferingStatus(OFFERING_OPEN.equals(cc.getOfferingStatus()) ? OFFERING_REMOVED : OFFERING_OPEN);
        touch(campaign);
    }

    /** UC1 bước 4: thêm học phần ngoài danh sách gợi ý. */
    @Transactional
    public void addCourse(Long campaignId, Long courseId) {
        RegistrationCampaign campaign = getDraft(campaignId);
        Course course = courseRepo.findById(courseId)
                .filter(Course::isActive)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học phần."));
        if (campaignCourseRepo.existsByCampaign_IdAndCourse_Id(campaignId, courseId)) {
            throw new BusinessException("Học phần " + course.getCourseCode() + " đã có trong đợt.");
        }
        campaignCourseRepo.save(newCampaignCourse(campaign, course));
        touch(campaign);
    }

    /** UC1 bước 5: PĐT xác nhận -> công bố đợt và tạo thông báo (cùng một giao dịch). */
    @Transactional
    public void publish(Long campaignId, String pdtUsername) {
        RegistrationCampaign campaign = getDraft(campaignId);
        OffsetDateTime open = campaign.getRegistrationOpenAt();
        OffsetDateTime close = campaign.getRegistrationCloseAt();
        if (open == null || close == null) {
            throw new BusinessException("Cần nhập đủ thời gian bắt đầu và kết thúc đăng ký trước khi công bố.");
        }
        if (!close.isAfter(open)) {
            throw new BusinessException("Thời gian kết thúc đăng ký phải sau thời gian bắt đầu.");
        }
        if (campaignCourseRepo.countByCampaign_IdAndOfferingStatus(campaignId, OFFERING_OPEN) == 0) {
            throw new BusinessException("Đợt đăng ký phải có ít nhất một học phần được mở.");
        }

        UserAccount pdt = findUser(pdtUsername);
        OffsetDateTime now = AppTime.now();
        campaign.setStatus(PUBLISHED);
        campaign.setPublishedAt(now);
        touch(campaign);

        if (!notificationRepo.existsByCampaign_Id(campaignId)) {
            AcademicTerm term = campaign.getTerm();
            Notification n = new Notification();
            n.setCampaign(campaign);
            n.setTitle("Thông báo đăng ký học phần " + TermTypes.fullName(term));
            n.setBody(notificationBody(term, open, close));
            n.setPublishedAt(now);
            n.setCreatedBy(pdt);
            notificationRepo.save(n);
        }
    }

    /** Hủy bản nháp để chuẩn bị lại từ đầu. */
    @Transactional
    public void cancelDraft(Long campaignId) {
        RegistrationCampaign campaign = getDraft(campaignId);
        campaign.setStatus(CANCELLED);
        touch(campaign);
    }

    @Transactional(readOnly = true)
    public RegistrationCampaign get(Long campaignId) {
        return campaignRepo.findById(campaignId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đợt đăng ký."));
    }

    /** "Sắp mở" / "Đang mở" / "Đã đóng" theo thời gian hiện tại. */
    public static String windowStatus(RegistrationCampaign c) {
        if (!PUBLISHED.equals(c.getStatus())) {
            return DRAFT.equals(c.getStatus()) ? "Bản nháp" : "Đã hủy";
        }
        OffsetDateTime now = AppTime.now();
        if (c.getRegistrationOpenAt() != null && now.isBefore(c.getRegistrationOpenAt())) {
            return "Sắp mở";
        }
        if (c.getRegistrationCloseAt() != null && now.isAfter(c.getRegistrationCloseAt())) {
            return "Đã đóng";
        }
        return "Đang mở";
    }

    public static boolean isRegistrationOpen(RegistrationCampaign c) {
        return "Đang mở".equals(windowStatus(c));
    }

    private RegistrationCampaign getDraft(Long campaignId) {
        RegistrationCampaign campaign = get(campaignId);
        if (!DRAFT.equals(campaign.getStatus())) {
            throw new BusinessException("Chỉ sửa được đợt đăng ký đang ở trạng thái nháp.");
        }
        return campaign;
    }

    private static String notificationBody(AcademicTerm term, OffsetDateTime open, OffsetDateTime close) {
        return "Phòng Đào tạo thông báo thời gian đăng ký học phần " + TermTypes.fullName(term)
                + ": từ " + AppTime.display(open) + " đến " + AppTime.display(close) + ". "
                + "Sinh viên bị cảnh báo học tập chỉ được đăng ký số tín chỉ tối đa theo mức cảnh báo "
                + "(mức 1: 14 tín chỉ, mức 2: 10 tín chỉ); sinh viên cảnh báo mức 3 không được đăng ký. "
                + "Sinh viên đăng nhập hệ thống để xem danh sách học phần và mức cảnh báo của mình.";
    }

    private UserAccount findUser(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản " + username));
    }

    private static CampaignCourse newCampaignCourse(RegistrationCampaign campaign, Course course) {
        CampaignCourse cc = new CampaignCourse();
        cc.setCampaign(campaign);
        cc.setCourse(course);
        cc.setOfferingStatus(OFFERING_OPEN);
        return cc;
    }

    private static void touch(RegistrationCampaign campaign) {
        campaign.setUpdatedAt(AppTime.now());
    }

    private static int yearsBetween(AcademicTerm from, AcademicTerm to) {
        Integer a = TermTypes.startYear(from.getAcademicYear());
        Integer b = TermTypes.startYear(to.getAcademicYear());
        return (a == null || b == null) ? 0 : b - a;
    }
}
