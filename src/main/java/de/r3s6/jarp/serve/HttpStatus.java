package de.r3s6.jarp.serve;

public enum HttpStatus {

    OK(200, "OK"), NOT_FOUND(404, "Not Found"), METHOD_NOT_ALLOWED(405, "Method Not Allowed");

    private int intValue;
    private String phrase;

    HttpStatus(int i, String string) {
        intValue = i;
        phrase = string;
    }

    @Override
    public String toString() {
        return intValue + " " + phrase;
    }
}
