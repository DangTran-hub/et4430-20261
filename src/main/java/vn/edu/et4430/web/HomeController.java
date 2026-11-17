package vn.edu.et4430.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Sau khi đăng nhập: PĐT vào /pdt, sinh viên vào /sv. */
    @GetMapping("/")
    public String home(Authentication auth) {
        boolean isPdt = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_PDT".equals(a.getAuthority()));
        return isPdt ? "redirect:/pdt" : "redirect:/sv";
    }
}
