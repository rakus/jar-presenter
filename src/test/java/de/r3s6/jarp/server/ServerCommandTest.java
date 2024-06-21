package de.r3s6.jarp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ServerCommandTest {

    @Test
    void testEscapeHtml() {

        assertEquals("&#60; &#62;", ServerCommand.escapeHtml("< >"));
        assertEquals("&#60;&#62;", ServerCommand.escapeHtml("<>"));
        assertEquals("Use &#60;xml&#62;", ServerCommand.escapeHtml("Use <xml>"));

        assertEquals("&#34;This is a quote&#39;", ServerCommand.escapeHtml("\"This is a quote'"));
        assertEquals("&#38;34; is a double quote", ServerCommand.escapeHtml("&34; is a double quote"));

        // Unicode "empty set": ∅
        assertEquals("&#8709;", ServerCommand.escapeHtml("" + (char) 8709));
        // Greek Theta: ϑ
        assertEquals("&#977;", ServerCommand.escapeHtml("" + (char) 977));
        // Smiley: ☺
        assertEquals("&#9786;", ServerCommand.escapeHtml("" + (char) 9786));

    }
}
