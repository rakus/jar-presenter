/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.server;

/**
 * HttpStatus enum with just the values we need.
 *
 * @author Ralf Schandl
 */
public enum HttpStatus {

    /** 200 - OK. */
    OK(200, "OK"),
    /** 400 - Bad Request. */
    BAD_REQUEST(400, "Bad Request"),
    /** 404 - Not Found. */
    NOT_FOUND(404, "Not Found"),
    /** 405 - Method Not Allowed. */
    NOT_IMPLEMENTED(501, "Not Implemented");

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
