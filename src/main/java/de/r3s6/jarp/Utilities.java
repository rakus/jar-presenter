/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility functions.
 *
 * @author Ralf Schandl
 */
public final class Utilities {

    private Utilities() {
        // EMPTY
    }

    /**
     * Loads a properties file via classloader into a Map.
     *
     * If the file is not found, a empty map is returned.
     *
     * @param resourceName the name of the classpath resource
     * @return A Map with the properties
     * @throws IOException if reading fails
     */
    public static Map<String, String> readPropertyMapResource(final String resourceName) throws IOException {
        try (InputStream in = Utilities.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                return readPropertyMap(in);
            } else {
                return Collections.emptyMap();
            }
        }
    }

    /**
     * Loads a properties Map from the given InputStream.
     *
     * The stream has to be closed externally.
     *
     * @param in InputSTream to read from
     * @return A Map with the properties
     * @throws IOException          if reading fails
     * @throws NullPointerException if the given stream is {@code null}
     */
    public static Map<String, String> readPropertyMap(final InputStream in) throws IOException {
        final Properties props = new Properties();
        props.load(in);
        final Map<String, String> map = new HashMap<>();
        for (final Map.Entry<Object, Object> entry : props.entrySet()) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return map;
    }
}
