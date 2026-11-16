package vn.edu.et4430.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import vn.edu.et4430.entity.AcademicTerm;
import vn.edu.et4430.entity.RegistrationCampaign;
import vn.edu.et4430.service.AppTime;
import vn.edu.et4430.service.CampaignService;
import vn.edu.et4430.service.StudentRegistrationService;
import vn.edu.et4430.service.TermTypes;
import vn.edu.et4430.service.WarningPolicy;

/** Dùng trong template Thymeleaf: ${@fmt.dt(x)}, ${@fmt.term(t)}, ... */
@Component("fmt")
public class ViewHelper {

    public String dt(OffsetDateTime time) {
        return AppTime.display(time);
    }

    public String input(OffsetDateTime time) {
        return AppTime.input(time);
    }

    public String term(AcademicTerm term) {
        return term == null ? "" : TermTypes.fullName(term);
    }

    public String termType(AcademicTerm term) {
        return term == null ? "" : TermTypes.typeName(term.getTermCode());
    }

    public String credits(BigDecimal value) {
        return StudentRegistrationService.plain(value);
    }

    public String window(RegistrationCampaign campaign) {
        return CampaignService.windowStatus(campaign);
    }

    /** Lớp CSS cho nhãn trạng thái. */
    public String windowCss(RegistrationCampaign campaign) {
        return switch (CampaignService.windowStatus(campaign)) {
            case "Đang mở" -> "badge ok";
            case "Sắp mở", "Bản nháp" -> "badge warn";
            default -> "badge muted";
        };
    }

    public String warning(int level) {
        return WarningPolicy.label(level);
    }

    public String warningDesc(int level) {
        return WarningPolicy.description(level);
    }

    public String warningCss(int level) {
        return switch (level) {
            case 0 -> "badge ok";
            case 1 -> "badge warn";
            case 2 -> "badge orange";
            default -> "badge danger";
        };
    }

    public int maxCredits(int level) {
        return WarningPolicy.maxCredits(level);
    }

    public int minCredits(int level) {
        return WarningPolicy.minCredits(level);
    }
}
