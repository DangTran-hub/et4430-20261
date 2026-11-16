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
