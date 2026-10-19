# Policy scraper prototype (Task 1)

A minimal Java prototype for CITS5501 Project Phase 2, Task 1.

`src/main/java/scraper/Scraper.java`:

1. Fetches the policy index page (`https://uwa-policy.cits5501.arranstewart.io/policy/`
   by default).
2. Extracts all `<a href>` links and keeps those whose path ends in a policy
   document extension (`.pdf`, `.doc`, `.docx`, `.rtf`, `.odt`, `.xls`, `.xlsx`,
   `.ods`, `.ppt`, `.pptx`, `.odp`). Duplicates (including links differing only
   by `#fragment`) are removed.
3. Downloads the first 5 distinct documents and prints, for each: URL, HTTP
   status, declared content type, size in bytes, the first 8 bytes (as text
   and hex), and a guess at the file type based on those bytes (e.g. `%PDF`
   → PDF, `PK` → modern Office file).

Downloaded data is not saved to disk. A failed download (e.g. a timeout) is
reported and the scraper moves on to the next document.

## Requirements

- A JDK, version 17 or later.

The included Maven wrapper downloads Maven on first run, and Maven downloads
the only dependency (jsoup).

## Running

From this directory:

```bash
# macOS/Linux
./mvnw -q compile exec:java

# Windows (PowerShell)
.\mvnw.cmd -q compile exec:java
```

To start from a different page (e.g. a local test server):

```bash
./mvnw -q compile exec:java -Dexec.args="http://localhost:3000/"
```

## Output

The program prints the index page status, every link found, the number of
links that look like policy documents, a metadata block for each download,
and the total number of requests made.

It exits with status 1 if the index page does not return a 2xx status or if
no policy links are found.

## Limits

All requests go through `fetch()`, which uses 10-second timeouts, a 1-second
delay between requests, and a cap of 20 requests per run. A normal run makes
6 requests (1 index page + 5 documents).

## Known limitations

- Links are identified by file extension only, so documents at
  extension-less URLs are missed, and off-site links with a document
  extension are included.
- The first-bytes check doesn't confirm a file is actually valid (a corrupt
  file starting with `%PDF` is still reported as a PDF).
- No retries; a single timeout or error response counts as a failure.