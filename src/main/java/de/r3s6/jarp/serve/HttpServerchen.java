package de.r3s6.jarp.serve;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * The most trivial HTTP server.
 *
 * Only serves resources available via classpath. No support for keepalive. No
 * security, nothing.
 *
 * @author rks
 *
 */
public class HttpServerchen implements Closeable {

    private static final Logger LOGGER = new Logger();

    private static final String HTTP404_FMT = "<html><head><title>Not Found</title></head>"
            + "<body><p>The requested resource could not be found.</p>"
            + "<tt>%s</tt><p><sub>jar presenter</sub></p></body></html>";

    /** Close socket when client send nothing within 60 seconds. */
    private static final int SOCKET_TIMEOUT = 60 * 1000;

    private final ServerSocket serverSocket;

    private final String rootDir;

    private final Map<String, String> fileMap;

    public HttpServerchen(final String rootDir) throws IOException {
        // automatically choose an available port
        serverSocket = new ServerSocket(8000);
        this.rootDir = rootDir;
        this.fileMap = readMapTable();
    }

    public HttpServerchen() throws IOException {
        this("presentation");
    }

    private Map<String, String> readMapTable() throws IOException {
        try (InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(rootDir + "/jarp-filemap.properties")) {
            if (in != null) {
                final Properties props = new Properties();
                props.load(in);
                final Map<String, String> map = new HashMap<>();
                for (final Map.Entry<Object, Object> entry : props.entrySet()) {
                    map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
                return Collections.unmodifiableMap(map);
            } else {
                return Collections.emptyMap();
            }
        }
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public void serve() throws IOException {
        readMapTable();
        while (true) {
            final Socket client = serverSocket.accept();
            new Thread(() -> handleClient(client)).start();
        }
    }

    private void handleClient(final Socket client) {

        try {
            client.setSoTimeout(SOCKET_TIMEOUT);
        } catch (final SocketException e) {
            LOGGER.error("Ignoring setting socket timeout failed: " + e);
        }

        try {
            final BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

            while (true) {
                final List<String> lines = new ArrayList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) {
                        break;
                    }
                    lines.add(line);
                }

                if (line == null) {
                    return;
                }

                if (lines.size() < 2) {
                    LOGGER.error("request to short: " + lines);
                    return;
                }

                final HttpRequest.Builder builder = new HttpRequest.Builder();

                final String[] requestParts = lines.get(0).split(" ");
                if (requestParts.length < 3) {
                    LOGGER.error("Invalid request: " + lines.get(0));
                    return;
                }

                builder.method(requestParts[0]).path(requestParts[1]).version(requestParts[2]);

                builder.host(lines.get(1).split(" ")[1]);

                lines.stream().skip(2).forEach(s -> {
                    final String name = s.substring(0, s.indexOf(":"));
                    final String value = s.substring(s.indexOf(":") + 1).trim();
                    builder.addHeader(name, value);
                });

                final HttpRequest req = builder.build();
                LOGGER.request(req);

                if (!"GET".equals(req.getMethod())) {
                    sendMethodNotAllowedResponse(client);
                    return;
                }

                handleRequest(client, builder.build());

                if (!"keep-alive".equals(req.getHeader("Connection"))) {
                    // No keep-alive -> close
                    return;
                }

            }
        } catch (final SocketException e) {
            // IGNORED Most likely socket closed by client
        } catch (final SocketTimeoutException e) {
            // IGNORED No incoming data for long time. Closing Socket.
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                LOGGER.info("Closing socket connection");
                client.close();
            } catch (final IOException e) {
                LOGGER.info("Socket close failed: " + e.toString());
            }

        }
    }

    private void handleRequest(final Socket client, final HttpRequest request) {

        try {
            final String fn = "/".equals(request.getPath()) ? "/index.html" : request.getPath();

            LOGGER.info("Serving: " + fn);

            final String resource = rootDir + fileMap.getOrDefault(fn, fn);

            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(resource)) {

                if (in != null) {
                    sendResponse(client, HttpStatus.OK, Collections.emptyMap(), resource, in);
                } else {
                    // 404
                    send404Response(client, request.getUrl());
                }
            } catch (final IOException e) {
                LOGGER.info("Error closing response: " + e.toString());
            }
        } finally {
            try {
                client.close();
            } catch (final IOException e) {
                LOGGER.info("Socket close failed: " + e.toString());
            }

        }
    }

    private void sendMethodNotAllowedResponse(final Socket client) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Allow", "GET");

        sendResponse(client, HttpStatus.METHOD_NOT_ALLOWED, Collections.emptyMap(), null, null);
    }

    private void send404Response(final Socket client, final URL url) {
        sendHtmlResponse(client, HttpStatus.NOT_FOUND, String.format(HTTP404_FMT, url.toString()));
    }

    private void sendHtmlResponse(final Socket client, final HttpStatus status, final String content) {
        try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            sendResponse(client, status, Collections.emptyMap(), ".html", in);
        } catch (final IOException e) {
            LOGGER.info("Error creating HTML response: " + e.toString());
        }
    }

    private void sendResponse(final Socket client, final HttpStatus status, final Map<String, String> headers,
            final String resource, final InputStream in) {

        final String contentType = guessContentType(resource);

        LOGGER.status(status);
        try (HttpOutputStream clientOutput = new HttpOutputStream(client.getOutputStream())) {
            clientOutput.println("HTTP/1.1 " + status);
            for (final Entry<String, String> entry : headers.entrySet()) {
                clientOutput.println(entry.getKey() + ": " + entry.getValue());
            }
            clientOutput.println("Cache-Control: no-cache, no-store, must-revalidate");
            clientOutput.println("Pragma: no-cache");
            clientOutput.println("Expires: 0");

            if (in != null) {
                clientOutput.println("ContentType: " + contentType);
                // No empty line here, as writeBody adds headers "Content-Length" or
                // "Transfer-Encoding".
                clientOutput.writeBody(in);
            }
            clientOutput.println();
            clientOutput.println();
            clientOutput.flush();
        } catch (final IOException e) {
            LOGGER.info("   response failed: " + e.toString());
        }
    }

    private String guessContentType(final String path) {

        String ext = path.substring(path.lastIndexOf('.') + 1);
        ext = ext.toLowerCase();

        switch (ext) {
        case "html":
            return "text/html";
        case "css":
            return "text/css";
        case "js":
            return "text/javascript";
        case "png":
            return "image/png";
        case "gif":
            return "image/gif";
        case "jpg":
            return "image/jpeg";
        case "svg":
            return "image/svg+xml; charset=UTF-8";
        case "woff":
            return "application/font-woff";
        case "ttf":
            return "font/sfnt";
        default:
            return "application/octet-stream; charset=binary";
        }
    }

    @Override
    public void close() {
        if (this.serverSocket != null) {
            try {
                serverSocket.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}
