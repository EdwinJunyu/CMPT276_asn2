# Testing

## What Was Tested

All tests are in `src/test/java/ca/sfu/cmpt276/asn2chen/Asn2ChenApplicationTests.java`.
They consistently use `@SpringBootTest` with `@AutoConfigureMockMvc` to load the full
application context. This approach tests how form binding, validation, controllers,
repositories, and real Thymeleaf templates work together, without mocking the repository.

The tests cover:

- **Validation:** Invalid emails, scores outside 1-10, missing required fields, blank
  names and emails, non-integer scores, and invalid roles are rejected. Scores of 1 and
  10 and an omitted optional comment are accepted. Invalid submissions return the form
  with field errors and do not create database records.
- **Web and controller behavior:** `/` redirects to `/ratings`. The index returns HTTP
  200 with the expected `ratings` model attribute, including empty and populated lists
  ordered newest first. The create form includes the expected model attributes.
  Successful creation normalizes input, saves the rating, and redirects to its detail
  page. Duplicate emails are rejected. Tests also cover detail page rendering, edit
  forms, successful edits, failed edits leaving stored data unchanged, keeping the
  current email, deletion redirects, and HTTP 404 responses for unknown records.
- **Persistence:** Saving and retrieving a rating preserves its fields and timestamps,
  and the overall score is calculated correctly. Deleting a rating removes it from the
  database. Tests flush changes and clear the JPA persistence context before reading
  results to verify database state rather than cached entities.

Tests use an isolated H2 in-memory database in PostgreSQL compatibility mode, with
automatic schema creation and removal. Each test runs in a transaction that is rolled
back afterward. Browser JavaScript, visual layout, real PostgreSQL, and deployment are
outside the scope of these tests.

## How to Run Tests

Install JDK 21 and set `JAVA_HOME` to its installation directory. Run commands from the
project root. Maven Wrapper is included, so a separate Maven installation is not
required. The first run needs internet access to download Maven and dependencies.
No PostgreSQL server or database environment variables are needed for these tests.

**Windows PowerShell:**

```powershell
.\mvnw.cmd test
```

**macOS / Linux:**

```sh
sh ./mvnw test
```

To run only the invalid-email test on Windows:

```powershell
.\mvnw.cmd '-Dtest=Asn2ChenApplicationTests#createRejectsInvalidEmail' test
```

In IntelliJ IDEA, you can also run **Maven > asn2_chen > Lifecycle > test**.
Maven prints the test results in the console and writes reports to
`target/surefire-reports/`. Parameterized tests run once for each supplied input.
