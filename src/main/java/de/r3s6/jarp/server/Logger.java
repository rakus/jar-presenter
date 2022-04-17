/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.server;

import java.io.PrintStream;
import java.time.LocalDateTime;

/**
 * Logger for server events.
 *
 * Logs to STDOUT/ERR.
 *
 * @author Ralf Schandl
 */
final class Logger {

    private int mVerbosity;

    private static class InstanceHolder {
        static final Logger INSTANCE = new Logger();
    }

    private Logger() {
    }

    static Logger instance() {
        return InstanceHolder.INSTANCE;
    }

    public void verbosity(final int value) {
        mVerbosity = value;
    }

    void error(final String message, final Throwable thr) {
        doLog(System.err, "ERROR: " + message, thr);
    }

    void error(final String message) {
        doLog(System.err, message, null);
    }

    void log(final String message) {
        doLog(System.out, message, null);
    }

    void info(final String message) {
        if (mVerbosity >= 1) {
            doLog(System.out, message, null);
        }
    }

    void debug(final String message) {
        if (mVerbosity >= 3) { // NOCS: MagicNumber
            doLog(System.out, message, null);
        }
    }

    void debug(final String message, final Throwable thr) {
        if (mVerbosity >= 3) { // NOCS: MagicNumber
            doLog(System.out, message, thr);
        }
    }

    void request(final HttpRequest request) {
        if (mVerbosity >= 2) {
            final StringBuilder sb = new StringBuilder();
            sb.append(request.getMethod()).append(" ").append(request.getUrl()).append(" ")
                    .append(request.getHeaders());

            doLog(System.out, sb.toString(), null);
        }
    }

    void status(final HttpStatus status) {
        if (mVerbosity >= 2) {
            doLog(System.out, "  " + status, null);
        }
    }

    void logRequestLine(final String line) {
        if (mVerbosity >= 3) { // NOCS: MagicNumber
            doLog(System.out, ">>" + line, null);
        }
    }

    void logResponseLine(final String line) {
        if (mVerbosity >= 3) { // NOCS: MagicNumber
            doLog(System.out, "<<" + line, null);
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
        stream.flush();
    }

}
