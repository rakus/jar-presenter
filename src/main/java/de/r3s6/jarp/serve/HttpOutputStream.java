package de.r3s6.jarp.serve;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpOutputStream extends OutputStream {

    private final OutputStream delegate;

    public HttpOutputStream(OutputStream out) {
        delegate = out;
    }

    public void print(String str) throws IOException {
        System.err.print(str);
        write(bytes(str));
    }

    public void println(String str) throws IOException {
        print(str);
        print("\r\n");
    }

    public void println() throws IOException {
        print("\r\n");
    }

    private byte[] bytes(String string) {
        if (string != null) {
            return string.getBytes(StandardCharsets.UTF_8);
        } else {
            return new byte[0];
        }
    }

    @Override
    public void write(int b) throws IOException {
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

}
