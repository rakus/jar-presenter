package de.r3s6.jarp.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Special output stream for HTTP responses, that handles chunked transfer.
 *
 * @author rks
 */
public class HttpOutputStream extends OutputStream {

    private static final Logger LOGGER = Logger.instance();

    private static final byte[] NL = utf8Bytes("\r\n");

    private final OutputStream mDelegate;

    /**
     * HttpOutputStream wrapped around given stream.
     *
     * @param out the actual stream to write to
     */
    public HttpOutputStream(final OutputStream out) {
        mDelegate = out;
    }

    /**
     * Print a linewiht CR-LF.
     *
     * Typically used for HTTP response status and headers.
     *
     * @param str text to write
     * @throws IOException on write error
     */
    public void println(final String str) throws IOException {
        LOGGER.logResponseLine(str);
        write(utf8Bytes(str));
        write(NL);
    }

    /** Insert a HTTP line break (CR-LF). */
    public void println() throws IOException {
        write(NL);
    }

    /**
     * Writes the reponse body with chunked trabnsfer if needed.
     *
     * @param in stream to read the body
     * @throws IOException when reading the body or writing the response fails
     */
    public void writeBody(final InputStream in) throws IOException {
        final int bufferSize = 1024 * 1024;
        final byte[] buffer = new byte[bufferSize];

        int cnt = in.readNBytes(buffer, 0, bufferSize);
        if (cnt < bufferSize) {
            // one go
            println("Content-Length: " + cnt);
            println();
            write(buffer, 0, cnt);
        } else {
            // Chunked transfer
            println("Transfer-Encoding: chunked");
            println();
            println(Integer.toHexString(cnt));
            write(buffer, 0, cnt);
            while ((cnt = in.readNBytes(buffer, 0, bufferSize)) > 0) {
                println();
                println(Integer.toHexString(cnt));
                write(buffer, 0, cnt);
            }
            println();
            println("0");
        }
    }

    /**
     * Write body from simple byte array.
     *
     * @param buffer body as byte array.
     * @throws IOException when writing the response fails
     */
    public void writeBody(final byte[] buffer) throws IOException {
        println("Content-Length: " + buffer.length);
        println();
        write(buffer);
    }

    @Override
    public void write(final int b) throws IOException {
        mDelegate.write(b);
    }

    @Override
    public void flush() throws IOException {
        System.err.flush();
        mDelegate.flush();
    }

    @Override
    public void close() throws IOException {
        mDelegate.close();
    }

    private static byte[] utf8Bytes(final String string) {
        if (string != null) {
            return string.getBytes(StandardCharsets.UTF_8);
        } else {
            return new byte[0];
        }
    }

}
