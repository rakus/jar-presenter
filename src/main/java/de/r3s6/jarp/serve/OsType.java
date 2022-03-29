package de.r3s6.jarp.serve;

import java.util.Locale;

public enum OsType {
    Windows, MacOS, Linux, Other;

    public static final OsType DETECTED;
    static {
        String osString = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
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
