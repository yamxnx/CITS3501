package scraper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * CITS5501 Project Phase 2 -- Task 1 minimal prototype.
 *
 * - reads an optional start URL from the command line (defaults to the
 *   policy index page),
 * - fetches that page via {@link #fetch}, which sets timeouts and a
 *   User-Agent, waits briefly between requests, and refuses to make more
 *   than {@link #MAX_REQUESTS} requests,
 * - extracts every {@code <a href="...">} link on the page and makes it
 *   absolute,
 * - filters links that look like policy documents (by file extension),
 * - downloads up to {@link #MAX_DOWNLOADS} distinct documents and prints
 *   their metadata (URL, status, content type, size, first bytes).
 *
 * Run with (see README.md for details):
 *
 * <pre>
 *   ./mvnw -q compile exec:java
 *   # OR against a locally running site
 *   ./mvnw -q compile exec:java -Dexec.args="http://localhost:3000/"
 * </pre>
 */
public class Scraper {

    public static final String START_URL =
        "https://uwa-policy.cits5501.arranstewart.io/policy/";

    // Politeness and safety limits. Please don't raise these when running
    // against the live site.
    public static final int MAX_REQUESTS = 20;                     // per run (index page + downloads)
    public static final Duration TIMEOUT = Duration.ofSeconds(10); // connect, and per request
    public static final Duration DELAY = Duration.ofSeconds(1);    // pause between requests

    /** Number of distinct documents to attempt to download (spec: 3-5). */
    public static final int MAX_DOWNLOADS = 5;

    /** Number of leading bytes of each download to display. */
    public static final int PREVIEW_BYTES = 8;

    /** File extensions treated as policy documents (PDF and common office formats). */
    public static final Set<String> POLICY_EXTENSIONS = Set.of(
        ".pdf", ".doc", ".docx", ".rtf", ".odt",
        ".xls", ".xlsx", ".ods",
        ".ppt", ".pptx", ".odp"
    );

    public static final String USER_AGENT =
        "CITS5501-student-scraper/0.1 (university course project)";

    /** Thrown when a run tries to make more than MAX_REQUESTS requests. */
    public static class RequestLimitExceededException extends RuntimeException {
        public RequestLimitExceededException(String message) {
            super(message);
        }
    }

    private final HttpClient client;
    private int requestCount = 0;

    public Scraper() {
        this.client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /** Number of requests made so far by this scraper. */
    public int getRequestCount() {
        return requestCount;
    }

    /**
     * Make a GET request, enforcing the request cap, a timeout and a delay.
     *
     * Note that this does NOT throw an exception for 4xx/5xx status
     * codes -- check {@code response.statusCode()} yourself.
     *
     * @throws RequestLimitExceededException if MAX_REQUESTS have already been made
     */
    public <T> HttpResponse<T> fetch(String url, HttpResponse.BodyHandler<T> bodyHandler)
            throws IOException, InterruptedException {
        if (requestCount >= MAX_REQUESTS) {
            throw new RequestLimitExceededException(
                "request limit of " + MAX_REQUESTS + " reached; not fetching " + url);
        }
        if (requestCount > 0) {
            Thread.sleep(DELAY.toMillis());
        }
        requestCount++;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build();
        return client.send(request, bodyHandler);
    }

    /**
     * Return the absolute URLs of every {@code <a href="...">} link in
     * {@code html}, in the order they appear. Relative links are resolved
     * against {@code baseUrl}. Duplicates are not removed.
     */
    public static List<String> extractLinks(String html, String baseUrl) {
        Document doc = Jsoup.parse(html, baseUrl);
        List<String> links = new ArrayList<>();
        for (Element a : doc.select("a[href]")) {
            links.add(a.absUrl("href"));   // "" if the href can't be resolved
        }
        return links;
    }

    /**
     * Return true if {@code url} looks like it leads to a downloadable policy
     * document, otherwise false.
     *
     * <p>A link qualifies if it is an http(s) URL whose path (ignoring any
     * query string or fragment) ends in one of {@link #POLICY_EXTENSIONS},
     * compared case-insensitively. This is a heuristic only: it misses
     * documents served from extension-less URLs, and trusts the extension
     * even if the server returns something else.
     */
    public static boolean isPolicyLink(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return false;
        }
        String path = uri.getPath();
        if (path == null) {
            return false;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        for (String ext : POLICY_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return {@code url} with any fragment ("#...") removed, so that links
     * differing only by fragment are treated as the same document.
     */
    static String stripFragment(String url) {
        int hash = url.indexOf('#');
        return hash >= 0 ? url.substring(0, hash) : url;
    }

    /**
     * Download the document at {@code url} and print its metadata: the URL,
     * HTTP status, declared content type, downloaded size, the first few
     * bytes of the body, and a rough guess at the file type based on those
     * bytes. The downloaded data is discarded afterwards.
     */
    public void describeDownload(String url) throws IOException, InterruptedException {
        HttpResponse<byte[]> response = fetch(url, HttpResponse.BodyHandlers.ofByteArray());
        byte[] body = response.body();
        int status = response.statusCode();
        String contentType = response.headers().firstValue("Content-Type").orElse("(none)");

        System.out.println("Document: " + url);
        if (!response.uri().toString().equals(url)) {
            System.out.println("  final URL:    " + response.uri());
        }
        System.out.println("  status:       " + status
            + (status >= 200 && status < 300 ? " (OK)" : " (NOT OK)"));
        System.out.println("  content type: " + contentType);
        System.out.println("  size:         " + body.length + " bytes");
        System.out.println("  first bytes:  " + previewBytes(body, PREVIEW_BYTES));
        System.out.println("  looks like:   " + guessFileType(body));
    }

   
    static String previewBytes(byte[] data, int n) {
        int len = Math.min(n, data.length);
        StringBuilder text = new StringBuilder();
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int b = data[i] & 0xff;
            text.append(b >= 32 && b < 127 ? (char) b : '.');
            if (i > 0) {
                hex.append(' ');
            }
            hex.append(String.format("%02x", b));
        }
        return "\"" + text + "\" [" + hex + "]";
    }

    
    static String guessFileType(byte[] data) {
        if (startsWith(data, "%PDF")) {
            return "PDF";
        }
        if (startsWith(data, "PK")) {
            return "ZIP-based file (e.g. .docx/.xlsx/.pptx/.odt)";
        }
        if (data.length >= 4 && (data[0] & 0xff) == 0xD0 && (data[1] & 0xff) == 0xCF
                && (data[2] & 0xff) == 0x11 && (data[3] & 0xff) == 0xE0) {
            return "legacy Office file (e.g. .doc/.xls/.ppt)";
        }
        if (startsWith(data, "{\\rtf")) {
            return "RTF";
        }
        String head = new String(data, 0, Math.min(data.length, 64),
            java.nio.charset.StandardCharsets.ISO_8859_1).trim().toLowerCase(Locale.ROOT);
        if (head.startsWith("<!doctype html") || head.startsWith("<html")) {
            return "HTML (not a document!)";
        }
        return "unknown";
    }

    private static boolean startsWith(byte[] data, String prefix) {
        if (data.length < prefix.length()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            if (data[i] != (byte) prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String startUrl = args.length > 0 ? args[0] : START_URL;
        Scraper scraper = new Scraper();

        System.out.println("Fetching index page: " + startUrl);
        HttpResponse<String> response =
            scraper.fetch(startUrl, HttpResponse.BodyHandlers.ofString());
        System.out.println("  status: " + response.statusCode()
            + ", content type: "
            + response.headers().firstValue("Content-Type").orElse(null));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            System.out.println("Index page did not return a 2xx status; giving up.");
            System.exit(1);
        }

        // response.uri() is the final URL, after any redirects.
        List<String> links = extractLinks(response.body(), response.uri().toString());
        System.out.println("Found " + links.size() + " links:");
        for (String link : links) {
            System.out.println("  " + link);
        }
        Set<String> policyLinks = new LinkedHashSet<>();
        for (String link : links) {
            if (isPolicyLink(link)) {
                policyLinks.add(stripFragment(link));
            }
        }
        System.out.println(policyLinks.size() + " distinct links look like policy documents.");

        if (policyLinks.isEmpty()) {
            System.out.println("No policy documents found -- has the site structure changed?");
            System.exit(1);
        }

        int attempted = 0;
        int succeeded = 0;
        for (String link : policyLinks) {
            if (attempted >= MAX_DOWNLOADS) {
                break;
            }
            attempted++;
            System.out.println();
            try {
                scraper.describeDownload(link);
                succeeded++;
            } catch (IOException e) {
                
                System.out.println("Document: " + link);
                System.out.println("  FAILED: " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
            } catch (RequestLimitExceededException e) {
                System.out.println("Stopping: " + e.getMessage());
                break;
            }
        }

        System.out.println();
        System.out.println("Downloads attempted: " + attempted
            + ", completed (any status): " + succeeded);
        System.out.println("Requests made: " + scraper.getRequestCount()
            + " (limit " + MAX_REQUESTS + ")");
    }
}