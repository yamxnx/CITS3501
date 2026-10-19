# Invalid policy PDF fixture

`invalid-policy.pdf` is deliberately **HTML**, not a PDF. It resembles an error page returned when a policy document is temporarily unavailable. The local Java server serves it from a `.pdf` URL with HTTP `200 OK`, `Content-Type: application/pdf`, and a PDF-looking download filename. This recreates a misleading response in which both the URL and HTTP metadata suggest a valid PDF while the body is HTML.

This fixture covers the HTML-at-a-PDF-link failure: the body lacks the `%PDF-` signature even though the server declares `application/pdf`. A separate corrupt-PDF file would be needed to check whether a scraper rejects PDF-like bytes that a PDF parser cannot open.

The package needs only a JDK with the built-in `jdk.httpserver` module (Java 11 or newer) and `curl` for the demonstration. It does not depend on a team scraper or test suite.

## Run the demonstration

From the directory containing this README, run the following commands. The server reads `invalid-policy.pdf` from its current working directory.

```sh
javac --add-modules jdk.httpserver FixtureServer.java
java --add-modules jdk.httpserver FixtureServer
```

Keep that terminal open. In a second terminal, from the same directory, retrieve and inspect the response:

```sh
curl --silent --show-error --dump-header response.headers --output downloaded-policy.pdf http://127.0.0.1:8765/invalid-policy.pdf
cat response.headers
head -n 5 downloaded-policy.pdf
od -An -tx1 -N8 downloaded-policy.pdf
```

The response should be `HTTP/1.1 200 OK` with `Content-type: application/pdf` (header capitalization can vary) and a `Content-disposition` download filename of `invalid-policy.pdf`. The downloaded bytes begin with `<!DOCTYPE html>` (hex `3c 21 44 4f 43 54 59 50`), not the PDF signature `%PDF-` (hex `25 50 44 46 2d`). The HTML body displays a “Policy document temporarily unavailable” message. Press Ctrl-C in the server terminal to stop it. Include `README.md`, `FixtureServer.java`, and `invalid-policy.pdf` in the submission ZIP. The demo-generated `response.headers`, `downloaded-policy.pdf`, and `.class` files are not needed.

## Expected scraper behavior

A robust scraper should treat the HTTP status, URL extension, and declared content type as clues, then inspect the downloaded bytes. For a PDF, it should at least check for the `%PDF-` signature and preferably ask a PDF parser to validate the file. Here, it should reject the HTML body, report an unsuccessful or invalid download with the URL and validation reason, and avoid counting or storing it as a successfully downloaded policy PDF. A signature check alone does not prove that every PDF is otherwise well formed.

## Why use a local fixture?

The real website cannot be made to return a chosen error page on demand, and waiting for a genuine outage would make the test unpredictable. Repeatedly probing the live site also creates unnecessary traffic and may encounter request limits. This local response reproduces the same failure every time without contacting the real website.
