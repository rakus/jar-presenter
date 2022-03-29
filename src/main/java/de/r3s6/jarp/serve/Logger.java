package de.r3s6.jarp.serve;

import java.io.PrintStream;
import java.time.LocalDateTime;

class Logger {

    void error(final String message, final Throwable thr) {
        doLog(System.err, "ERROR: " + message, thr);
    }

    void error(final String message) {
        doLog(System.err, message, null);
    }

    void info(final String message) {
        doLog(System.out, message, null);
    }

    void request(final HttpRequest request) {
        final StringBuilder sb = new StringBuilder();
        sb.append(request.getMethod()).append(" ").append(request.getUrl()).append(" ").append(request.getHeaders());

        doLog(System.out, sb.toString(), null);
    }

    void status(final HttpStatus status) {
        doLog(System.out, "  " + status, null);
    }

    void logResponseLine(final String line) {
        doLog(System.out, ">>" + line + "<<", null);
    }

    void doLog(final PrintStream stream, final String message, final Throwable thr) {
        final LocalDateTime now = LocalDateTime.now();

        final StringBuffer sb = new StringBuffer();
        sb.append(now).append(" [").append(Thread.currentThread().getId()).append("] ").append(message);
        stream.println(sb.toString());
        if (thr != null) {
            thr.printStackTrace(stream);
        }

    }

}
