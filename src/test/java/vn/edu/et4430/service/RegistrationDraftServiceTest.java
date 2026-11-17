package vn.edu.et4430.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.et4430.dto.*;
import vn.edu.et4430.entity.*;
import vn.edu.et4430.exception.DraftException;
import vn.edu.et4430.repository.*;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static vn.edu.et4430.exception.DraftException.Code.*;

@ExtendWith(MockitoExtension.class)
class RegistrationDraftServiceTest {
    @Mock AcademicTermRepository terms;
    @Mock RegistrationCampaignRepository campaigns;
    @Mock CampaignCourseRepository rows;
    @Mock CourseTermRuleRepository rules;
    @Mock UserAccountRepository users;
    @InjectMocks RegistrationDraftService service;
    AcademicTerm target;
    Course a;
    Course b;
    UserAccount pdt;

    @BeforeEach void setup() {
        target = term(10L, "HK1", "2026-09-01");
        a = course(1L);
        b = course(2L);
        pdt = new UserAccount();
        pdt.setRole("PDT");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("pdt", "unused", List.of()));
        lenient().when(users.findByUsername("pdt")).thenReturn(Optional.of(pdt));
        lenient().when(terms.findById(10L)).thenReturn(Optional.of(target));
        lenient().when(terms.findLockedById(10L)).thenReturn(Optional.of(target));
        lenient().when(rules.findByTermCodeAndActiveTrue("HK1")).thenReturn(List.of(rule(a), rule(b)));
        lenient().when(campaigns.saveAndFlush(any())).thenAnswer(invocation -> {
            RegistrationCampaign c = invocation.getArgument(0);
            if (c.getId() == null) {
                ReflectionTestUtils.setField(c, "id", 100L);
                ReflectionTestUtils.invokeMethod(c, "initializeAudit");
            } else ReflectionTestUtils.invokeMethod(c, "updateAudit");
            return c;
        });
    }

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test void preparationUsesLatestPublishedEarlierCampaignAndCurrentRules() {
        RegistrationCampaign latest = campaign(20L, term(2L, "HK1", "2025-09-01"));
        RegistrationCampaign older = campaign(19L, term(1L, "HK1", "2024-09-01"));
        when(campaigns.findByStatusAndTerm_TermCodeAndTerm_TermStartDateBeforeOrderByTerm_TermStartDateDesc(
                "PUBLISHED", "HK1", target.getTermStartDate())).thenReturn(List.of(latest, older));
        Course inactive = course(3L);
        inactive.setActive(false);
        CourseTermRule disabled = rule(b);
        disabled.setActive(false);
        CourseTermRule otherSemester = rule(b);
        otherSemester.setTermCode("HK2");
        CourseTermRule programRule = rule(a);
        programRule.setProgramCode("IT");
        when(rules.findByTermCodeAndActiveTrue("HK1"))
                .thenReturn(List.of(programRule, rule(a), rule(inactive), disabled, otherSemester));
        when(rows.findByCampaign_Id(20L)).thenReturn(List.of(row(a), row(b), row(inactive)));
        DraftPreparation result = service.prepare(10L);
        assertThat(result.sourceCampaignId()).isEqualTo(20L);
        assertThat(result.semesterType()).isEqualTo(DraftPreparation.SemesterType.ODD);
        assertThat(result.selectedCourseIds()).containsExactly(1L);
        assertThat(result.eligibleCourses()).extracting(DraftPreparation.CourseOption::id).containsExactly(1L);
        verify(rows, never()).findByCampaign_Id(19L);
        verify(campaigns, never()).saveAndFlush(any());
    }

    @Test void noHistoryExplainsManualPreparationAndAllowsSave() {
        var preview = service.prepare(10L);
        assertThat(preview.sourceCampaignId()).isNull();
        assertThat(preview.messages()).anyMatch(m -> m.contains("thủ công"));
        assertThat(preview.selectedCourseIds()).isEmpty();
        DraftView saved = service.saveDraft(10L, selection(1L));
        assertThat(saved.sourceCampaignId()).isNull();
        assertThat(saved.courseIds()).containsExactly(1L);
        assertThat(saved.createdAt()).isEqualTo(saved.updatedAt()).isNotNull();
        verify(terms).findLockedById(10L);
    }

    @Test void saveCreatesNewCourseRowsWithProvenanceAndNoHistoricalDatesOrApproval() {
        var source = campaign(20L, term(2L, "HK1", "2025-09-01"));
        source.setRegistrationOpenAt(OffsetDateTime.now().minusYears(1));
        source.setApprovedBy(pdt);
        CampaignCourse sourceRow = row(a);
        when(campaigns.findByStatusAndTerm_TermCodeAndTerm_TermStartDateBeforeOrderByTerm_TermStartDateDesc(
                "PUBLISHED", "HK1", target.getTermStartDate())).thenReturn(List.of(source));
        when(rows.findByCampaign_Id(20L)).thenReturn(List.of(sourceRow));
        var result = service.saveDraft(10L, selection(1L, 2L));
        assertThat(result.registrationOpenAt()).isNull();
        assertThat(result.status()).isEqualTo("DRAFT");
        ArgumentCaptor<List<CampaignCourse>> captor = ArgumentCaptor.forClass(List.class);
        verify(rows).saveAllAndFlush(captor.capture());
        var added = captor.getValue();
        assertThat(added.getFirst()).isNotSameAs(sourceRow);
        assertThat(added.getFirst().getCopiedFromCampaignCourse()).isSameAs(sourceRow);
        assertThat(added.get(1).getCopiedFromCampaignCourse()).isNull();
        assertThat(added.getFirst().getCampaign().getApprovedBy()).isNull();
        assertThat(added).allMatch(r -> "DRAFT".equals(r.getOfferingStatus()));
    }

    @Test void noCoursesReturnsActionablePreview() {
        when(rules.findByTermCodeAndActiveTrue("HK1")).thenReturn(List.of());
        var result = service.prepare(10L);
        assertThat(result.eligibleCourses()).isEmpty();
        assertThat(result.messages()).anyMatch(m -> m.contains("cấu hình"));
        verifyNoInteractions(rows);
    }

    @Test void duplicateIsRejectedBeforeWriting() {
        when(campaigns.existsByTerm_Id(10L)).thenReturn(true);
        assertCode(DUPLICATE_PLAN, () -> service.prepare(10L));
        assertCode(DUPLICATE_PLAN, () -> service.saveDraft(10L, selection(1L)));
        verify(campaigns, never()).saveAndFlush(any());
    }

    @Test void equalOrEarlierClosingInstantIsRejected() {
        var open = OffsetDateTime.parse("2026-11-17T10:00:00+07:00");
        assertCode(INVALID_DATES, () -> service.saveDraft(10L,
                new DraftSelection(List.of(1L), open, open.withOffsetSameInstant(ZoneOffset.UTC))));
        assertCode(INVALID_DATES, () -> service.saveDraft(10L,
                new DraftSelection(List.of(1L), open, open.minusMinutes(1))));
        verify(campaigns, never()).saveAndFlush(any());
    }

    @Test void validDatesAndPartialDatesAreAllowedInDraft() {
        var open = OffsetDateTime.parse("2026-11-17T10:00:00+07:00");
        assertThat(service.saveDraft(10L, new DraftSelection(List.of(1L), open, open.plusHours(1)))
                .registrationCloseAt()).isEqualTo(open.plusHours(1));
        assertThat(service.saveDraft(10L, new DraftSelection(List.of(), open, null))
                .registrationCloseAt()).isNull();
    }

    @Test void duplicateAndIneligibleCourseSelectionsAreRejected() {
        assertCode(INVALID_COURSES, () -> service.saveDraft(10L, selection(1L, 1L)));
        assertCode(INVALID_COURSES, () -> service.saveDraft(10L, selection(99L)));
        assertCode(INVALID_COURSES, () -> service.saveDraft(10L, null));
        verify(campaigns, never()).saveAndFlush(any());
    }

    @Test void editingRetainsRowsAddsEligibleCoursesAndRemovesDeselectedRows() {
        var draft = editable();
        var removed = row(a);
        when(rows.findByCampaign_Id(100L)).thenReturn(List.of(removed));
        var created = draft.getCreatedAt();
        var result = service.editDraft(100L, selection(2L));
        assertThat(result.courseIds()).containsExactly(2L);
        assertThat(result.createdAt()).isEqualTo(created);
        assertThat(result.updatedAt()).isAfter(created);
        assertThat(draft.getCreatedBy()).isSameAs(pdt);
        verify(rows).deleteAll(List.of(removed));
        verify(campaigns).findLockedById(100L);
    }

    @Test void courseOnlyEditingKeepsRetainedRowIdentityAndAllowsEmptySelection() {
        editable();
        var retained = row(a);
        when(rows.findByCampaign_Id(100L)).thenReturn(List.of(retained));
        service.editDraft(100L, selection(1L));
        verify(rows).saveAllAndFlush(List.of());
        service.editDraft(100L, selection());
        verify(rows).deleteAll(List.of(retained));
    }

    @Test void publishedCampaignCannotBeEdited() {
        editable().setStatus("PUBLISHED");
        assertCode(NOT_EDITABLE, () -> service.editDraft(100L, selection(1L)));
        verifyNoInteractions(rows);
    }

    @Test void editingRevalidatesCoursesAndDatesBeforeMutation() {
        var draft = editable();
        var before = draft.getUpdatedAt();
        var open = OffsetDateTime.now();
        assertCode(INVALID_COURSES, () -> service.editDraft(100L, selection(99L)));
        assertCode(INVALID_DATES, () -> service.editDraft(100L,
                new DraftSelection(List.of(1L), open, open)));
        assertThat(draft.getUpdatedAt()).isEqualTo(before);
        verifyNoInteractions(rows);
    }

    @Test void uniquenessConflictBecomesBusinessError() {
        doThrow(new DataIntegrityViolationException("unique")).when(campaigns).saveAndFlush(any());
        assertCode(STORAGE_CONFLICT, () -> service.saveDraft(10L, selection(1L)));
    }

    @Test void courseWriteConflictBecomesBusinessError() {
        when(rows.saveAllAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));
        assertCode(STORAGE_CONFLICT, () -> service.saveDraft(10L, selection(1L)));
    }

    @Test void unknownTermAndUnknownSemesterAreRejected() {
        assertCode(NOT_FOUND, () -> service.prepare(999L));
        target.setTermCode("UNKNOWN");
        assertCode(INVALID_TERM, () -> service.prepare(10L));
    }

    @Test void nullCourseIdsReturnBusinessValidationError() {
        assertCode(INVALID_COURSES, () -> service.saveDraft(10L,
                new DraftSelection(Arrays.asList(1L, null), null, null)));
        assertCode(INVALID_COURSES, () -> service.saveDraft(10L,
                new DraftSelection(null, null, null)));
    }

    @Test void equallyRecentHistoricalPlansRequireDataCorrection() {
        when(campaigns.findByStatusAndTerm_TermCodeAndTerm_TermStartDateBeforeOrderByTerm_TermStartDateDesc(
                "PUBLISHED", "HK1", target.getTermStartDate())).thenReturn(List.of(
                campaign(20L, term(2L, "HK1", "2025-09-01")),
                campaign(21L, term(3L, "HK1", "2025-09-01"))));
        assertCode(AMBIGUOUS_SOURCE, () -> service.prepare(10L));
        verify(campaigns, never()).saveAndFlush(any());
    }

    @Test void mapsEvenAndSummerExplicitly() {
        for (String code : List.of("HK2", "HK3")) {
            target.setTermCode(code);
            assertThat(service.prepare(10L).semesterType()).isEqualTo(code.equals("HK2")
                    ? DraftPreparation.SemesterType.EVEN : DraftPreparation.SemesterType.SUMMER);
        }
    }

    @Test void anonymousStudentAndInactivePdtAreDenied() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.prepare(10L)).isInstanceOf(AccessDeniedException.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("pdt", "unused", List.of()));
        pdt.setRole("STUDENT");
        assertThatThrownBy(() -> service.saveDraft(10L, selection(1L))).isInstanceOf(AccessDeniedException.class);
        pdt.setRole("PDT");
        pdt.setActive(false);
        assertThatThrownBy(() -> service.editDraft(100L, selection(1L))).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(terms, campaigns, rows, rules);
    }

    private RegistrationCampaign editable() {
        var draft = campaign(100L, target);
        draft.setStatus("DRAFT");
        draft.setCreatedBy(pdt);
        ReflectionTestUtils.setField(draft, "createdAt", OffsetDateTime.now().minusDays(1));
        draft.setUpdatedAt(draft.getCreatedAt());
        when(campaigns.findLockedById(100L)).thenReturn(Optional.of(draft));
        return draft;
    }

    private void assertCode(DraftException.Code code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DraftException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(code));
    }
    private DraftSelection selection(Long... ids) { return new DraftSelection(List.of(ids), null, null); }
    private AcademicTerm term(Long id, String code, String start) {
        var t = new AcademicTerm();
        ReflectionTestUtils.setField(t, "id", id);
        t.setTermCode(code);
        t.setAcademicYear("2026-2027");
        t.setTermStartDate(LocalDate.parse(start));
        return t;
    }
    private Course course(Long id) {
        var c = new Course();
        ReflectionTestUtils.setField(c, "id", id);
        c.setCourseCode("C" + id);
        return c;
    }
    private CourseTermRule rule(Course course) {
        var r = new CourseTermRule(); r.setCourse(course); r.setTermCode("HK1"); return r;
    }
    private CampaignCourse row(Course course) {
        var r = new CampaignCourse(); r.setCourse(course); return r;
    }
    private RegistrationCampaign campaign(Long id, AcademicTerm term) {
        var c = new RegistrationCampaign();
        ReflectionTestUtils.setField(c, "id", id);
        c.setTerm(term); c.setStatus("PUBLISHED"); return c;
    }
}
