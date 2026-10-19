# CITS3501 / CITS5501 — Software Testing and Quality Assurance

## Phase 2: Policy Document Scraper and Testing

This repository contains the group's Phase 2 work on a minimal Java policy-document scraper and the design of tests for unusual website and network conditions.

## Repository contents

| File | Purpose |
| --- | --- |
| `Scraper.java` | Java scraper prototype (Task 1). |
| `FixtureServer.java` | Local HTTP server for the invalid-PDF fixture (Task 3). |
| `Task3README.md` | Instructions and expected results for the Task 3 fixture demonstration. |
| `PHASE 2 CITS3301_5501 SOFTWARE TESTING AND QUALITY ASSURANCE.md` | Group Phase 2 report in Markdown. |
| `PHASE 2 CITS3301_5501 SOFTWARE TESTING AND QUALITY ASSURANCE (1).docx` | Word copy of the group report. |
| `Prototype README.md` | Original prototype documentation. |

**Note:** The uploaded repository snapshot does not contain a `pom.xml`, Maven wrapper, or `invalid-policy.pdf` fixture. These need to be added if they are required for the final submission. The existing prototype README refers to Maven commands that cannot run from this snapshot as-is.

## Task 1 — Scraper prototype

The Java scraper starts from the UWA policy index page:

`https://uwa-policy.cits5501.arranstewart.io/policy/`

The prototype:

- Retrieves the index page and extracts HTML `<a href>` links using **jsoup**.
- Resolves relative links to absolute URLs.
- Filters HTTP(S) links by common PDF and Office-document extensions.
- Removes duplicate document URLs, including fragment-only differences.
- Attempts up to **five** document downloads.
- Prints each document URL, HTTP status, declared content type, response size, first eight bytes, and a basic file-type guess.
- Does **not** save downloaded documents to disk.

### Request limits

- Maximum **20** requests per run (as counted by the scraper).
- **10-second** connection/request timeout.
- **1-second** delay between requests.
- Redirects are followed using Java's `HttpClient.Redirect.NORMAL` policy.

### Requirements and running the scraper

- **JDK 17 or later**.
- The **jsoup** Java library on the classpath.

`Scraper.java` declares `package scraper;`, so its source file must be compiled into the corresponding package layout. This repository snapshot has no build configuration or bundled jsoup dependency, so there is **not yet a verified one-command run instruction** for the checked-in files.

Once the group adds a working `pom.xml` and the Maven wrapper configured to run `scraper.Scraper`, the intended commands described in the prototype documentation are:

```bash
# macOS/Linux — after adding the Maven build files
./mvnw -q compile exec:java

# Optional custom starting page
./mvnw -q compile exec:java -Dexec.args="http://localhost:3000/"
```

These commands are **not runnable from the current ZIP without the missing Maven files**. Do not present them as tested until the build configuration has been added and checked.

## Task 2 — Test design

The group report documents test scenarios for scraper behaviour under normal and abnormal conditions, including changes to webpage structure, truncated HTML, timeouts, HTTP error responses, and invalid documents. Refer to the Markdown report for the test cases and expected outcomes.

## Task 3 — Test fixture and test double

`FixtureServer.java` implements a small local HTTP server using Java's built-in `jdk.httpserver` module. It serves the path `/invalid-policy.pdf` on `127.0.0.1:8765` and intentionally responds with:

- HTTP `200 OK`;
- `Content-Type: application/pdf`;
- a PDF download filename;
- **HTML content instead of actual PDF bytes**.

This checks whether a scraper trusts the URL and headers without inspecting the response body.

### Run the fixture server

The server reads `invalid-policy.pdf` from the current working directory. **That fixture file is not included in the uploaded snapshot**; add the HTML-content fixture under that exact filename before running the demonstration.

```bash
javac --add-modules jdk.httpserver FixtureServer.java
java --add-modules jdk.httpserver FixtureServer
```

In a second terminal:

```bash
curl --silent --show-error --dump-header response.headers \
  --output downloaded-policy.pdf \
  http://127.0.0.1:8765/invalid-policy.pdf

cat response.headers
head -n 5 downloaded-policy.pdf
od -An -tx1 -N8 downloaded-policy.pdf
```

The expected result is a response that claims to be a PDF but whose first bytes are HTML (`<!DOCTYPE html>`), rather than the `%PDF-` signature. See [`Task3README.md`](Task3README.md) for the detailed demonstration and expected behaviour.

## Task 4 — Adversarial inputs

The report proposes unusual conditions that may reveal hidden assumptions in another team's scraper, including:

1. Extremely large downloads.
2. Unexpected document formats.
3. A `.pdf` link that returns HTML.
4. Special characters in filenames or URLs.
5. Repeated document links.
6. Relative document URLs.
7. Interrupted downloads.
8. Redirects to a login page.

These are **test ideas**, not claims that all scenarios were implemented or executed.

## Known limitations

- The scraper identifies candidate documents mainly by URL extension, so extension-less download endpoints may be missed.
- File-type detection uses a short byte signature and does not fully validate document integrity.
- A completed HTTP response can be reported even when its status is not successful; the output must be interpreted accordingly.
- There is no retry mechanism for failed requests.
- The current snapshot lacks a reproducible Maven build configuration and the Task 3 fixture file.

## Submission notes

Check the assessment instructions for required deliverables, including the report formats, runnable source code, test fixture, and team details. The repository alone does not replace the formal assignment submission.
