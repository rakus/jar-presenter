package de.r3s6.jarp.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Special stream for HTTP responses, that handles chunked transfer.
 *
 * This is not fail safe. The methods {@link #header(String, String)} and
 * {@link #writeBody(byte[])} or {@link #writeBody(InputStream)} has to be
 * called in the right sequence.
 *
 * <b>IMPORTANT</b>: Closing the response will just flush the underlying output
 * steam but will NOT close it!
 *
 * @author rks
 */
public class HttpResponseStream implements Closeable {

    private static final Logger LOGGER = Logger.instance();

    private static final byte[] NL = utf8Bytes("\r\n");

    private final OutputStream mDelegate;

    /**
     * HttpOutputStream wrapped around given stream.
     *
     * @param out    the actual stream to write to
     * @param status the HTTP status
     * @throws IOException when writing status failed
     */
    public HttpResponseStream(final OutputStream out, final HttpStatus status) throws IOException {
        mDelegate = out;
        println("HTTP/1.1" + status);
    }

    /**
     * Writes a header to the stream.
     *
     * Don't set the headers "Content-Length" or "Transfer-Encoding". They are set
     * by the {@code writeBody} methods.
     *
     * @param headerName  the name of the header
     * @param headerValue the value of the header
     * @throws IOException if writing fails
     */
    public void header(final String headerName, final String headerValue) throws IOException {
        println(headerName + ": " + headerValue);
    }

    /**
     * Writes the response body with chunked transfer if needed.
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
        println();
        println();
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
        println();
        println();
    }

    /**
     * Close will NOT close the underlying OutputStream, just flush it!
     *
     * @throws IOException if flush fails
     */
    @Override
    public void close() throws IOException {
        System.err.flush();
        mDelegate.flush();
    }

    /**
     * Print a line with CR-LF.
     *
     * Typically used for HTTP response status and headers.
     *
     * @param str text to write
     * @throws IOException on write error
     */
    private void println(final String str) throws IOException {
        LOGGER.logResponseLine(str);
        write(utf8Bytes(str));
        write(NL);
    }

    /** Insert a HTTP line break (CR-LF). */
    private void println() throws IOException {
        write(NL);
    }

    private void write(final byte[] bytes, final int offset, final int length) throws IOException {
        mDelegate.write(bytes, offset, length);
    }

    private void write(final byte[] bytes) throws IOException {
        mDelegate.write(bytes);
    }

    private static byte[] utf8Bytes(final String string) {
        if (string != null) {
            return string.getBytes(StandardCharsets.UTF_8);
        } else {
            return new byte[0];
        }
    }

}
