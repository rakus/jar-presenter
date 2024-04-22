package de.r3s6.jarp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import de.r3s6.jarp.server.HttpTestUtils.Response;

class HttpServerchenTest2 {

    @Test
    void testCustomStartPage() throws MalformedURLException, IOException {

        try (TestServer server = new TestServer("mapped-index-no-slash")) {

            final String expectedHtml = "<html><head><title>MAPPED-INDEX</title></head><body>MAPPED-INDEX</body></html>";

            final Response response = HttpTestUtils.doGet(new URL("http://localhost:" + server.getPort()));
            assertEquals(200, response.getResponseCode());
            assertEquals(expectedHtml, response.getBodyAsString().trim());
        }
    }

    @Test
    void testCustomStartPageWithSlash() throws MalformedURLException, IOException {

        try (TestServer server = new TestServer("mapped-index-with-slash")) {

            final String expectedHtml = "<html><head><title>MAPPED-INDEX</title></head><body>MAPPED-INDEX</body></html>";

            final Response response = HttpTestUtils.doGet(new URL("http://localhost:" + server.getPort()));
            assertEquals(200, response.getResponseCode());
            assertEquals(expectedHtml, response.getBodyAsString().trim());
        }
    }

    private static class TestServer implements AutoCloseable {

        private final HttpServerchen mServer;

        public TestServer(final String dataDir) {

            try {
                mServer = new HttpServerchen(0, dataDir, HttpServerchenTest.class.getClassLoader());
            } catch (final IOException e) {
                fail("Starting http Server failed", e);
                // never reached
                throw new AssertionError("Starting http Server failed", e);
            }

            new Thread(() -> {
                try {
                    mServer.serve();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }).start();
            try {
                // Sleep for a moment, so the server thread is started.
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                // IGNORED
            }
        }

        public int getPort() {
            return mServer.getPort();
        }

        @Override
        public void close() {
            mServer.shutdown();
        }
    }

}
