# PostgreSQL foundation

Flyway owns schema changes. V1 creates the eight UC1 ERD tables; it contains no
seed records, drops, deletes, or updates. Hibernate uses `validate`, and SQL
initialization is disabled. No entities exist yet, so Hibernate does not currently
validate these tables against mappings.

Use an existing PostgreSQL database and a role permitted to create tables and
sequences in its target schema. Supply these process environment variables:

- `DB_URL`: JDBC URL, for example `jdbc:postgresql://localhost:5432/quanlyhocphan`
- `DB_USERNAME`: database role
- `DB_PASSWORD`: database password

Credentials have no defaults. Inject them through your local environment or IDE;
do not commit credentials. From PowerShell in the repository root, with those
variables supplied:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd spring-boot:run
```

The context test and application startup both run Flyway against the configured
database. Use a separate empty test database for tests. Compilation/package without
executing tests: `.\mvnw.cmd -DskipTests package`.

Flyway records successful migrations in `flyway_schema_history`. Subsequent starts
validate and skip already-applied versions. Clean is disabled and automatic
baselining is disabled: an existing nonempty schema without Flyway history must
be reviewed before adoption. Do not reset it or enable automatic baselining to
bypass that check. Existing conflicting tables cause failure, not replacement.

## Physical design choices and outstanding decisions

Identifiers use BIGINT identity columns; student identity shares the account key.
Text columns avoid inventing business length limits; credits use NUMERIC(6,2).
Term boundaries use DATE and event timestamps use TIMESTAMP WITH TIME ZONE to
store instants; the business timezone and date-boundary rules remain unresolved.
Registration dates are nullable for preparation; the ordering check applies when
both are present. Publication completeness remains future application validation.
`updated_at` has an insert default only; future writes must explicitly maintain it.
Foreign keys do not cascade deletes. No role or lifecycle enum is invented.

Academic-term and campaign/course uniqueness and date ordering are included.
The ERD explicitly conditions campaign term uniqueness on a decision about
cancelled plans and registration rounds; that constraint is deferred. Thus V1
alone does not yet enforce UC1 duplicate-plan prevention. Resolve this before
implementing plan creation, then add a new migration.

Approval columns are retained as nullable storage, without imposing a separate
approval workflow. Semester parity, academic-warning storage, notification
recipients/read state, and notification uniqueness remain unresolved and are not
invented here. PDT authorization, student-specific access and inheritance rules
require later application work. This migration is only the database foundation.

## V2 decisions (UC1 implementation + student registration)

`V2__warning_level_and_course_registration.sql` resolves the decisions needed to
implement UC1 and student course registration:

- **Semester type** = `academic_term.term_code`: `SEMESTER_1` (odd), `SEMESTER_2`
  (even), `SUMMER`. `course_term_rule.term_code` uses the same values.
- **Inheritance source**: the most recent `PUBLISHED` campaign whose term has the
  same `term_code` and an earlier `academic_year`. Its `OPEN` courses are copied
  (with `copied_from_campaign_course_id`) and its registration dates are shifted by
  the year difference. Courses with an active rule that the source lacked are added.
- **Lifecycle**: `DRAFT -> PUBLISHED`; PDT reviews and publishes (no separate
  approver). A draft can be `CANCELLED`. Only drafts are editable.
- **One plan per term**: partial unique index on `registration_campaign(term_id)`
  where `status <> 'CANCELLED'`.
- **One notification per campaign**: unique `notification(campaign_id)`; it is
  created in the same transaction as publication. All students see it.
- **Academic warning**: `student_profile.academic_warning_level` 0..3. Credit
  limits live in `WarningPolicy`: 0 -> 12..24, 1 -> max 14, 2 -> max 10,
  3 -> cannot register.
- **Registration window**: a student can register/unregister only while the
  campaign is `PUBLISHED` and now is between open and close times.
- **Timezone**: input and display use Asia/Ho_Chi_Minh; storage is timestamptz.

Demo accounts and terms are created by `DemoDataInitializer` (not by Flyway) when
`app.demo-data=true` and no `pdt` account exists.
