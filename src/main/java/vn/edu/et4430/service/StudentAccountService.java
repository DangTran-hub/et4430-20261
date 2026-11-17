package vn.edu.et4430.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.et4430.entity.StudentProfile;
import vn.edu.et4430.entity.UserAccount;
import vn.edu.et4430.repository.StudentProfileRepository;
import vn.edu.et4430.repository.UserAccountRepository;

/** Phòng Đào tạo tạo tài khoản sinh viên và cập nhật mức cảnh báo học tập. */
@Service
public class StudentAccountService {

    /** Dữ liệu từ form tạo sinh viên. */
    public record NewStudent(String username, String password, String fullName, String email,
                             String studentCode, String programCode, Integer cohortYear,
                             String classCode, int warningLevel) {
    }

    private final UserAccountRepository userRepo;
    private final StudentProfileRepository studentRepo;
    private final PasswordEncoder passwordEncoder;

    public StudentAccountService(UserAccountRepository userRepo,
                                 StudentProfileRepository studentRepo,
                                 PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.studentRepo = studentRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<StudentProfile> listStudents() {
        return studentRepo.findAll(Sort.by("studentCode"));
    }

    @Transactional
    public StudentProfile create(NewStudent form) {
        String username = trim(form.username());
        String fullName = trim(form.fullName());
        String studentCode = trim(form.studentCode());

        if (username == null || fullName == null || studentCode == null) {
            throw new BusinessException("Cần nhập tên đăng nhập, họ tên và mã số sinh viên.");
        }
        if (!username.matches("[A-Za-z0-9._-]{3,30}")) {
            throw new BusinessException("Tên đăng nhập chỉ gồm chữ không dấu, số, dấu chấm, gạch dưới, gạch ngang (3–30 ký tự).");
        }
        if (form.password() == null || form.password().length() < 6) {
            throw new BusinessException("Mật khẩu phải có ít nhất 6 ký tự.");
        }
        if (form.warningLevel() < 0 || form.warningLevel() > 3) {
            throw new BusinessException("Mức cảnh báo phải từ 0 đến 3.");
        }
        if (userRepo.findByUsername(username).isPresent()) {
            throw new BusinessException("Tên đăng nhập \"" + username + "\" đã tồn tại.");
        }
        boolean codeUsed = studentRepo.findAll().stream()
                .anyMatch(s -> s.getStudentCode().equalsIgnoreCase(studentCode));
        if (codeUsed) {
            throw new BusinessException("Mã số sinh viên " + studentCode + " đã tồn tại.");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(form.password()));
        user.setFullName(fullName);
        user.setEmail(trim(form.email()));
        user.setRole("STUDENT");
        user.setActive(true);
        userRepo.save(user);

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setStudentCode(studentCode);
        profile.setProgramCode(trim(form.programCode()));
        profile.setCohortYear(form.cohortYear());
        profile.setClassCode(trim(form.classCode()));
        profile.setActive(true);
        profile.setAcademicWarningLevel(form.warningLevel());
        return studentRepo.save(profile);
    }

    @Transactional
    public void updateWarningLevel(Long studentId, int level) {
        if (level < 0 || level > 3) {
            throw new BusinessException("Mức cảnh báo phải từ 0 đến 3.");
        }
        StudentProfile profile = studentRepo.findById(studentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sinh viên."));
        profile.setAcademicWarningLevel(level);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
