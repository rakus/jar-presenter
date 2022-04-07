package de.r3s6.jarp.server;

/**
 * HttpStatus enum with just the values we need.
 *
 * @author rks
 */
public enum HttpStatus {

    /** 200 - OK. */
    OK(200, "OK"),
    /** 400 - Bad Request. */
    BAD_REQUEST(400, "Bad Request"),
    /** 404 - Not Found. */
    NOT_FOUND(404, "Not Found"),
    /** 405 - Method Not Allowed. */
    METHOD_NOT_ALLOWED(405, "Method Not Allowed");

    private int mIntValue;
    private String mPhrase;

    HttpStatus(final int intValue, final String reasonPhrase) {
        mIntValue = intValue;
        mPhrase = reasonPhrase;
    }

    public int getIntValue() {
        return mIntValue;
    }

    @Override
    public String toString() {
        return mIntValue + " " + mPhrase;
    }
}
