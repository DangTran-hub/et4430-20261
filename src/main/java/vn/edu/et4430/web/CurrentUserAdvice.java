package vn.edu.et4430.web;

import java.security.Principal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.et4430.entity.UserAccount;
import vn.edu.et4430.repository.UserAccountRepository;

/** Đưa tên người đang đăng nhập vào mọi trang (thanh tiêu đề). */
@ControllerAdvice
public class CurrentUserAdvice {

    private final UserAccountRepository userRepo;

    public CurrentUserAdvice(UserAccountRepository userRepo) {
        this.userRepo = userRepo;
    }

    @ModelAttribute("currentUserName")
    public String currentUserName(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userRepo.findByUsername(principal.getName())
                .map(UserAccount::getFullName)
                .orElse(principal.getName());
    }
}
