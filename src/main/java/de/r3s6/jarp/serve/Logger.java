package de.r3s6.jarp.serve;

import java.io.PrintStream;
import java.time.LocalDateTime;

class Logger {

    private int verbosity;

    private static class InstanceHolder {
        static Logger INSTANCE = new Logger();
    }

    private Logger() {
    }

    public static Logger instance() {
        return InstanceHolder.INSTANCE;
    }

    public void verbosity(final int value) {
        verbosity = value;
    }

    void error(final String message, final Throwable thr) {
        doLog(System.err, "ERROR: " + message, thr);
    }

    void error(final String message) {
        if (verbosity >= 1) {
            doLog(System.err, message, null);
        }
    }

    void info(final String message) {
        if (verbosity >= 1) {
            doLog(System.out, message, null);
        }
    }

    void debug(final String message) {
        if (verbosity >= 3) {
            doLog(System.out, message, null);
        }
    }

    void request(final HttpRequest request) {
        if (verbosity >= 2) {
            final StringBuilder sb = new StringBuilder();
            sb.append(request.getMethod()).append(" ").append(request.getUrl()).append(" ")
                    .append(request.getHeaders());

            doLog(System.out, sb.toString(), null);
        }
    }

    void status(final HttpStatus status) {
        if (verbosity >= 2) {
            doLog(System.out, "  " + status, null);
        }
    }

    void logResponseLine(final String line) {
        if (verbosity >= 3) {
            doLog(System.out, ">>" + line + "<<", null);
        }
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
