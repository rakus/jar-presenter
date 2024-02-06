package de.r3s6.jarp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.r3s6.jarp.server.HttpTestUtils.Response;

class HttpServerchenTest {

    private static final String DATA_DIR = "test-data";

    private static HttpServerchen sHttpd;
    private static URL sBaseUrl;
    private static int sPort;

    @BeforeAll
    static void startServer() {
        // set to higher value while debugging
        Logger.instance().verbosity(0);
        try {
            sHttpd = new HttpServerchen(0, DATA_DIR);
            sPort = sHttpd.getPort();
            sBaseUrl = new URL("http://localhost:" + sPort);

            new Thread(() -> {
                try {
                    sHttpd.serve();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }).start();
            // Sleep for a moment, so the server thread is started.
            Thread.sleep(100);
        } catch (final IOException | InterruptedException e) {
            sHttpd.shutdown();
            fail("Starting http Server failed", e);
        }
    }

    @AfterAll
    static void shutdownServer() {
        sHttpd.shutdown();
    }

    @Test
    void testIndex() throws IOException, InterruptedException {

        final String expectedHtml = "<html><head><title>INDEX</title></head><body>INDEX</body></html>";

        Response response = HttpTestUtils.doGet(sBaseUrl);
        assertEquals(200, response.getResponseCode());
        assertEquals("text/html", response.getHeader("Content-Type"));
        assertNotNull(response.getHeader("Last-Modified"));
        assertTrue(response.getHeader("Last-Modified").endsWith("GMT"));
        assertNotNull(response.getBodyAsString());
        assertEquals(expectedHtml, response.getBodyAsString().trim());

        response = HttpTestUtils.doGet(new URL(sBaseUrl, "index.html"));
        assertEquals(200, response.getResponseCode());
        assertEquals("text/html", response.getHeader("Content-Type"));
        assertNotNull(response.getHeader("Last-Modified"));
        assertTrue(response.getHeader("Last-Modified").endsWith("GMT"));
        assertNotNull(response.getBodyAsString());
        assertEquals(expectedHtml, response.getBodyAsString().trim());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/../index.html",
            "/../META-INF/MANIFEST.MF",
            "/./../META-INF/MANIFEST.MF",
            "test/../../index.html",
            "/./../META-INF/MANIFEST.MF",
            "/./..//////META-INF/MANIFEST.MF"
    })
    void invalidRequestPath(final String path) throws IOException {

        final Response response = HttpTestUtils.doGet(new URL(sBaseUrl, path));
        assertEquals(400, response.getResponseCode());
        assertTrue(response.getBodyAsString().contains("Invalid request path"),
                "Expected text 'Invalid request path' not found in body");
    }

    @Test
    void testMapped() throws IOException, InterruptedException {
        final Response response = HttpTestUtils.doGet(new URL(sBaseUrl, "mapped"));
        assertEquals(200, response.getResponseCode());
        assertEquals("Map-Target", response.getBodyAsString());
    }

    @ParameterizedTest
    @CsvSource({ "one-pixel.gif,image/gif", "one-pixel.jpg,image/jpeg", "one-pixel.png,image/png",
            "one-pixel.svg,image/svg+xml", "index.html,text/html" })
    void testGetFile(final String filename, final String contentType) throws IOException, URISyntaxException {

        final URL url = this.getClass().getClassLoader().getResource(DATA_DIR + "/" + filename);
        final long size = Files.size(Path.of(url.toURI()));

        final Response response = HttpTestUtils.doGet(new URL(sBaseUrl, filename));
        assertEquals(200, response.getResponseCode());
        assertEquals(contentType, response.getHeader("Content-Type"));
        assertNotNull(response.getBody());
        assertEquals(size, response.getBody().length);

    }

    @Test
    void testEtagNotModified() throws IOException, InterruptedException {
        final URL url = new URL(sBaseUrl, "index.html");

        Response response = HttpTestUtils.doGet(url);
        assertEquals(200, response.getResponseCode());
        assertNotNull(response.getHeader("ETag"));
        final String etag = response.getHeader("ETag");

        response = HttpTestUtils.doGet(url, Collections.singletonMap("If-None-Match", etag));
        assertEquals(304, response.getResponseCode());
        assertNotNull(response.getHeader("ETag"));
        final String newEtag = response.getHeader("ETag");
        assertEquals(etag, newEtag);
    }

    @Test
    void testEtagModified() throws IOException, InterruptedException {
        final URL url = new URL(sBaseUrl, "index.html");

        Response response = HttpTestUtils.doGet(url);
        assertEquals(200, response.getResponseCode());
        assertNotNull(response.getHeader("ETag"));
        final String etag = response.getHeader("ETag");

        // Sending other ETag value, expecting 200
        response = HttpTestUtils.doGet(url, Collections.singletonMap("If-None-Match", "Wrong-ETag"));
        assertEquals(200, response.getResponseCode());
        assertNotNull(response.getHeader("ETag"));
        final String newEtag = response.getHeader("ETag");

        assertEquals(etag, newEtag);
    }

    @Test
    void testNotFound() throws IOException, InterruptedException {
        final URL url = new URL(sBaseUrl, "unknown-file");
        final Response response = HttpTestUtils.doGet(url);
        assertEquals(404, response.getResponseCode());
        assertTrue(response.getBodyAsString().contains(url.toString()),
                "Does not contain request URL: " + response.getBodyAsString());
    }

    @Test
    void testHeadRequest() throws IOException, InterruptedException {
        final URL url = new URL(sBaseUrl, "mapped");
        final Response response = HttpTestUtils.doGet(url, Collections.emptyMap());
        assertEquals(200, response.getResponseCode());

        final Map<String, List<String>> getHeaders = new HashMap<>(response.getHeaders());

        Map<String, List<String>> headHeaders = null;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            final Response headResponse = HttpTestUtils.doRequest(con, "HEAD", Collections.emptyMap());
            assertEquals(200, headResponse.getResponseCode());

            // might be understood as an empty body.
            assertTrue(headResponse.getBody() == null || headResponse.getBody().length == 0);

            headHeaders = new HashMap<>(headResponse.getHeaders());

        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        // HEAD should return the same header, but just no body
        assertEquals(getHeaders, headHeaders);

    }

    @Test
    void testHeadRequest404() throws IOException, InterruptedException {
        final URL url = new URL(sBaseUrl, "not-there");

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            final Response response = HttpTestUtils.doRequest(con, "HEAD", Collections.emptyMap());
            assertEquals(404, response.getResponseCode());
            // Response to HEAD doesn't have a body
            assertNull(response.getBodyAsString());

        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

    }

    @Test
    void testMethodDeleteResult501() throws IOException, InterruptedException {

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) sBaseUrl.openConnection();
            con.setUseCaches(false);
            final Response response = HttpTestUtils.doRequest(con, "DELETE", Collections.emptyMap());
            assertEquals(501, response.getResponseCode());
            assertNotNull(response.getHeaderList("Allow"));
            assertEquals(1, response.getHeaderList("Allow").size());
            assertEquals("GET, HEAD", response.getHeader("Allow"));
            assertNotNull(response.getBody());
            assertTrue(response.getBodyAsString().contains("DELETE"));

        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    @Test
    void testMethodPostResult501() throws IOException, InterruptedException {

        final byte[] postBytes = "This is a test".getBytes();

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) sBaseUrl.openConnection();
            con.setUseCaches(false);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "text/plain");
            con.setRequestProperty("Content-Length", String.valueOf(postBytes.length));
            con.setDoOutput(true);
            con.getOutputStream().write(postBytes);

            final int respCode = con.getResponseCode();
            assertEquals(501, respCode);

            // Add another request
            final Response response = HttpTestUtils.doGet(new URL(sBaseUrl, "mapped"),
                    Collections.emptyMap());
            assertEquals(200, response.getResponseCode());
            assertEquals("Map-Target", response.getBodyAsString());

        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
