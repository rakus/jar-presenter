package de.r3s6.jarp;

import java.util.Deque;

public class ArgsHandler {
    private final Deque<String> args;

    private String rest;

    public ArgsHandler(final Deque<String> args) {
        this.args = args;
    }

    public String next() {
        String ret;
        if (rest != null && rest.length() != 0) {
            ret = "-" + rest.charAt(0);
            rest = rest.substring(1);
        } else {
            if (args.size() != 0) {
                final String a = args.poll();
                if (a.startsWith("--")) {
                    ret = a;
                } else if (a.startsWith("-")) {
                    ret = "-" + a.charAt(1);
                    rest = a.substring(2);
                } else {
                    ret = a;
                }
            } else {
                ret = null;
            }
        }
        return ret;
    }

    public String fetchArgument() {
        String ret;
        if (rest != null && rest.length() != 0) {
            ret = rest;
            rest = null;
        } else {
            if (args.size() != 0) {
                ret = args.poll();
            } else {
                return null;
            }
        }
        return ret;
    }

}
