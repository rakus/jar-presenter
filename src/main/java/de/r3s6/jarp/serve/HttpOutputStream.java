package de.r3s6.jarp.serve;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpOutputStream extends OutputStream {

    private static final Logger LOGGER = new Logger();

    private static final byte[] NL = utf8Bytes("\r\n");

    private final OutputStream delegate;

    public HttpOutputStream(final OutputStream out) {
        delegate = out;
    }

    public void println(final String str) throws IOException {
        LOGGER.logResponseLine(str);
        write(utf8Bytes(str));
        write(NL);
    }

    public void println() throws IOException {
        write(NL);
    }

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

    public void writeBody(final byte[] buffer) throws IOException {
        println("Content-Length: " + buffer.length);
        println();
        write(buffer);
    }

    @Override
    public void write(final int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void flush() throws IOException {
        System.err.flush();
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    private static byte[] utf8Bytes(final String string) {
        if (string != null) {
            return string.getBytes(StandardCharsets.UTF_8);
        } else {
            return new byte[0];
        }
    }

}
