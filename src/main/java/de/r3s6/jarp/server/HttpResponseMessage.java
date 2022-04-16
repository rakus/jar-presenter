/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.server;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A HTTP Response Message that writes to a response stream and handles chunked
 * data transfer.
 *
 * This is not fail safe. The methods {@link #header(String, String)} or
 * {@link #headers(Map)} and {@link #writeBody(byte[])} or
 * {@link #writeBody(InputStream)} has to be called in the right sequence. The
 * message is completed by calling close.
 *
 * In case of an invalid call sequence a {@link IllegalStateException} is
 * thrown.
 *
 * <b>IMPORTANT</b>: Closing the response will just flush the underlying output
 * steam but will NOT close it!
 *
 * @author Ralf Schandl
 */
public class HttpResponseMessage implements Closeable, Flushable {

    private static final Logger LOGGER = Logger.instance();

    private static final String SRV_VERSION;
    static {
        final String version = HttpResponseMessage.class.getPackage().getImplementationVersion();
        if (version != null) {
            SRV_VERSION = version;
        } else {
            SRV_VERSION = "UNKNOWN";
        }
    }

    private static final byte[] NL = utf8Bytes("\r\n");

    private final OutputStream mDelegate;

    /** State of the HTTP response. */
    private enum State {
        HEADER, BODY, DONE
    }

    private State mState;

    /**
     * HttpOutputStream wrapped around given stream.
     *
     * @param out    the actual stream to write to
     * @param status the HTTP status
     * @throws IOException when writing status failed
     */
    public HttpResponseMessage(final OutputStream out, final HttpStatus status) throws IOException {
        mDelegate = new BufferedOutputStream(out);

        println("HTTP/1.1 " + status);
        mState = State.HEADER;
        header("Server", "Jar Presenter v" + SRV_VERSION);
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
        assertState(State.HEADER);
        println(headerName + ": " + headerValue);
    }

    /**
     * Writes headers to the stream.
     *
     * The headers are written in random order. Entries containing {@code null} for
     * key or value are ignored.
     *
     * @param headers map of header-name to header value.
     * @throws IOException when writing a header fails
     */
    public void headers(final Map<String, String> headers) throws IOException {
        for (final Entry<String, String> hdr : headers.entrySet()) {
            if (hdr.getKey() != null && hdr.getValue() != null) {
                header(hdr.getKey(), hdr.getValue());
            }
        }
    }

    /**
     * Writes the response body with chunked transfer if needed.
     *
     * @param in stream to read the body
     * @throws IOException when reading the body or writing the response fails
     */
    public void writeBody(final InputStream in) throws IOException {
        assertState(State.HEADER);

        final int bufferSize = 1024 * 1024;
        final byte[] buffer = new byte[bufferSize];

        int cnt = in.readNBytes(buffer, 0, bufferSize);
        if (cnt < bufferSize) {
            // one go
            header("Content-Length", Integer.toString(cnt));
            finishHeader();
            LOGGER.logResponseLine("body - " + cnt + " bytes");
            write(buffer, 0, cnt);
        } else {
            // Chunked transfer
            header("Transfer-Encoding", "chunked");
            finishHeader();
            println(Integer.toHexString(cnt));
            LOGGER.logResponseLine("body-chunk - " + cnt + " bytes");
            write(buffer, 0, cnt);
            while ((cnt = in.readNBytes(buffer, 0, bufferSize)) > 0) {
                println();
                println(Integer.toHexString(cnt));
                LOGGER.logResponseLine("body-chunk - " + cnt + " bytes");
                write(buffer, 0, cnt);
            }
            println();
            println("0");
            println();
        }
    }

    /**
     * Write body from simple byte array.
     *
     * @param buffer body as byte array.
     * @throws IOException when writing the response fails
     */
    public void writeBody(final byte[] buffer) throws IOException {
        assertState(State.HEADER);
        header("Content-Length", Integer.toString(buffer.length));
        finishHeader();
        write(buffer);
    }

    @Override
    public void flush() throws IOException {
        mDelegate.flush();
    }

    /**
     * Close will NOT close the underlying OutputStream, just flush it!
     *
     * @throws IOException if flush fails
     */
    @Override
    public void close() throws IOException {
        if (mState == State.HEADER) {
            finishHeader();
        }
        mState = State.DONE;
        flush();
    }

    private void assertState(final State state) {
        if (mState != state) {
            throw new IllegalStateException("Expected state " + state + ", but is " + mState);
        }
    }

    /**
     * Write the empty line at the end of the headers and set {@link #mState} to
     * {@link State#BODY}.
     *
     * @throws IOException when writing fails.
     */
    private void finishHeader() throws IOException {
        mState = State.BODY;
        println();
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
