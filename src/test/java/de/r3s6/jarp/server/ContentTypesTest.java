package de.r3s6.jarp.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class ContentTypesTest {

    private final ContentTypes cts = ContentTypes.instance();

    @Test
    void test() {
        assertArrayEquals(array("text/html", null), cts.guess("test.html"));

        assertArrayEquals(array("text/html", null), cts.guess("/presentation/test.html"));
        assertArrayEquals(array("text/css", null), cts.guess("/presentation/test.css"));
        assertArrayEquals(array("application/javascript", null), cts.guess("/presentation/test.js"));
        assertArrayEquals(array("image/png", null), cts.guess("/presentation/test.png"));
        assertArrayEquals(array("image/gif", null), cts.guess("/presentation/test.gif"));
        assertArrayEquals(array("image/jpeg", null), cts.guess("/presentation/test.jpeg"));
        assertArrayEquals(array("image/jpeg", null), cts.guess("/presentation/test.jpg"));
        assertArrayEquals(array("image/svg+xml", null), cts.guess("/presentation/test.svg"));
        assertArrayEquals(array("font/woff", null), cts.guess("/presentation/test.woff"));
        assertArrayEquals(array("font/woff2", null), cts.guess("/presentation/test.woff2"));
        assertArrayEquals(array("font/ttf", null), cts.guess("/presentation/test.ttf"));

    }

    @Test
    void testUnknown() {
        assertArrayEquals(array("application/octet-stream", null), cts.guess("test.data"));
        assertArrayEquals(array("application/octet-stream", null), cts.guess("test.gz"));
        assertArrayEquals(array("application/octet-stream", null), cts.guess("test"));
    }

    @Test
    void testCornerCases() {
        assertArrayEquals(array("application/octet-stream", null), cts.guess("/presentation/.data"));
        assertArrayEquals(array("application/octet-stream", null), cts.guess("/presentation/.html"));
        assertArrayEquals(array("application/octet-stream", null), cts.guess("/presentation/.html.gz"));
        assertArrayEquals(array("text/plain", null), cts.guess("/presentation/.html.txt"));
        assertArrayEquals(array("application/octet-stream", null), cts.guess("/presentation/"));
    }

    @Test
    void testCompressed() {
        assertArrayEquals(array("text/html", "gzip"), cts.guess("test.html.gz"));
        assertArrayEquals(array("text/html", "compress"), cts.guess("test.html.Z"));
        assertArrayEquals(array("text/html", "bzip2"), cts.guess("test.html.bzip2"));
        assertArrayEquals(array("text/html", "bzip2"), cts.guess("test.html.bz2"));
        assertArrayEquals(array("text/html", "xz"), cts.guess("test.html.xz"));

        assertArrayEquals(array("image/svg+xml", "gzip"), cts.guess("test.svg.gz"));

    }

    @Test
    void testCombiNames() {
        assertArrayEquals(array("image/svg+xml", "gzip"), cts.guess("test.svgz"));
        assertArrayEquals(array("application/x-tar", "gzip"), cts.guess("test.tgz"));
        assertArrayEquals(array("application/x-tar", "gzip"), cts.guess("test.taz"));
        assertArrayEquals(array("application/x-tar", "gzip"), cts.guess("test.tz"));

        assertArrayEquals(array("application/x-tar", "bzip2"), cts.guess("test.tbz2"));

        assertArrayEquals(array("application/x-tar", "xz"), cts.guess("test.txz"));
    }

    private String[] array(final String s1, final String s2) {
        return new String[] { s1, s2 };
    }

}
