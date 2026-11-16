package vn.edu.et4430.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.et4430.entity.AcademicTerm;
import vn.edu.et4430.entity.CampaignCourse;
import vn.edu.et4430.entity.Course;
import vn.edu.et4430.entity.CourseTermRule;
import vn.edu.et4430.entity.Notification;
import vn.edu.et4430.entity.RegistrationCampaign;
import vn.edu.et4430.entity.StudentProfile;
import vn.edu.et4430.entity.UserAccount;
import vn.edu.et4430.repository.AcademicTermRepository;
import vn.edu.et4430.repository.CampaignCourseRepository;
import vn.edu.et4430.repository.CourseRepository;
import vn.edu.et4430.repository.CourseTermRuleRepository;
import vn.edu.et4430.repository.NotificationRepository;
import vn.edu.et4430.repository.RegistrationCampaignRepository;
import vn.edu.et4430.repository.StudentProfileRepository;
import vn.edu.et4430.repository.UserAccountRepository;
import vn.edu.et4430.service.AppTime;
import vn.edu.et4430.service.CampaignService;
import vn.edu.et4430.service.TermTypes;

/**
 * Dữ liệu demo, chỉ tạo khi DB chưa có tài khoản "pdt".
 * Tắt bằng app.demo-data=false.
 *
 * Kịch bản demo UC1:
 *  - 2026-2027 Học kỳ 2  -> kế thừa từ đợt 2025-2026 Học kỳ 2 (đã công bố)
 *  - 2027-2028 Học kỳ 1  -> 3a: chưa có đợt kỳ lẻ trước đó, chọn theo quy tắc mở
 *  - 2026-2027 Học kỳ hè -> 3b: không có học phần mở kỳ hè
 *  - chuẩn bị lại một học kỳ đã có đợt -> 4a
 */
@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final UserAccountRepository userRepo;
    private final StudentProfileRepository studentRepo;
    private final AcademicTermRepository termRepo;
    private final CourseRepository courseRepo;
    private final CourseTermRuleRepository ruleRepo;
    private final RegistrationCampaignRepository campaignRepo;
    private final CampaignCourseRepository campaignCourseRepo;
    private final NotificationRepository notificationRepo;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserAccountRepository userRepo,
                               StudentProfileRepository studentRepo,
                               AcademicTermRepository termRepo,
                               CourseRepository courseRepo,
                               CourseTermRuleRepository ruleRepo,
                               RegistrationCampaignRepository campaignRepo,
                               CampaignCourseRepository campaignCourseRepo,
                               NotificationRepository notificationRepo,
                               PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.studentRepo = studentRepo;
        this.termRepo = termRepo;
        this.courseRepo = courseRepo;
        this.ruleRepo = ruleRepo;
        this.campaignRepo = campaignRepo;
        this.campaignCourseRepo = campaignCourseRepo;
        this.notificationRepo = notificationRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepo.findByUsername("pdt").isPresent()) {
            return;
        }
        log.info("Tạo dữ liệu demo (tắt bằng app.demo-data=false)");

        // ---- Tài khoản ----
        UserAccount pdt = user("pdt", "pdt123", "Phòng Đào tạo", "pdt@example.edu.vn", "PDT");
        student("sv01", "20230001", "Nguyễn Văn An", 0);
        student("sv02", "20230002", "Trần Thị Bình", 1);
        student("sv03", "20230003", "Lê Văn Cường", 2);
        student("sv04", "20230004", "Phạm Thị Dung", 3);

        // ---- Học kỳ ----
        AcademicTerm past = term("2025-2026", TermTypes.SEMESTER_2, "2026-02-16", "2026-06-30", "FINISHED");
        term("2026-2027", TermTypes.SEMESTER_1, "2026-09-07", "2027-01-17", "ACTIVE");
        term("2026-2027", TermTypes.SEMESTER_2, "2027-02-22", "2027-07-04", "PLANNING");
        term("2026-2027", TermTypes.SUMMER, "2027-07-12", "2027-08-22", "PLANNING");
        term("2027-2028", TermTypes.SEMESTER_1, "2027-09-06", "2028-01-16", "PLANNING");

        // ---- Học phần + quy tắc mở theo loại học kỳ ----
        String s1 = TermTypes.SEMESTER_1;
        String s2 = TermTypes.SEMESTER_2;
        Map<String, Course> c = new HashMap<>();
        c.put("ET4430", course("ET4430", "Lập trình nâng cao", 3, s1, s2));
        c.put("ET3260", course("ET3260", "Cơ sở dữ liệu", 3, s2));
        c.put("ET3230", course("ET3230", "Mạng máy tính", 3, s2));
        c.put("ET4250", course("ET4250", "Hệ thống nhúng", 3, s2));
        c.put("MI1121", course("MI1121", "Giải tích II", 3, s2));
        c.put("SSH1111", course("SSH1111", "Triết học Mác-Lênin", 3, s1, s2));
        c.put("PE1014", course("PE1014", "Giáo dục thể chất", 1, s1, s2));
        c.put("ET2031", course("ET2031", "Kỹ thuật lập trình C/C++", 3, s1));
        c.put("ET3210", course("ET3210", "Trường điện từ", 3, s1));
        c.put("ET4020", course("ET4020", "Xử lý tín hiệu số", 3, s1));
        c.put("MI1111", course("MI1111", "Giải tích I", 4, s1));

        // ---- Đợt đăng ký đã công bố của năm trước (nguồn kế thừa) ----
        RegistrationCampaign old = new RegistrationCampaign();
        old.setTerm(past);
        old.setStatus(CampaignService.PUBLISHED);
        old.setCreatedBy(pdt);
        old.setRegistrationOpenAt(vn("2026-01-05T07:00"));
        old.setRegistrationCloseAt(vn("2026-01-12T17:00"));
        old.setPublishedAt(vn("2025-12-20T09:00"));
        campaignRepo.save(old);
        for (String code : new String[]{"ET4430", "ET3260", "ET3230", "MI1121", "SSH1111"}) {
            CampaignCourse cc = new CampaignCourse();
            cc.setCampaign(old);
            cc.setCourse(c.get(code));
            cc.setOfferingStatus(CampaignService.OFFERING_OPEN);
            campaignCourseRepo.save(cc);
        }
        Notification n = new Notification();
        n.setCampaign(old);
        n.setTitle("Thông báo đăng ký học phần " + TermTypes.fullName(past));
        n.setBody("Thời gian đăng ký: từ " + AppTime.display(old.getRegistrationOpenAt())
                + " đến " + AppTime.display(old.getRegistrationCloseAt()) + ".");
        n.setPublishedAt(old.getPublishedAt());
        n.setCreatedBy(pdt);
        notificationRepo.save(n);
    }

    private UserAccount user(String username, String password, String fullName, String email, String role) {
        UserAccount u = new UserAccount();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setFullName(fullName);
        u.setEmail(email);
        u.setRole(role);
        u.setActive(true);
        return userRepo.save(u);
    }

    private void student(String username, String code, String fullName, int warningLevel) {
        UserAccount u = user(username, "sv123", fullName, username + "@example.edu.vn", "STUDENT");
        StudentProfile p = new StudentProfile();
        p.setUser(u);
        p.setStudentCode(code);
        p.setProgramCode("ET-E4");
        p.setCohortYear(2023);
        p.setClassCode("ET-E4 01");
        p.setAcademicWarningLevel(warningLevel);
        studentRepo.save(p);
    }

    private AcademicTerm term(String year, String code, String start, String end, String status) {
        AcademicTerm t = new AcademicTerm();
        t.setAcademicYear(year);
        t.setTermCode(code);
        t.setTermStartDate(LocalDate.parse(start));
        t.setTermEndDate(LocalDate.parse(end));
        t.setStatus(status);
        return termRepo.save(t);
    }

    private Course course(String code, String name, int credits, String... termCodes) {
        Course course = new Course();
        course.setCourseCode(code);
        course.setCourseName(name);
        course.setCredits(BigDecimal.valueOf(credits));
        course.setActive(true);
        courseRepo.save(course);
        for (String termCode : termCodes) {
            CourseTermRule rule = new CourseTermRule();
            rule.setCourse(course);
            rule.setTermCode(termCode);
            rule.setActive(true);
            ruleRepo.save(rule);
        }
        return course;
    }

    private static OffsetDateTime vn(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(AppTime.ZONE).toOffsetDateTime();
    }
}
