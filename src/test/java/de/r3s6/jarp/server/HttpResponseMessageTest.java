package de.r3s6.jarp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HttpResponseMessageTest {

    @BeforeAll
    static void setup() {
        Logger.instance().verbosity(0);
    }

    @Test
    void testNoBody() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");

        resp.close();
        baos.close();

        final String result = baos.toString();
        final String[] parts = result.split("\r\n", -1);

        assertTrue(result.endsWith("\r\n"), "response does not end with \\r\\n");
        assertEquals(5, parts.length);
        assertEquals("HTTP/1.1 200 OK", parts[0]);
        // Can only determine version when packed in JAR
        assertEquals("Server: Jar Presenter vUNKNOWN", parts[1]);
        assertEquals("Test: test", parts[2]);
        assertEquals("", parts[3]);
        assertEquals("", parts[4]);

    }

    @Test
    void testSimpleBodyFromArray() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");

        resp.writeBody("TestCase".getBytes(StandardCharsets.UTF_8));

        resp.close();
        baos.close();

        final String result = baos.toString();
        final String[] parts = result.split("\r\n", -1);

        // print(parts);

        assertEquals(6, parts.length);
        assertEquals("HTTP/1.1 200 OK", parts[0]);
        // Can only determine version when packed in JAR
        assertEquals("Server: Jar Presenter vUNKNOWN", parts[1]);
        assertEquals("Test: test", parts[2]);
        assertEquals("Content-Length: 8", parts[3]);
        assertEquals("", parts[4]);
        assertEquals("TestCase", parts[5]);

    }

    @Test
    void testEmptyBody() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");

        resp.writeBody(new byte[0]);

        resp.close();
        baos.close();

        final String result = baos.toString();
        final String[] parts = result.split("\r\n", -1);

        // print(parts);

        assertEquals(6, parts.length);
        assertEquals("HTTP/1.1 200 OK", parts[0]);
        // Can only determine version when packed in JAR
        assertEquals("Server: Jar Presenter vUNKNOWN", parts[1]);
        assertEquals("Test: test", parts[2]);
        assertEquals("Content-Length: 0", parts[3]);
        assertEquals("", parts[4]);
        assertEquals("", parts[5]);

    }

    @Test
    void testSimpleBodyFromStream() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");

        resp.writeBody(new SameByteInputStream("1".getBytes()[0], 10));

        resp.close();
        baos.close();

        final String result = baos.toString();
        final String[] parts = result.split("\r\n", -1);

        // print(parts);

        assertEquals(6, parts.length);
        assertEquals("HTTP/1.1 200 OK", parts[0]);
        // Can only determine version when packed in JAR
        assertEquals("Server: Jar Presenter vUNKNOWN", parts[1]);
        assertEquals("Test: test", parts[2]);
        assertEquals("Content-Length: 10", parts[3]);
        assertEquals("", parts[4]);
        assertEquals("1111111111", parts[5]);

    }

    @Test
    void testMaxBodyDirect() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");

        resp.writeBody(new SameByteInputStream("1".getBytes()[0], (1024 * 1024) - 1));

        resp.close();
        baos.close();

        final String result = baos.toString();
        final String[] parts = result.split("\r\n", -1);

        // print(parts);

        assertEquals(6, parts.length);
        assertEquals("HTTP/1.1 200 OK", parts[0]);
        // Can only determine version when packed in JAR
        assertEquals("Server: Jar Presenter vUNKNOWN", parts[1]);
        assertEquals("Test: test", parts[2]);
        assertEquals("Content-Length: 1048575", parts[3]);
        assertEquals("", parts[4]);
        assertEquals((1024 * 1024) - 1, parts[5].length());
    }

    @Test
    void testChunkedBodyOneChunk() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");

        resp.writeBody(new SameByteInputStream("1".getBytes()[0], 1024 * 1024));

        resp.close();
        baos.close();

        final String result = baos.toString();
        final String[] parts = result.split("\r\n", -1);

        // print(parts);

        assertTrue(result.endsWith("\r\n"), "response does not end with \\r\\n");
        assertEquals(10, parts.length);
        assertEquals("HTTP/1.1 200 OK", parts[0]);
        // Can only determine version when packed in JAR
        assertEquals("Server: Jar Presenter vUNKNOWN", parts[1]);
        assertEquals("Test: test", parts[2]);
        assertEquals("Transfer-Encoding: chunked", parts[3]);
        assertEquals("", parts[4]);
        assertEquals("100000", parts[5]);
        assertEquals(1024 * 1024, parts[6].length());
        assertEquals("0", parts[7]);
        assertEquals("", parts[8]);
        assertEquals("", parts[9]);
    }

    @Test
    void testChunkedBodyTwoChunks() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");

        resp.writeBody(new SameByteInputStream("1".getBytes()[0], (1024 * 1024) + 1));

        resp.close();
        baos.close();

        final String result = baos.toString();
        final String[] parts = result.split("\r\n", -1);

        // print(parts);

        assertTrue(result.endsWith("\r\n"), "response does not end with \\r\\n");
        assertEquals(12, parts.length);
        assertEquals("HTTP/1.1 200 OK", parts[0]);
        // Can only determine version when packed in JAR
        assertEquals("Server: Jar Presenter vUNKNOWN", parts[1]);
        assertEquals("Test: test", parts[2]);
        assertEquals("Transfer-Encoding: chunked", parts[3]);
        assertEquals("", parts[4]);
        assertEquals("100000", parts[5]);
        assertEquals(1024 * 1024, parts[6].length());
        assertEquals("1", parts[7]);
        assertEquals("1", parts[8]);
        assertEquals("0", parts[9]);
        assertEquals("", parts[10]);
        assertEquals("", parts[11]);
    }

    @Test
    void testChunkedBodyThreeChunks() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");

        resp.writeBody(new SameByteInputStream("1".getBytes()[0], (1024 * 1024) * 2));

        resp.close();
        baos.close();

        final String result = baos.toString();
        final String[] parts = result.split("\r\n", -1);

        // print(parts);

        assertTrue(result.endsWith("\r\n"), "response does not end with \\r\\n");
        assertEquals(12, parts.length);
        assertEquals("HTTP/1.1 200 OK", parts[0]);
        // Can only determine version when packed in JAR
        assertEquals("Server: Jar Presenter vUNKNOWN", parts[1]);
        assertEquals("Test: test", parts[2]);
        assertEquals("Transfer-Encoding: chunked", parts[3]);
        assertEquals("", parts[4]);
        assertEquals("100000", parts[5]);
        assertEquals(1024 * 1024, parts[6].length());
        assertEquals("100000", parts[7]);
        assertEquals(1024 * 1024, parts[8].length());
        assertEquals("0", parts[9]);
        assertEquals("", parts[10]);
        assertEquals("", parts[11]);
    }

    @Test
    void testInvalidCallHeaderAfterBody() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");

        resp.writeBody(new byte[0]);

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resp.header("Invalid", "No header after body"));
        resp.close();

        assertEquals("Expected state HEADER, but is BODY", ex.getMessage());
    }

    @Test
    void testInvalidCallDoubleBody() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");

        resp.writeBody(new byte[0]);

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resp.writeBody(new byte[0]));
        resp.close();

        assertEquals("Expected state HEADER, but is BODY", ex.getMessage());
    }

    @Test
    void testInvalidCallHeaderAfterClose() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");
        resp.close();

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resp.header("Invalid", "No header after body"));

        assertEquals("Expected state HEADER, but is DONE", ex.getMessage());
    }

    @Test
    void testInvalidCallBodyAfterClose() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final HttpResponseMessage resp = new HttpResponseMessage("GET", HttpStatus.OK, baos);

        resp.header("Test", "test");
        resp.close();

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resp.writeBody(new byte[0]));

        assertEquals("Expected state HEADER, but is DONE", ex.getMessage());
    }

    @SuppressWarnings("unused")
    private void print(final String[] parts) {
        for (int i = 0; i < parts.length; i++) {
            final String p = parts[i];
            if (p.length() <= 80) {
                System.out.println(i + " >>" + parts[i] + "<<<");
            } else {
                System.out.println(i + " >>" + p.substring(0, 30) + "..." + p.length() + "..."
                        + p.substring(p.length() - 30) + "<<<");
            }
        }
    }

    /**
     * InputStream that provides a given number of a given byte.
     */
    private static class SameByteInputStream extends InputStream {

        private final byte mTheByte;
        private int mCount;

        public SameByteInputStream(final byte theByte, final int count) {
            mTheByte = theByte;
            mCount = count;
        }

        @Override
        public int read() throws IOException {
            if (mCount <= 0) {
                return -1;
            } else {
                mCount--;
                return mTheByte;
            }
        }

    }

}
