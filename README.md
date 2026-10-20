# CITS3501 / CITS5501 — Software Testing and Quality Assurance

## Phase 2: Policy Document Scraper and Testing

This repository contains the group's Phase 2 work on a minimal Java policy-document scraper and the design of tests for unusual website and network conditions.
| Student Name |  Student Number | GitHub ID |
| :---: | :---: | :---: |
| Yamini Singh | 24193929 | yamxnx |
| Anand Panicker | 25159471 | Anand21-Dev |
| Alan Ling | 23843412 | AlanLingYS |
| MIneth Perera | 23284373 | ethnimp |
| Suraj Kumar Vijay Kumar | 24444964 | surajkumar1818 |

## Repository contents

| File | Purpose |
| --- | --- |
| `task1-prototype/` | Java scraper prototype, Maven build files, and run instructions (Task 1). |
| `fixtures/FixtureServer.java` | Local HTTP server for the invalid-PDF fixture (Task 3). |
| `fixtures/invalid-policy.pdf` | HTML error page deliberately named as a PDF (Task 3). |
| `fixtures/README.md` | Instructions and expected results for the Task 3 demonstration. |
| `PHASE 2 CITS3301_5501 SOFTWARE TESTING AND QUALITY ASSURANCE.md` | Group Phase 2 report in Markdown. |
| `PHASE 2 CITS3301_5501 SOFTWARE TESTING AND QUALITY ASSURANCE (1).docx` | Word copy of the group report. |
| `task1-prototype/README.md` | Prototype documentation. |

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

The prototype source and Maven build files are in `task1-prototype/`. From that directory, follow its README. Its documented macOS/Linux command is:

```bash
./mvnw -q compile exec:java

# Optional custom starting page
./mvnw -q compile exec:java -Dexec.args="http://localhost:3000/"
```

The Maven wrapper may need to download Maven and jsoup on first use.

## Task 2 — Test design

The group report documents test scenarios for scraper behaviour under normal and abnormal conditions, including changes to webpage structure, truncated HTML, timeouts, HTTP error responses, and invalid documents. Refer to the Markdown report for the test cases and expected outcomes.

## Task 3 — Test fixture and test double

`fixtures/FixtureServer.java` implements a small local HTTP server using Java's built-in `jdk.httpserver` module. It serves the path `/invalid-policy.pdf` on `127.0.0.1:8765` and intentionally responds with:

- HTTP `200 OK`;
- `Content-Type: application/pdf`;
- a PDF download filename;
- **HTML content instead of actual PDF bytes**.

This checks whether a scraper trusts the URL and headers without inspecting the response body.

### Run the fixture server

The server reads `invalid-policy.pdf` from its current working directory. Run these commands from `fixtures/`:

```bash
cd fixtures
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

The expected result is a response that claims to be a PDF but whose first bytes are HTML (`<!DOCTYPE html>`), rather than the `%PDF-` signature. See [`fixtures/README.md`](fixtures/README.md) for the detailed demonstration and expected behaviour.

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
