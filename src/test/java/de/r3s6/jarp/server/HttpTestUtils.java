package de.r3s6.jarp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.opentest4j.AssertionFailedError;

class HttpTestUtils {

    static Response doGet(final URL url) throws IOException {
        return doGet(url, Collections.emptyMap());
    }

    static Response doGet(final URL url, final Map<String, String> requestHeader) throws IOException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            return doGet(con, requestHeader);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    static Response doGet(final HttpURLConnection con, final Map<String, String> requestHeader) throws IOException {
        return doRequest(con, "GET", requestHeader);
    }

    static Response doRequest(final HttpURLConnection con, final String method,
            final Map<String, String> requestHeader) throws IOException {

        con.setRequestMethod(method);
        con.setUseCaches(false);

        final int respCode = con.getResponseCode();
        final InputStream in;
        if (respCode < 400) {
            in = con.getInputStream();
        } else {
            in = con.getErrorStream();
        }

        if (in != null) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            in.transferTo(baos);

            in.close();
            return new Response(respCode, con.getHeaderFields(), baos.toByteArray());
        } else {
            return new Response(respCode, con.getHeaderFields(), null);
        }

    }

    static class Response {
        private int responseCode;
        private Map<String, List<String>> headers;
        private byte[] body;

        public Response(final int responseCode, final Map<String, List<String>> header, final byte[] body) {
            super();
            this.responseCode = responseCode;
            this.headers = header;
            this.body = body;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public List<String> getHeaderList(final String name) {
            return headers.get(name);
        }

        /**
         * Get the single value of the given header.
         *
         * @param name the header name
         * @return the single value or {@code null} if the header is not set
         * @throws AssertionFailedError if the header value is a list
         */
        public String getHeader(final String name) {
            final List<String> hdr = headers.get(name);
            if (hdr != null && !hdr.isEmpty()) {
                assertEquals(1, hdr.size());
                return hdr.get(0);
            } else {
                return null;
            }
        }

        public byte[] getBody() {
            return body;
        }

        public String getBodyAsString() {
            if (body != null) {
                return new String(body);
            } else {
                return null;
            }
        }

    }
}
