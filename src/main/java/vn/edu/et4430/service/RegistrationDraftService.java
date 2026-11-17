package vn.edu.et4430.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.et4430.dto.*;
import vn.edu.et4430.entity.*;
import vn.edu.et4430.exception.DraftException;
import vn.edu.et4430.repository.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static vn.edu.et4430.exception.DraftException.Code.*;

@Service
public class RegistrationDraftService {
    private final AcademicTermRepository terms;
    private final RegistrationCampaignRepository campaigns;
    private final CampaignCourseRepository campaignCourses;
    private final CourseTermRuleRepository rules;
    private final UserAccountRepository users;

    public RegistrationDraftService(AcademicTermRepository terms, RegistrationCampaignRepository campaigns,
                                    CampaignCourseRepository campaignCourses, CourseTermRuleRepository rules,
                                    UserAccountRepository users) {
        this.terms = terms;
        this.campaigns = campaigns;
        this.campaignCourses = campaignCourses;
        this.rules = rules;
        this.users = users;
    }

    /** Preview only: no campaign or course-list rows are written. */
    @Transactional(readOnly = true)
    public DraftPreparation prepare(Long termId) {
        requirePdt();
        AcademicTerm term = terms.findById(termId)
                .orElseThrow(() -> new DraftException(NOT_FOUND, "Không tìm thấy học kỳ đã chọn."));
        rejectDuplicate(termId);
        var type = semesterType(term.getTermCode());
        Map<Long, Course> eligible = eligibleCourses(term);
        RegistrationCampaign source = source(term);
        Map<Long, CampaignCourse> inherited = inheritedCourses(source);
        List<String> messages = new ArrayList<>();
        if (source == null) {
            messages.add("Không có kế hoạch đã công bố cùng loại học kỳ trước đó. Hãy chọn học phần để lập bản nháp thủ công.");
        }
        if (eligible.isEmpty()) {
            messages.add("Không có học phần phù hợp. Hãy kiểm tra trạng thái học phần và cấu hình mở học phần cho học kỳ này.");
        }
        List<Long> selected = inherited.keySet().stream().filter(eligible::containsKey).sorted().toList();
        if (source != null && selected.isEmpty() && !eligible.isEmpty()) {
            messages.add("Không có học phần kế thừa phù hợp. Hãy chọn từ danh sách học phần được mở trong học kỳ này.");
        }
        return new DraftPreparation(term.getId(), term.getAcademicYear(), term.getTermCode(), type,
                source == null ? null : source.getId(), eligible.values().stream()
                .map(c -> new DraftPreparation.CourseOption(c.getId(), c.getCourseCode(), c.getCourseName(), c.getCredits()))
                .toList(), selected, messages);
    }

    /** Saves the explicit PDT selection, including manual preparation when no source exists. */
    @Transactional
    public DraftView saveDraft(Long termId, DraftSelection selection) {
        UserAccount creator = requirePdt();
        AcademicTerm term = terms.findLockedById(termId)
                .orElseThrow(() -> new DraftException(NOT_FOUND, "Không tìm thấy học kỳ đã chọn."));
        rejectDuplicate(termId);
        semesterType(term.getTermCode());
        Map<Long, Course> eligible = eligibleCourses(term);
        validateSelection(selection, eligible);
        RegistrationCampaign source = source(term);
        RegistrationCampaign draft = new RegistrationCampaign();
        draft.setTerm(term);
        draft.setStatus("DRAFT");
        draft.setCreatedBy(creator);
        draft.setCopiedFromCampaign(source);
        applyDates(draft, selection);
        try {
            campaigns.saveAndFlush(draft);
            List<CampaignCourse> rows = addCourses(draft, selection.courseIds(), eligible, inheritedCourses(source));
            campaignCourses.saveAllAndFlush(rows);
            return view(draft, selection.courseIds());
        } catch (DataIntegrityViolationException ex) {
            throw storageConflict(ex);
        }
    }

    /** Replaces dates and selection only; term, creator and inheritance source remain fixed. */
    @Transactional
    public DraftView editDraft(Long campaignId, DraftSelection selection) {
        requirePdt();
        RegistrationCampaign draft = campaigns.findLockedById(campaignId)
                .orElseThrow(() -> new DraftException(NOT_FOUND, "Không tìm thấy bản nháp."));
        if (!"DRAFT".equals(draft.getStatus())) {
            throw new DraftException(NOT_EDITABLE, "Chỉ được chỉnh sửa kế hoạch ở trạng thái bản nháp.");
        }
        semesterType(draft.getTerm().getTermCode());
        Map<Long, Course> eligible = eligibleCourses(draft.getTerm());
        validateSelection(selection, eligible);
        Map<Long, CampaignCourse> existing = new LinkedHashMap<>();
        campaignCourses.findByCampaign_Id(campaignId).forEach(row -> existing.put(row.getCourse().getId(), row));
        Set<Long> selected = new HashSet<>(selection.courseIds());
        try {
            // Keep retained rows and their identity/provenance; remove only deselected rows.
            campaignCourses.deleteAll(existing.entrySet().stream().filter(e -> !selected.contains(e.getKey()))
                    .map(Map.Entry::getValue).toList());
            List<Long> added = selection.courseIds().stream().filter(id -> !existing.containsKey(id)).toList();
            campaignCourses.saveAllAndFlush(addCourses(draft, added, eligible,
                    inheritedCourses(draft.getCopiedFromCampaign())));
            applyDates(draft, selection);
            // A course-only edit must update the campaign timestamp too.
            draft.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            campaigns.saveAndFlush(draft);
            return view(draft, selection.courseIds());
        } catch (DataIntegrityViolationException ex) {
            throw storageConflict(ex);
        }
    }

    private UserAccount requirePdt() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Vui lòng đăng nhập bằng tài khoản PDT.");
        }
        return users.findByUsername(auth.getName()).filter(UserAccount::isActive)
                .filter(u -> "PDT".equals(u.getRole()))
                .orElseThrow(() -> new AccessDeniedException("Chỉ tài khoản PDT đang hoạt động được quản lý bản nháp."));
    }

    private DraftPreparation.SemesterType semesterType(String code) {
        if (code == null) throw new DraftException(INVALID_TERM, "Học kỳ chưa có mã hợp lệ.");
        return switch (code) {
            case "HK1" -> DraftPreparation.SemesterType.ODD;
            case "HK2" -> DraftPreparation.SemesterType.EVEN;
            case "HK3" -> DraftPreparation.SemesterType.SUMMER;
            default -> throw new DraftException(INVALID_TERM, "Mã học kỳ phải là HK1, HK2 hoặc HK3.");
        };
    }

    private Map<Long, Course> eligibleCourses(AcademicTerm term) {
        Map<Long, Course> result = new TreeMap<>();
        for (CourseTermRule rule : rules.findByTermCodeAndActiveTrue(term.getTermCode())) {
            if (rule.isActive() && term.getTermCode().equals(rule.getTermCode()) && rule.getCourse().isActive()) {
                result.put(rule.getCourse().getId(), rule.getCourse());
            }
        }
        return result;
    }

    private RegistrationCampaign source(AcademicTerm term) {
        var candidates = campaigns.findByStatusAndTerm_TermCodeAndTerm_TermStartDateBeforeOrderByTerm_TermStartDateDesc(
                "PUBLISHED", term.getTermCode(), term.getTermStartDate());
        if (candidates.isEmpty()) return null;
        if (candidates.size() > 1 && candidates.get(0).getTerm().getTermStartDate()
                .equals(candidates.get(1).getTerm().getTermStartDate())) {
            throw new DraftException(AMBIGUOUS_SOURCE, "Có nhiều kế hoạch nguồn gần nhất. Hãy kiểm tra dữ liệu kế hoạch cũ.");
        }
        return candidates.getFirst();
    }

    private Map<Long, CampaignCourse> inheritedCourses(RegistrationCampaign source) {
        Map<Long, CampaignCourse> result = new LinkedHashMap<>();
        if (source != null) campaignCourses.findByCampaign_Id(source.getId())
                .forEach(row -> result.put(row.getCourse().getId(), row));
        return result;
    }

    private List<CampaignCourse> addCourses(RegistrationCampaign draft, List<Long> ids,
                                           Map<Long, Course> eligible, Map<Long, CampaignCourse> inherited) {
        return ids.stream().map(id -> {
            CampaignCourse row = new CampaignCourse();
            row.setCampaign(draft);
            row.setCourse(eligible.get(id));
            row.setCopiedFromCampaignCourse(inherited.get(id));
            // Storage label for an unpublished selection; no historical lifecycle state is copied.
            row.setOfferingStatus("DRAFT");
            return row;
        }).toList();
    }

    private void validateSelection(DraftSelection selection, Map<Long, Course> eligible) {
        if (selection == null || selection.courseIds() == null
                || selection.courseIds().contains(null)
                || new HashSet<>(selection.courseIds()).size() != selection.courseIds().size()
                || !eligible.keySet().containsAll(selection.courseIds())) {
            throw new DraftException(INVALID_COURSES, "Danh sách học phần không hợp lệ, bị trùng hoặc có học phần không được mở trong học kỳ này.");
        }
        if (selection.registrationOpenAt() != null && selection.registrationCloseAt() != null
                && !selection.registrationCloseAt().isAfter(selection.registrationOpenAt())) {
            throw new DraftException(INVALID_DATES, "Thời điểm đóng đăng ký phải sau thời điểm mở đăng ký.");
        }
    }

    private void rejectDuplicate(Long termId) {
        if (campaigns.existsByTerm_Id(termId)) {
            throw new DraftException(DUPLICATE_PLAN, "Học kỳ đã có kế hoạch đăng ký. Hãy chỉnh sửa bản nháp hiện có.");
        }
    }

    private void applyDates(RegistrationCampaign draft, DraftSelection selection) {
        draft.setRegistrationOpenAt(selection.registrationOpenAt());
        draft.setRegistrationCloseAt(selection.registrationCloseAt());
    }

    private DraftException storageConflict(DataIntegrityViolationException ex) {
        return new DraftException(STORAGE_CONFLICT,
                "Không thể lưu do dữ liệu trùng hoặc xung đột ràng buộc. Hãy tải lại kế hoạch và kiểm tra danh sách học phần.", ex);
    }

    private DraftView view(RegistrationCampaign draft, List<Long> ids) {
        return new DraftView(draft.getId(), draft.getTerm().getId(),
                draft.getCopiedFromCampaign() == null ? null : draft.getCopiedFromCampaign().getId(),
                draft.getStatus(), ids, draft.getRegistrationOpenAt(), draft.getRegistrationCloseAt(),
                draft.getCreatedAt(), draft.getUpdatedAt());
    }
}
