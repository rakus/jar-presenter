package de.r3s6.jarp.serve;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class HttpRequest {
    private final String method;
    private final String version;
    private final Map<String, String> headers;
    private final URL url;

    public HttpRequest(HttpRequest.Builder builder) throws MalformedURLException {
        this.method = builder.method;
        this.version = builder.version;
        this.headers = Collections.unmodifiableMap(builder.headers);

        this.url = new URL("http://" + builder.host + builder.path);
    }

    public String getMethod() {
        return method;
    }

    public String getVersion() {
        return version;
    }

    public URL getUrl() {
        return url;
    }

    public String getPath() {
        return url.getPath();
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    static class Builder {
        private String method;
        private String path;
        private String version;
        private Map<String, String> headers = new HashMap<String, String>();
        private String host;

        HttpRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        HttpRequest.Builder path(String path) {
            this.path = path;
            return this;
        }

        HttpRequest.Builder version(String version) {
            this.version = version;
            return this;
        }

        HttpRequest.Builder host(String host) {
            this.host = host;
            return this;
        }

        HttpRequest.Builder addHeader(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        HttpRequest build() throws MalformedURLException {
            return new HttpRequest(this);
        }
    }
}