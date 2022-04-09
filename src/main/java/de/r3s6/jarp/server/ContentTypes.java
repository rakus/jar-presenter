/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.r3s6.jarp.Utilities;

/**
 * Provides method to determine the content type of a file by it's file
 * extension.
 *
 * This is guessing, as the class only uses the extension, but doesn't actually
 * look into the content.
 *
 * See Resource {@code de/r3s6/jarp/server/content-types.properties} for
 * supported content types.
 *
 * Supported content encoding are gzip, compress, bzip2, xz and br.
 *
 * <b>Important</b>: File extensions are case-sensitive!
 *
 * @author Ralf Schandl
 */
final class ContentTypes {

    private static final String OCTETT_STREAM = "application/octet-stream";

    private final Map<String, String> mTypeMap = new HashMap<>();
    private final Map<String, String> mEncodingMap = new HashMap<>();

    private static class Holder {
        private static ContentTypes sInstance = new ContentTypes();
    }

    private ContentTypes() {

        // add defaults
        mTypeMap.put("html", "text/html");
        mTypeMap.put("css", "text/css");
        mTypeMap.put("js", "application/javascript");
        mTypeMap.put("png", "image/png");
        mTypeMap.put("gif", "image/gif");
        mTypeMap.put("jpg", "image/jpeg");
        mTypeMap.put("jpeg", "image/jpeg");
        mTypeMap.put("svg", "image/svg+xml");
        mTypeMap.put("woff", "font/woff");
        mTypeMap.put("woff2", "font/woff2");
        mTypeMap.put("ttf", "font/ttf");

        try {
            mTypeMap.putAll(Utilities.readPropertyMapResource("de/r3s6/jarp/server/content-types.properties"));
        } catch (final IOException e) {
            Logger.instance().error("Failed to load extended content-types table - trying without", e);
        }

        registerCompression("gzip", "gz", "gzip");
        registerCompression("compress", "Z");
        registerCompression("bzip2", "bz2", "bzip2");
        registerCompression("xz", "xz");
        registerCompression("br", "br");
    }

    private void registerCompression(final String name, final String... extensions) {
        for (final String ext : extensions) {
            mEncodingMap.put(ext, name);
        }
    }

    static ContentTypes instance() {
        return Holder.sInstance;
    }

    /**
     * Guesses the content-type and content-encoding for the given file name by its
     * extension.
     *
     * @param fileName the file name
     * @return tuple of content-type and content-encoding. Content-encoding might be
     *         {@code null}
     */
    String[] guess(final String fileName) {

        final String[] ext = getExtensions(fileName);
        if (ext == null) {
            return new String[] { OCTETT_STREAM, null };
        } else {
            return new String[] { lookupType(ext[0]), lookupEncoding(ext[1]) };
        }
    }

    private String[] getExtensions(final String filePath) {

        final String fn = basename(filePath);

        final int lastDot = fn.lastIndexOf('.');
        if (lastDot < 0) {
            // No extension
            return null;
        }

        String extension = fn.substring(lastDot + 1);

        final String[] combi = checkCombiExtension(extension);
        if (combi != null) {
            return combi;
        }

        if (!mEncodingMap.containsKey(extension)) {
            return new String[] { extension, null };
        }
        final String encoding = extension;

        final int secondLastDot = fn.lastIndexOf('.', lastDot - 1);
        if (secondLastDot < 0) {
            return new String[] { extension, null };
        }
        extension = fn.substring(secondLastDot + 1, lastDot);
        return new String[] { extension, encoding };
    }

    private String basename(final String filePath) {
        final int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash < 0) {
            return filePath;
        }
        String fn = filePath.substring(lastSlash + 1);
        while (fn.length() > 0 && fn.charAt(0) == '.') {
            // Not very efficient, but more than on dot is unlikely.
            fn = fn.substring(1);
        }
        return fn;
    }

    private String[] checkCombiExtension(final String extension) {

        switch (extension) {
        case "svgz":
            return new String[] { "svg", "gz" };
        case "tgz":
        case "taz":
        case "tz":
            return new String[] { "tar", "gz" };
        case "tbz2":
            return new String[] { "tar", "bzip2" };
        case "txz":
            return new String[] { "tar", "xz" };
        default:
            return null;
        }
    }

    private String lookupType(final String fileExtension) {
        return mTypeMap.getOrDefault(fileExtension, OCTETT_STREAM);
    }

    private String lookupEncoding(final String fileExtension) {
        return mEncodingMap.get(fileExtension);
    }

}
