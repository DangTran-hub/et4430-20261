package vn.edu.et4430.web;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.et4430.entity.StudentProfile;
import vn.edu.et4430.service.BusinessException;
import vn.edu.et4430.service.StudentAccountService;

/** Phòng Đào tạo: quản lý tài khoản sinh viên. */
@Controller
@RequestMapping("/pdt/students")
public class PdtStudentController {

    private final StudentAccountService studentAccountService;

    public PdtStudentController(StudentAccountService studentAccountService) {
        this.studentAccountService = studentAccountService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("students", studentAccountService.listStudents());
        if (!model.containsAttribute("form")) {
            Map<String, String> empty = new HashMap<>();
            empty.put("programCode", "ET-E4");
            empty.put("warningLevel", "0");
            model.addAttribute("form", empty);
        }
        return "pdt/students";
    }

    @PostMapping
    public String create(@RequestParam Map<String, String> params, RedirectAttributes ra) {
        try {
            StudentProfile created = studentAccountService.create(new StudentAccountService.NewStudent(
                    params.get("username"),
                    params.get("password"),
                    params.get("fullName"),
                    params.get("email"),
                    params.get("studentCode"),
                    params.get("programCode"),
                    parseYear(params.get("cohortYear")),
                    params.get("classCode"),
                    parseLevel(params.get("warningLevel"))));
            ra.addFlashAttribute("info", "Đã tạo tài khoản sinh viên " + created.getStudentCode()
                    + " (" + created.getUser().getUsername() + ").");
        } catch (BusinessException e) {
            Map<String, String> keep = new HashMap<>(params);
            keep.remove("password");
            keep.remove("_csrf");
            ra.addFlashAttribute("form", keep);
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pdt/students";
    }

    @PostMapping("/{id}/warning")
    public String updateWarning(@PathVariable("id") Long id, @RequestParam("level") String level,
                                RedirectAttributes ra) {
        try {
            studentAccountService.updateWarningLevel(id, parseLevel(level));
            ra.addFlashAttribute("info", "Đã cập nhật mức cảnh báo.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pdt/students";
    }

    private static Integer parseYear(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("Khóa (năm nhập học) phải là số, ví dụ 2023.");
        }
    }

    private static int parseLevel(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("Mức cảnh báo không hợp lệ.");
        }
    }
}
