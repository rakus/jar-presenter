package de.r3s6.jarp.server;

/**
 * HttpStatus enum with just the values we need.
 *
 * @author rks
 */
public enum HttpStatus {

    // CSOFF: Javadoc
    OK(200, "OK"), BAD_REQUEST(400, "Bad Request"), NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed");
    // CSON: Javadoc

    private int mIntValue;
    private String mPhrase;

    HttpStatus(final int intValue, final String reasonPhrase) {
        mIntValue = intValue;
        mPhrase = reasonPhrase;
    }

    @Override
    public String toString() {
        return mIntValue + " " + mPhrase;
    }
}
