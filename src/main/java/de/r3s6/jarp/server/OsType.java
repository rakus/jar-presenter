/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.server;

import java.util.Locale;

/**
 * Operating system types.
 *
 * The constant {@link #DETECTED} contains the detected OS type.
 *
 * @author Ralf Schandl
 */
public enum OsType {
    // CSOFF: Javadoc
    Windows, MacOS, Linux, Other;
    // CSON: Javadoc

    /**
     * The detected operating system type.
     */
    public static final OsType DETECTED;
    static {
        // Simple try to detect the operating system.
        final String osString = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if (osString.contains("mac") || osString.contains("darwin")) {
            DETECTED = MacOS;
        } else if (osString.contains("windows")) {
            DETECTED = Windows;
        } else if (osString.contains("linux")) {
            DETECTED = Linux;
        } else {
            DETECTED = Other;
        }
    }

}
