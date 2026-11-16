package vn.edu.et4430.web;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.et4430.entity.CampaignCourse;
import vn.edu.et4430.entity.CourseRegistration;
import vn.edu.et4430.entity.RegistrationCampaign;
import vn.edu.et4430.entity.StudentProfile;
import vn.edu.et4430.repository.CampaignCourseRepository;
import vn.edu.et4430.repository.NotificationRepository;
import vn.edu.et4430.repository.RegistrationCampaignRepository;
import vn.edu.et4430.service.BusinessException;
import vn.edu.et4430.service.CampaignService;
import vn.edu.et4430.service.StudentRegistrationService;
import vn.edu.et4430.service.WarningPolicy;

/** Màn hình sinh viên: UC1 bước 6–8 và đăng ký học phần. */
@Controller
@RequestMapping("/sv")
public class StudentController {

    private final StudentRegistrationService registrationService;
    private final NotificationRepository notificationRepo;
    private final RegistrationCampaignRepository campaignRepo;
    private final CampaignCourseRepository campaignCourseRepo;

    public StudentController(StudentRegistrationService registrationService,
                             NotificationRepository notificationRepo,
                             RegistrationCampaignRepository campaignRepo,
                             CampaignCourseRepository campaignCourseRepo) {
        this.registrationService = registrationService;
        this.notificationRepo = notificationRepo;
        this.campaignRepo = campaignRepo;
        this.campaignCourseRepo = campaignCourseRepo;
    }

    /** Thông báo, đợt đăng ký và mức cảnh báo học tập của chính sinh viên. */
    @GetMapping
    public String index(Principal principal, Model model) {
        StudentProfile student = registrationService.getStudent(principal.getName());
        model.addAttribute("student", student);
        model.addAttribute("notifications", notificationRepo.findAllByOrderByPublishedAtDesc());
        model.addAttribute("campaigns", campaignRepo.findByStatusOrderByPublishedAtDesc(CampaignService.PUBLISHED));
        return "sv/index";
    }

    /** Danh sách học phần của đợt + đăng ký. */
    @GetMapping("/campaigns/{id}")
    public String campaign(@PathVariable("id") Long id, Principal principal, Model model, RedirectAttributes ra) {
        RegistrationCampaign campaign = campaignRepo.findById(id)
                .filter(c -> CampaignService.PUBLISHED.equals(c.getStatus()))
                .orElse(null);
        if (campaign == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đợt đăng ký.");
            return "redirect:/sv";
        }
        StudentProfile student = registrationService.getStudent(principal.getName());
        List<CampaignCourse> courses = campaignCourseRepo
                .findByCampaign_IdAndOfferingStatusOrderByCourse_CourseCodeAsc(id, CampaignService.OFFERING_OPEN);
        List<CourseRegistration> regs = registrationService.registrations(student.getId(), id);
        Set<Long> registeredIds = regs.stream()
                .map(r -> r.getCampaignCourse().getId())
                .collect(Collectors.toSet());
        BigDecimal total = StudentRegistrationService.totalCredits(regs);
        int level = student.getAcademicWarningLevel();

        model.addAttribute("campaign", campaign);
        model.addAttribute("student", student);
        model.addAttribute("courses", courses);
        model.addAttribute("registrations", regs);
        model.addAttribute("registeredIds", registeredIds);
        model.addAttribute("totalCredits", total);
        model.addAttribute("maxCredits", WarningPolicy.maxCredits(level));
        model.addAttribute("minCredits", WarningPolicy.minCredits(level));
        model.addAttribute("belowMin", total.compareTo(BigDecimal.valueOf(WarningPolicy.minCredits(level))) < 0);
        model.addAttribute("windowOpen", CampaignService.isRegistrationOpen(campaign));
        model.addAttribute("canRegister", WarningPolicy.canRegister(level));
        return "sv/campaign";
    }

    @PostMapping("/campaigns/{id}/register")
    public String register(@PathVariable("id") Long id, @RequestParam("campaignCourseId") Long ccId,
                           Principal principal, RedirectAttributes ra) {
        try {
            registrationService.register(principal.getName(), ccId);
            ra.addFlashAttribute("info", "Đăng ký học phần thành công.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sv/campaigns/" + id;
    }

    @PostMapping("/campaigns/{id}/unregister")
    public String unregister(@PathVariable("id") Long id, @RequestParam("campaignCourseId") Long ccId,
                             Principal principal, RedirectAttributes ra) {
        try {
            registrationService.unregister(principal.getName(), ccId);
            ra.addFlashAttribute("info", "Đã hủy đăng ký học phần.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sv/campaigns/" + id;
    }
}
