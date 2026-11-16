package vn.edu.et4430.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.et4430.entity.CampaignCourse;
import vn.edu.et4430.entity.CourseRegistration;
import vn.edu.et4430.entity.RegistrationCampaign;
import vn.edu.et4430.entity.StudentProfile;
import vn.edu.et4430.repository.CampaignCourseRepository;
import vn.edu.et4430.repository.CourseRegistrationRepository;
import vn.edu.et4430.repository.StudentProfileRepository;

/** Sinh viên đăng ký / hủy học phần trong một đợt đã công bố. */
@Service
public class StudentRegistrationService {

    private final StudentProfileRepository studentRepo;
    private final CampaignCourseRepository campaignCourseRepo;
    private final CourseRegistrationRepository registrationRepo;

    public StudentRegistrationService(StudentProfileRepository studentRepo,
                                      CampaignCourseRepository campaignCourseRepo,
                                      CourseRegistrationRepository registrationRepo) {
        this.studentRepo = studentRepo;
        this.campaignCourseRepo = campaignCourseRepo;
        this.registrationRepo = registrationRepo;
    }

    @Transactional(readOnly = true)
    public StudentProfile getStudent(String username) {
        return studentRepo.findByUser_Username(username)
                .orElseThrow(() -> new BusinessException("Tài khoản này không có hồ sơ sinh viên."));
    }

    @Transactional(readOnly = true)
    public List<CourseRegistration> registrations(Long studentId, Long campaignId) {
        return registrationRepo.findByStudent_IdAndCampaignCourse_Campaign_Id(studentId, campaignId);
    }

    public static BigDecimal totalCredits(List<CourseRegistration> regs) {
        return regs.stream()
                .map(r -> r.getCampaignCourse().getCourse().getCredits())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void register(String username, Long campaignCourseId) {
        StudentProfile student = getStudent(username);
        CampaignCourse cc = campaignCourseRepo.findById(campaignCourseId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy học phần."));
        RegistrationCampaign campaign = cc.getCampaign();

        checkWindow(campaign);
        int level = student.getAcademicWarningLevel();
        if (!WarningPolicy.canRegister(level)) {
            throw new BusinessException("Bạn đang ở " + WarningPolicy.label(level)
                    + " nên không được đăng ký học phần.");
        }
        if (!CampaignService.OFFERING_OPEN.equals(cc.getOfferingStatus())) {
            throw new BusinessException("Học phần " + cc.getCourse().getCourseCode() + " không mở trong đợt này.");
        }
        if (registrationRepo.existsByStudent_IdAndCampaignCourse_Id(student.getId(), cc.getId())) {
            throw new BusinessException("Bạn đã đăng ký học phần " + cc.getCourse().getCourseCode() + ".");
        }

        BigDecimal current = totalCredits(registrations(student.getId(), campaign.getId()));
        BigDecimal after = current.add(cc.getCourse().getCredits());
        int max = WarningPolicy.maxCredits(level);
        if (after.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new BusinessException("Vượt giới hạn tín chỉ: " + plain(current) + " + "
                    + plain(cc.getCourse().getCredits()) + " > " + max + " tín chỉ ("
                    + WarningPolicy.label(level) + ").");
        }

        CourseRegistration reg = new CourseRegistration();
        reg.setStudent(student);
        reg.setCampaignCourse(cc);
        reg.setStatus(CourseRegistration.REGISTERED);
        registrationRepo.save(reg);
    }

    @Transactional
    public void unregister(String username, Long campaignCourseId) {
        StudentProfile student = getStudent(username);
        CourseRegistration reg = registrationRepo.findByStudent_IdAndCampaignCourse_Id(student.getId(), campaignCourseId)
                .orElseThrow(() -> new BusinessException("Bạn chưa đăng ký học phần này."));
        checkWindow(reg.getCampaignCourse().getCampaign());
        registrationRepo.delete(reg);
    }

    private static void checkWindow(RegistrationCampaign campaign) {
        if (!CampaignService.isRegistrationOpen(campaign)) {
            throw new BusinessException("Ngoài thời gian đăng ký (" + AppTime.display(campaign.getRegistrationOpenAt())
                    + " – " + AppTime.display(campaign.getRegistrationCloseAt()) + ").");
        }
    }

    public static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
