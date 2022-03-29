package de.r3s6.jarp.server;

public enum HttpStatus {

    OK(200, "OK"), BAD_REQUEST(400, "Bad Request"), NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed");

    private int intValue;
    private String phrase;

    HttpStatus(final int i, final String string) {
        intValue = i;
        phrase = string;
    }

    @Override
    public String toString() {
        return intValue + " " + phrase;
    }
}
