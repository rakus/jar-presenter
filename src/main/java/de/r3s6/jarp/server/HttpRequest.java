/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.server;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a simple HTTP request.
 * <p>
 * Note: All HTTP header names are lower case.
 *
 * @author Ralf Schandl
 */
class HttpRequest {
    private final String mMethod;
    private final String mVersion;
    private final Map<String, String> mHeaders;
    private final URL mUrl;

    private final boolean mKeepAlive;

    HttpRequest(final HttpRequest.Builder builder) throws MalformedURLException {
        mMethod = builder.mMethod;
        mVersion = builder.mVersion;
        mHeaders = Collections.unmodifiableMap(builder.mHeaders);
        final String url = "http://" + builder.mHost + builder.mPath;
        try {
            final URI uri = new URI(url);
            mUrl = uri.normalize().toURL();
        } catch (final URISyntaxException e) {
            Logger.instance().error("Invalid request URL: " + url, e);
            throw new MalformedURLException(e.getMessage());
        }

        mKeepAlive = "keep-alive".equalsIgnoreCase(mHeaders.get("connection"));
    }

    public String getMethod() {
        return mMethod;
    }

    public String getVersion() {
        return mVersion;
    }

    public URL getUrl() {
        return mUrl;
    }

    public String getPath() {
        return mUrl.getPath();
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    public String getHeader(final String name) {
        return mHeaders.get(name.toLowerCase());
    }

    public boolean isKeepAlive() {
        return mKeepAlive;
    }

    static class Builder {
        private String mMethod;
        private String mPath;
        private String mVersion;
        private Map<String, String> mHeaders = new HashMap<>();
        private String mHost;

        HttpRequest.Builder method(final String method) {
            mMethod = method;
            return this;
        }

        HttpRequest.Builder path(final String path) {
            mPath = path;
            return this;
        }

        HttpRequest.Builder version(final String version) {
            mVersion = version;
            return this;
        }

        HttpRequest.Builder host(final String host) {
            mHost = host;
            return this;
        }

        HttpRequest.Builder addHeader(final String name, final String value) {
            mHeaders.put(name.toLowerCase(), value);
            return this;
        }

        HttpRequest build() throws MalformedURLException {
            return new HttpRequest(this);
        }
    }
}
