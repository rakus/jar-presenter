package de.r3s6.jarp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HttpServerchenTest {

    private static HttpServerchen sHttpd;
    private static URL sBaseUrl;
    private static int sPort;

    @BeforeAll
    static void startServer() {
        // set to higher value while debugging
        Logger.instance().verbosity(0);
        try {
            sHttpd = new HttpServerchen(0, "test-data");
            sPort = sHttpd.getPort();
            sBaseUrl = new URL("http://localhost:" + sPort);

            new Thread(() -> {
                try {
                    sHttpd.serve();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }).start();
            Thread.sleep(500);
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

        final Response response = doGet(sBaseUrl, Collections.emptyMap());
        assertEquals(200, response.getResponseCode());
        assertEquals("text/html", response.getHeaderFirst("Content-Type"));
        assertNotNull(response.getBodyAsString());
        assertEquals(expectedHtml, response.getBodyAsString().trim());
    }

    @Test
    void testMapped() throws IOException, InterruptedException {
        final Response response = doGet(new URL(sBaseUrl, "mapped"), Collections.emptyMap());
        assertEquals(200, response.getResponseCode());
        assertEquals("Map-Target", response.getBodyAsString());
    }

    @Test
    void testGifImage() throws IOException, InterruptedException {

        final Response response = doGet(new URL(sBaseUrl, "one-pixel.gif"), Collections.emptyMap());
        assertEquals(200, response.getResponseCode());
        assertEquals("image/gif", response.getHeaderFirst("Content-Type"));
    }

    @Test
    void testPngImage() throws IOException, InterruptedException {
        final Response response = doGet(new URL(sBaseUrl, "one-pixel.png"), Collections.emptyMap());
        assertEquals(200, response.getResponseCode());
        assertEquals("image/png", response.getHeaderFirst("Content-Type"));
    }

    @Test
    void testJpgImage() throws IOException, InterruptedException {
        final Response response = doGet(new URL(sBaseUrl, "one-pixel.jpg"), Collections.emptyMap());
        assertEquals(200, response.getResponseCode());
        assertEquals("image/jpeg", response.getHeaderFirst("Content-Type"));
    }

    @Test
    void testNotFound() throws IOException, InterruptedException {
        final URL url = new URL(sBaseUrl, "unknown-file");
        final Response response = doGet(url, Collections.emptyMap());
        assertEquals(404, response.getResponseCode());
        assertTrue(response.getBodyAsString().contains(url.toString()),
                "Does not contain request URL: " + response.getBodyAsString());
    }

    @Test
    void testMethodDeleteResult405() throws IOException, InterruptedException {

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) sBaseUrl.openConnection();
            final Response response = doRequest(con, "DELETE", Collections.emptyMap());
            assertEquals(405, response.getResponseCode());
            assertNotNull(response.getHeader("Allow"));
            assertEquals(1, response.getHeader("Allow").size());
            assertEquals("GET", response.getHeaderFirst("Allow"));
            assertNull(response.getBody());

        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private Response doGet(final URL url, final Map<String, String> requestHeader) throws IOException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            return doGet(con, requestHeader);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private Response doGet(final HttpURLConnection con, final Map<String, String> requestHeader) throws IOException {
        return doRequest(con, "GET", requestHeader);
    }

    private Response doRequest(final HttpURLConnection con, final String method,
            final Map<String, String> requestHeader) throws IOException {

        con.setRequestMethod(method);

        final int respCode = con.getResponseCode();
        final InputStream in;
        if (respCode < 400) {
            in = con.getInputStream();
        } else {
            in = con.getErrorStream();
        }

        if (in != null) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            in.transferTo(baos);

            in.close();
            return new Response(respCode, con.getHeaderFields(), baos.toByteArray());
        } else {
            return new Response(respCode, con.getHeaderFields(), null);
        }

    }

    @SuppressWarnings("unused")
    private static class Response {
        private int responseCode;
        private Map<String, List<String>> headers;
        private byte[] body;

        public Response(final int responseCode, final Map<String, List<String>> header, final byte[] body) {
            super();
            this.responseCode = responseCode;
            this.headers = header;
            this.body = body;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public List<String> getHeader(final String name) {
            return headers.get(name);
        }

        public String getHeaderFirst(final String name) {
            final List<String> hdr = headers.get(name);
            if (hdr != null && !hdr.isEmpty()) {
                return hdr.get(0);
            } else {
                return null;
            }
        }

        public byte[] getBody() {
            return body;
        }

        public String getBodyAsString() {
            if (body != null) {
                return new String(body);
            } else {
                return null;
            }
        }

    }

}
