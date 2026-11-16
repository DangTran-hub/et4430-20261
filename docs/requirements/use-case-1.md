# Use Case 1: Publish the course-registration plan

## Source and technology decision
Report.pdf, Table 3.2, printed pp. 13–15 (PDF pp. 17–19).
Supporting dictionary: printed pp. 10–12.

The current project and user direction establish Spring Boot,
Maven, Thymeleaf, and PostgreSQL as the intended stack.
The report's Swing/DesktopApp proposal does not govern this feature.

## Actors
- PDT: authenticated account authorized to manage registration plans.
- Student: authenticated account viewing published information.

PDT reviews its prepared plan; a separate approver is not specified.

## Preconditions
- PDT is authenticated and authorized.
- Academic year and semester are configured.
- Courses and their odd/even semester-opening rules are configured.

## Main flow
1. PDT selects an academic year and semester.
2. The system determines the semester type.
3. The system selects suitable courses and prepares a new plan using
   information inherited from a previous plan of the same semester type.
4. PDT reviews and edits the plan, including registration dates.
5. PDT confirms; the system saves the plan, updates its status,
   and publishes an in-app notification.
6. A student signs in and views the notification and plan.
7. The system displays academic year, semester, registration opening
   and closing times, and that student's academic-warning status.
8. The student can consult the published plan and its course list.

## Business rules and alternatives
- Prevent duplicate plans for the selected academic term.
- Registration closing time must be later than opening time.
- No matching previous plan: explain that inheritance is unavailable
  and allow PDT to enter the new plan manually.
- No suitable courses: explain the problem and direct PDT to check
  course and semester-opening configuration.
- PDT can correct inaccurate plan information; whether this includes
  already-published plans is unresolved.
- Published information must be available for student consultation.
- Academic-warning information belongs to the signed-in student.

## Data needed
- Account identity, credentials, role, and active status.
- Student identity and academic-warning status.
- Academic year, semester, semester type, and term dates.
- Course identity, code, name, credits, and opening rules.
- Plan term, dates, status, selected courses, and inheritance source.
- Creator/reviewer identity and relevant timestamps.
- Notification title, body, publication time, and associated plan.

## Acceptance criteria derived from the report
1. An authorized PDT account can select an existing academic term.
2. Preparation selects courses matching the configured semester type
   and inherits information from a matching previous plan.
3. Without a previous matching plan, PDT sees an explanation and can
   prepare the plan manually.
4. Without matching courses, PDT sees an actionable explanation.
5. A second plan for the same term is rejected.
6. PDT can review and correct prepared information before confirming.
7. Closing time equal to or earlier than opening time is rejected.
8. Successful confirmation persists the plan and selected courses,
   updates its status, and publishes its in-app notification.
9. An authenticated student can view the published notification,
   academic year, semester, registration dates, and course list.
10. Each student sees only their own academic-warning status.

## Proposed safeguards requiring agreement
- Students cannot access draft plans or PDT management operations.
- Publication and notification creation succeed or fail together.
- Repeated confirmation does not create duplicate notifications.

These safeguards are proposed design criteria, not explicit report text.

## Open decisions
- How do semester codes map to odd/even types, especially summer?
- Which matching historical plan is selected, and which fields are copied?
- How are historical courses reconciled with current opening rules?
- Can PDT add courses outside the generated selection?
- Must a plan contain courses before publication?
- Is approval separate from publication? Are different staff required?
- Can published plans be edited, withdrawn, or republished?
- Does one-plan-per-term cover cancelled plans and all registration rounds?
- Does “open the registration campaign” mean publication immediately
  enables registration, or does the configured opening time control it?
- Which timezone and date-boundary rules apply?
- Which students receive each notification? Is audience filtering needed?
- Are inbox entries, read/unread tracking, or real-time delivery required?
- Where does academic-warning status come from, which term does it
  describe, and what should appear when it is unavailable?
- Does the broader report's class/timetable preparation belong here,
  or does UC1 publish only a course list?

## Outside this use case
- Student course selection and enrollment submission.
- Prerequisite, timetable-conflict, credit-limit, and seat checks.
- Class-capacity processing, merging, cancellation, and result notices.
- Registration adjustments and their approval.
- Advisor workflows, supplemental paper registration, and withdrawal.
- Fees, grades, teaching activities, printed forms, archival systems.
- Email/SMS/push delivery, external synchronization, and import/export.
- Catalog, account, and term management screens beyond prerequisites.

Academic-warning display is included; calculating warnings is not.
