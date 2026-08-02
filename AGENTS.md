## Code Formatting

- Indentation: 2 spaces.
- Line length: keep close to 120 characters (matches the longest existing lines in the codebase).
- UTF-8 encoding.
- No enforced formatter/linter is wired into the build (no Checkstyle/Spotless plugin in `pom.xml`) — match the
  surrounding file's style by hand.

## Java Style

- Java 17, Spring Boot 3.1.12, Maven.
- Use descriptive names for classes, methods, and variables.
- Never abbreviate a name down to initials (e.g. `final User u = userService.getById(1);` is forbidden). Use the full
  word instead (`User user = ...`), or a qualifying suffix when the plain name is already taken in scope (`userOne`,
  `userTwo`).
- `var` is fine for local variables when the type is already obvious from the right-hand side (e.g.
  `var amount = type.applySign(rawAmount);`) — this codebase uses it, don't avoid it.
- Parameters and local variables are not declared `final` — that's not a convention here; don't add it.
- Prefer early returns and guard clauses; avoid an `else` right after a branch that already returned.
- Avoid comments explaining *what* code does. Comments are fine for: a brief one-line Javadoc on an enum or
  non-obvious class, TODOs, and flagging a genuinely non-obvious constraint or past incident (see the enum-storage
  note below, or `db/changelog/7-fix-enum-storage-to-string.yaml`'s own comment).

## Dependency Injection

- Constructor injection via a plain, hand-written constructor (no Lombok `@RequiredArgsConstructor`). See any
  `*ServiceImplementation` class for the pattern.
- Field injection (`@Autowired` on a field) is not used in production code.

## Lombok

- Used **only** on request/response DTOs, via `@Data` (e.g. `TransactionRequestDto`, `RecurrentTransactionDto`). DTOs
  are plain, short-lived data carriers, so a generated `equals`/`hashCode`/`toString`/getters/setters is safe there.
- **Never** put Lombok (`@Data`, `@Getter`/`@Setter`, `@EqualsAndHashCode`, `@Builder`) on a JPA entity. Entities use
  hand-written getters/setters instead (see `Transaction.java`, `RecurrentTransaction.java`, `BaseEntity.java`) —
  Lombok-generated `equals`/`hashCode`/`toString` on an entity can trigger Hibernate lazy-loading proxy issues and
  infinite recursion across bidirectional relations.
- No `@Slf4j`, `@Builder`, or `@RequiredArgsConstructor` elsewhere in this codebase — plain Java for everything but
  DTOs.

## Mapping (DTO ↔ Entity)

- Use the shared `ModelMapper` bean (configured in `ExpensesApplication.java`) via `mapper.map(source, Target.class)`.
  This project standardizes on ModelMapper — do not introduce MapStruct or hand-written static mappers.
- ModelMapper flattens nested objects by matching names (e.g. source `account.id`/`account.name` → destination
  `accountId`/`accountName`), so most DTO fields need no explicit mapping configuration as long as names line up.
- When a relation (account/category/subcategory) needs to be resolved and validated from an incoming ID, do that
  explicitly in the service via the relevant repository, then set it on the entity *after* `mapper.map(...)` — see
  `getTransactionRelatedEntities`/`getRelatedEntities` in `TransactionServiceImplementation` /
  `RecurrentTransactionServiceImplementation`.
- A custom `ModelMapper` converter (see the `LinkedTransactionDTO` converter in `ExpensesApplication.java`) is the
  right tool when a DTO field needs logic ModelMapper's default flattening can't express.

## Persistence / JPA

- Entities extend `BaseEntity`, which supplies `owner` (current-user FK), audit fields (`createdAt/By`,
  `updatedAt/By`), and soft-delete (`deleted`, `deletedAt/By`).
- Repositories extend `BaseEntityRepository<T>` for the shared active/inactive/soft-delete query family
  (`findActive`, `findActiveById`, `softDelete`, `undoSoftDelete`, ...), all pre-scoped to the current user via Spring
  Security's SpEL (`?#{ principal?.id }`). Add a repository-specific `@Query` only for lookups that family doesn't
  cover.
- **Any enum persisted on an entity must use `@Enumerated(EnumType.STRING)`** with a `VARCHAR` column, from the very
  first migration. This codebase was bitten once by ordinal storage (see
  `db/changelog/7-fix-enum-storage-to-string.yaml`, which had to retrofit this) — never repeat that mistake.
- Small, bounded multi-valued attributes (e.g. days-of-month on a recurrence) are modeled with `@ElementCollection` +
  `@CollectionTable`. Add `@Fetch(FetchMode.SUBSELECT)` (`org.hibernate.annotations`) when the owning entity is ever
  loaded in bulk, to avoid one extra query per row.
- New tables/columns go through a new Liquibase changelog file (`db/changelog/<n>-description.yaml`), registered in
  `db.changelog-master.yaml`. Match the column/constraint/index style already in `0-init-tables.yaml` (author name,
  `createTable`/`addForeignKeyConstraint`/`createIndex` shape, `onDelete`/`onUpdate: RESTRICT` unless the relation is
  a genuine ownership/composition FK, in which case `CASCADE` is appropriate).

## Validation & Error Handling

- Request DTOs use Jakarta Bean Validation (`@NotNull`, `@NotBlank`) with an `UPPER_SNAKE_CASE` message code (e.g.
  `"MISSING_DESCRIPTION"`), never a human sentence — the frontend maps these codes to translated copy.
- Domain-rule violations Bean Validation can't express (cross-field checks, business rules) throw
  `IllegalArgumentException` with the same `UPPER_SNAKE_CASE` code convention.
- Missing entities throw `ResourceNotFoundException(resourceName, fieldName, fieldValue)`.
- Both are already handled by `GlobalExceptionHandler` (`@RestControllerAdvice`) — don't add per-controller
  try/catch or a new `@ExceptionHandler` unless a genuinely new error shape is needed.

## Transactions

- Annotate individual service methods with `@Transactional`, not the class. This keeps read-only lookups out of a
  transaction and matches every existing service.

## Scheduled Jobs

- Enabled via `@EnableScheduling` on `ExpensesApplication`. Only add a second scheduled job if it genuinely needs its
  own cadence — don't split one job into several for organizational reasons alone.
- A scheduled job that iterates many independent records must isolate failures per record (try/catch inside the
  loop, log and continue) so one bad record doesn't abort the whole run — see `RecurrentTransactionGeneratorService`.
- Log with a plain SLF4J `Logger` (`LoggerFactory.getLogger(YourClass.class)`), not Lombok's `@Slf4j` — consistent
  with Lombok being DTO-only in this codebase.
- This app runs as a single instance per environment (see `docker-compose.yml`); a scheduled job does not need a
  distributed lock (e.g. ShedLock) today. Revisit if the app is ever horizontally scaled.

## Testing

- We are actively closing a test-coverage gap — this codebase has almost no tests today. Every new service, utility,
  or piece of real branching logic (date math, validation, sign conventions, anything with more than one code path)
  should ship with a JUnit 5 unit test. `RecurrenceDateCalculatorTest` is the reference example.
- Default to plain, dependency-free unit tests (no Spring context) using AssertJ (`assertThat(...)`) for assertions,
  and Mockito to stub out a repository/collaborator when a class under test genuinely needs one. Reach for
  `@SpringBootTest` or `@WebMvcTest` only when the thing being tested truly requires a Spring context, a real
  database, or HTTP-layer wiring — not as the default.
- Structure each test as Arrange-Act-Assert, and give it a descriptive camelCase name that states the scenario and
  expectation (e.g. `monthlyDay31_clampsToLastDayOfShortMonth_nonLeapYear`) — not `test1`/`shouldWork`.
- Given/when/then as brief comments inside a test body is one of the few places comments are welcome, even though
  they're discouraged elsewhere.
- Controller-slice tests (`@WebMvcTest`) and end-to-end tests don't exist yet in this project. That's a known gap,
  not a blocker for every change — prioritize unit-testing new business logic first (cheapest, highest value),
  then add `@WebMvcTest` coverage for controllers as they're touched, and treat true integration/E2E tests
  (Testcontainers + a real Postgres, or a full request against a running app) as a separate, later effort once the
  unit-test base is in place.

## Coverage

- Coverage is enforced via [Codecov](https://codecov.io), not a hand-maintained `jacoco:check` `<includes>` list in
  `pom.xml`. `codecov.yml` defines two checks: `patch` (80% of lines you added/changed in a PR must be covered) and
  `project` (`target: auto` — total coverage must not regress from the base branch). This ratchets coverage upward
  over time without anyone maintaining a per-class allowlist.
- `pom.xml`'s `jacoco-maven-plugin` only runs `prepare-agent` + `report` (producing
  `target/site/jacoco/jacoco.xml`, bound to the `test` phase) — no local `check` goal. `mvn verify` still runs
  cleanly; it just no longer fails the build on a coverage shortfall itself (Codecov's PR check does that).
- A `pre-push` git hook (`scripts/check-diff-coverage.mjs`, wired via the checked-in `.githooks/pre-push`) mirrors
  Codecov's `patch` check locally: it runs `mvn test`, diffs `src/main/java` against `origin/develop`, and fails
  the push if the *added* lines fall under 80% covered — so a coverage regression is caught before you even open
  a PR, not just in CI. No `package.json`/npm involved — it's a plain script, activated once per clone via
  `git config core.hooksPath .githooks` (see the README's Getting Started section). Node.js is only needed to run
  the script itself.
