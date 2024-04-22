package de.r3s6.jarp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import de.r3s6.jarp.server.HttpRequest.Builder;

class HttpRequestTest {

    @Test
    void testBasic() throws MalformedURLException {

        final Builder builder = new HttpRequest.Builder();
        final HttpRequest req = builder.host("localhost:8123")
                .method("GET")
                .path("/hello.html")
                .build();

        assertEquals(new URL("http://localhost:8123/hello.html"), req.getUrl());
        assertEquals("GET", req.getMethod());
        assertEquals("/hello.html", req.getPath());
    }

    @Test
    void testCaseInsensitiveHeaderName() throws MalformedURLException {

        final Builder builder = new HttpRequest.Builder();
        final HttpRequest req = builder.host("localhost:8123")
                .method("GET")
                .path("/hello.html")
                .addHeader("Example", "ExampleValue")
                .build();

        assertEquals("ExampleValue", req.getHeader("Example"));
        assertEquals("ExampleValue", req.getHeader("example"));
        assertEquals("ExampleValue", req.getHeader("EXAMPLE"));
    }

    @Test
    void testNoPathBecomesSlash() throws MalformedURLException {

        final Builder builder = new HttpRequest.Builder();
        final HttpRequest req = builder.host("localhost:8123")
                .build();
        assertEquals(new URL("http://localhost:8123/"), req.getUrl());
        assertEquals("/", req.getPath());
    }

    @Test
    void testBlankPathBecomesSlash() throws MalformedURLException {

        final Builder builder = new HttpRequest.Builder();
        final HttpRequest req = builder.host("localhost:8123")
                .path("")
                .build();
        assertEquals(new URL("http://localhost:8123/"), req.getUrl());
        assertEquals("/", req.getPath());
    }

}
