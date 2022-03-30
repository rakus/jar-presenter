package de.r3s6.jarp.server;

import java.util.Locale;

/**
 * Operating system types.
 *
 * The constant {@link #DETECTED} contains the detected OS type.
 *
 * @author rks
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
        if ((osString.indexOf("mac") >= 0) || (osString.indexOf("darwin") >= 0)) {
            DETECTED = MacOS;
        } else if (osString.indexOf("win") >= 0) {
            DETECTED = Windows;
        } else if (osString.indexOf("nux") >= 0) {
            DETECTED = Linux;
        } else {
            DETECTED = Other;
        }
    }

}
