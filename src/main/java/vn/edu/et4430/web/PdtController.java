package vn.edu.et4430.web;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.et4430.entity.CampaignCourse;
import vn.edu.et4430.entity.Course;
import vn.edu.et4430.entity.RegistrationCampaign;
import vn.edu.et4430.repository.AcademicTermRepository;
import vn.edu.et4430.repository.CampaignCourseRepository;
import vn.edu.et4430.repository.CourseRegistrationRepository;
import vn.edu.et4430.repository.CourseRepository;
import vn.edu.et4430.repository.RegistrationCampaignRepository;
import vn.edu.et4430.service.BusinessException;
import vn.edu.et4430.service.CampaignService;
import vn.edu.et4430.service.TermService;
import vn.edu.et4430.entity.AcademicTerm;

/** Màn hình Phòng Đào tạo – UC1. */
@Controller
@RequestMapping("/pdt")
public class PdtController {

    private final CampaignService campaignService;
    private final AcademicTermRepository termRepo;
    private final RegistrationCampaignRepository campaignRepo;
    private final CampaignCourseRepository campaignCourseRepo;
    private final CourseRepository courseRepo;
    private final CourseRegistrationRepository registrationRepo;
    private final TermService termService;

    public PdtController(CampaignService campaignService,
                         TermService termService,
                         AcademicTermRepository termRepo,
                         RegistrationCampaignRepository campaignRepo,
                         CampaignCourseRepository campaignCourseRepo,
                         CourseRepository courseRepo,
                         CourseRegistrationRepository registrationRepo) {
        this.campaignService = campaignService;
        this.termService = termService;
        this.termRepo = termRepo;
        this.campaignRepo = campaignRepo;
        this.campaignCourseRepo = campaignCourseRepo;
        this.courseRepo = courseRepo;
        this.registrationRepo = registrationRepo;
    }

    /** Danh sách học kỳ + đợt đăng ký; chọn học kỳ để mở đợt mới (UC1 bước 1–2). */
    @GetMapping
    public String index(Model model) {
        List<RegistrationCampaign> campaigns = campaignRepo.findByStatusNotOrderByCreatedAtDesc(CampaignService.CANCELLED);
        Map<Long, RegistrationCampaign> campaignByTerm = new HashMap<>();
        for (RegistrationCampaign c : campaigns) {
            campaignByTerm.putIfAbsent(c.getTerm().getId(), c);
        }
        model.addAttribute("terms", termRepo.findAllByOrderByAcademicYearDescTermCodeAsc());
        model.addAttribute("campaignByTerm", campaignByTerm);
        model.addAttribute("campaigns", campaigns);
        model.addAttribute("currentTerm", termService.currentTerm().orElse(null));
        model.addAttribute("nextTerm", termService.suggestNext());
        return "pdt/index";
    }

    /** Nút "Tạo học kỳ mới": tạo kỳ ngay sau kỳ mới nhất (20261 -> 20262 -> 20263 -> 20271). */
    @PostMapping("/terms/next")
    public String createNextTerm(RedirectAttributes ra) {
        try {
            AcademicTerm term = termService.createNext();
            ra.addFlashAttribute("info", "Đã tạo học kỳ " + TermService.code(term)
                    + ". Bấm \"Mở đợt\" ở dòng học kỳ này để mở đăng ký học phần.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pdt";
    }

    /** Sửa ngày bắt đầu / kết thúc học kỳ. */
    @PostMapping("/terms/{id}/dates")
    public String updateTermDates(@PathVariable("id") Long id,
                                  @RequestParam(name = "start", required = false) String start,
                                  @RequestParam(name = "end", required = false) String end,
                                  RedirectAttributes ra) {
        try {
            AcademicTerm term = termService.updateDates(id, start, end);
            ra.addFlashAttribute("info", "Đã cập nhật thời gian học kỳ " + TermService.code(term) + ".");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pdt";
    }

    /** UC1 bước 3: hệ thống chuẩn bị đợt nháp. */
    @PostMapping("/campaigns/prepare")
    public String prepare(@RequestParam("termId") Long termId, Principal principal, RedirectAttributes ra) {
        try {
            CampaignService.PrepareResult result = campaignService.prepare(termId, principal.getName());
            ra.addFlashAttribute("info", result.message());
            return "redirect:/pdt/campaigns/" + result.campaign().getId();
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/pdt";
        }
    }

    /** UC1 bước 4: xem và chỉnh sửa đợt. */
    @GetMapping("/campaigns/{id}")
    public String campaign(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        RegistrationCampaign campaign;
        try {
            campaign = campaignService.get(id);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/pdt";
        }
        List<CampaignCourse> courses = campaignCourseRepo.findByCampaign_IdOrderByCourse_CourseCodeAsc(id);
        Set<Long> inCampaign = new HashSet<>();
        Map<Long, Long> regCounts = new HashMap<>();
        for (CampaignCourse cc : courses) {
            inCampaign.add(cc.getCourse().getId());
            regCounts.put(cc.getId(), registrationRepo.countByCampaignCourse_Id(cc.getId()));
        }
        List<Course> available = courseRepo.findByActiveTrueOrderByCourseCodeAsc().stream()
                .filter(c -> !inCampaign.contains(c.getId()))
                .toList();

        model.addAttribute("campaign", campaign);
        model.addAttribute("courses", courses);
        model.addAttribute("regCounts", regCounts);
        model.addAttribute("availableCourses", available);
        model.addAttribute("editable", CampaignService.DRAFT.equals(campaign.getStatus()));
        model.addAttribute("datesEditable", !CampaignService.CANCELLED.equals(campaign.getStatus()));
        return "pdt/campaign";
    }

    @PostMapping("/campaigns/{id}/dates")
    public String updateDates(@PathVariable("id") Long id,
                              @RequestParam(name = "openAt", required = false) String openAt,
                              @RequestParam(name = "closeAt", required = false) String closeAt,
                              RedirectAttributes ra) {
        return run(id, ra, "Đã lưu thời gian đăng ký.", () -> campaignService.updateDates(id, openAt, closeAt));
    }

    @PostMapping("/campaigns/{id}/courses/{ccId}/toggle")
    public String toggleCourse(@PathVariable("id") Long id, @PathVariable("ccId") Long ccId, RedirectAttributes ra) {
        return run(id, ra, null, () -> campaignService.toggleCourse(id, ccId));
    }

    @PostMapping("/campaigns/{id}/courses/add")
    public String addCourse(@PathVariable("id") Long id, @RequestParam("courseId") Long courseId, RedirectAttributes ra) {
        return run(id, ra, "Đã thêm học phần.", () -> campaignService.addCourse(id, courseId));
    }

    /** UC1 bước 5: xác nhận mở đợt, công bố thông báo. */
    @PostMapping("/campaigns/{id}/publish")
    public String publish(@PathVariable("id") Long id, Principal principal, RedirectAttributes ra) {
        return run(id, ra, "Đã mở đợt đăng ký và công bố thông báo cho sinh viên.",
                () -> campaignService.publish(id, principal.getName()));
    }

    @PostMapping("/campaigns/{id}/cancel")
    public String cancel(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            campaignService.cancelDraft(id);
            ra.addFlashAttribute("info", "Đã hủy bản nháp. Có thể chuẩn bị lại đợt cho học kỳ này.");
            return "redirect:/pdt";
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/pdt/campaigns/" + id;
        }
    }

    private String run(Long id, RedirectAttributes ra, String successMessage, Runnable action) {
        try {
            action.run();
            if (successMessage != null) {
                ra.addFlashAttribute("info", successMessage);
            }
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pdt/campaigns/" + id;
    }
}
