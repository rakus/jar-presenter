package de.r3s6.jarp.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.r3s6.jarp.server.HttpTestUtils.Response;

class HttpServerchenDownloadTest {

    private static final String DATA_DIR = "test-data";

    private static HttpServerchen sHttpd;
    private static URL sBaseUrl;
    private static int sPort;
    private static Path sDataDir;

    @BeforeAll
    static void startServer(@TempDir final Path tempDirectory) throws IOException {
        Logger.instance().verbosity(0);

        final ClassLoader testDataLoader = new URLClassLoader(new URL[] { tempDirectory.toUri().toURL() });

        final Path dataDir = tempDirectory.resolve(DATA_DIR);
        sDataDir = Files.createDirectories(dataDir);

        try {
            sHttpd = new HttpServerchen(0, DATA_DIR, testDataLoader);
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

    /*-
     * Test file download with random data files.
     * - 1048575: 1 byte before switching to chunked transfer
     * - 1048576: using chunked transfer. 1 block
     * - 1048571: using chunked transfer. 1 block + 1 byte
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 13, 1712, 214831, 1048575, 1048576, 1048577, 3312317, 10485760, 20971514 })
    void testFileDownload(final int byteSize) throws IOException {

        final File file = sDataDir.resolve("testfile-" + byteSize).toFile();
        // write random data
        final byte[] fileMd5 = writeRandomData(file, byteSize);

        final Response resp = HttpTestUtils.doGet(new URL(sBaseUrl, "testfile-" + byteSize));

        assertEquals(200, resp.getResponseCode());
        assertEquals("application/octet-stream", resp.getHeader("Content-Type"));
        assertNotNull(resp.getBody());
        assertEquals(byteSize, resp.getBody().length);

        if (byteSize >= (1024 * 1024)) {
            assertEquals("chunked", resp.getHeader("Transfer-Encoding"));
            assertNull(resp.getHeader("Content-Length"));
        } else {
            assertEquals(Integer.toString(byteSize), resp.getHeader("Content-Length"));
            assertNull(resp.getHeader("Transfer-Encoding"));
        }

        final byte[] dataMd5 = calcMd5(resp.getBody());

        assertArrayEquals(fileMd5, dataMd5);

        file.delete();
    }

    /**
     * Write random data to file.
     *
     * @param file file to write to
     * @param size number of random bytes
     * @return MD5 checksum of written data
     * @throws IOException if file operation fails
     */
    private byte[] writeRandomData(final File file, final int size) throws IOException {
        final byte[] buffer = new byte[1024];
        final Random rand = new Random();

        int written = 0;

        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");

            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                while (written < (size - buffer.length)) {
                    rand.nextBytes(buffer);
                    md.update(buffer);
                    out.write(buffer);
                    written += buffer.length;
                }
                if (size - written > 0) {
                    final byte[] rest = new byte[size - written];
                    rand.nextBytes(rest);
                    md.update(rest);
                    out.write(rest);
                }
            }
            return md.digest();
        } catch (final NoSuchAlgorithmException e) {
            fail(e);
            // never reached
            throw new IllegalStateException();
        }
    }

    private byte[] calcMd5(final byte[] data) {
        try {
            return MessageDigest.getInstance("MD5").digest(data);
        } catch (final NoSuchAlgorithmException e) {
            fail(e);
            // never reached
            throw new IllegalStateException();
        }
    }
}
