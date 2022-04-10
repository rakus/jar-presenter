package de.r3s6.jarp.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpRetryException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.r3s6.jarp.JarPresenter;

/**
 * The most trivial HTTP server.
 *
 * Only serves resources available via classpath. No support for keepalive. No
 * security, nothing.
 *
 * BTW: In German the suffix "chen" is used to minimize something. Like a
 * "Schiff" (ship) is rather big. Like a cruise ship. A "SchiffCHEN" is pretty
 * small. Down to a kids toy.
 *
 * @author rks
 *
 */
public class HttpServerchen implements Closeable {

    private static final Logger LOGGER = Logger.instance();

    private static final String HTTP404_FMT = "<html><head><meta charset=\"utf-8\"><title>Not Found</title></head>"
            + "<body><p>The requested resource could not be found.</p>"
            + "<tt>%s</tt><p><sub>jar presenter</sub></p></body></html>";

    private static final String HTTP400_FMT = "<html><head><meta charset=\"utf-8\"><title>Bad Request</title></head>"
            + "<body><p>%s</p><tt>%s</tt><p><sub>jar presenter</sub></p></body></html>";

    /** Close socket when client send nothing within 60 seconds. */
    private static final int SOCKET_TIMEOUT = 60 * 1000;

    private final ServerSocket mServerSocket;

    private final String mRootDir;

    /**
     * Used to map file names to actual resource name. Most important use case is
     * when the root HTML document is _NOT_ named index.html.
     */
    private final Map<String, String> mFileMap;

    /**
     * Constructs a HttpServerchen.
     *
     * @param port    the port to open. 0 means to choose a random port.
     * @param rootDir the root dir of the resources to serve.
     * @throws IOException if reading the filemap files produces it.
     */
    public HttpServerchen(final int port, final String rootDir) throws IOException {
        // backlog = 0 -> "an implementation specific default will be used"
        mServerSocket = new ServerSocket(port, 0, InetAddress.getByName("localhost"));
        this.mRootDir = rootDir;
        this.mFileMap = readMapTable();
    }

    /**
     * Constructs a HttpServerchen.
     *
     * @param port the port to open. 0 means to choose a random port.
     * @throws IOException if reading the filemap files produces it.
     */
    public HttpServerchen(final int port) throws IOException {
        this(port, JarPresenter.PRESENTATION_DIR);
    }

    private Map<String, String> readMapTable() throws IOException {
        try (InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(mRootDir + "/" + JarPresenter.FILEMAP_BASENAME)) {
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
        return mServerSocket.getLocalPort();
    }

    /**
     * Start serving.
     *
     * @throws IOException on socket problems
     */
    public void serve() throws IOException {
        while (true) {
            final Socket client = mServerSocket.accept();
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

            while (!client.isClosed() && !client.isInputShutdown()) {
                final HttpRequest req = readRequest(br);
                if (req == null) {
                    return;
                }
                LOGGER.request(req);

                if (!"GET".equals(req.getMethod())) {
                    sendMethodNotAllowedResponse(client, req);
                    /*
                     * Close - there might be additional lines in the input stream we can't handle.
                     * E.g. POST request.
                     */
                    return;
                }

                if (!validatePath(req.getPath())) {
                    sendBadRequestResponse(client, req, "Invalid request path", req.getPath());
                } else {
                    handleRequest(client, req);

                    if (!req.isKeepAlive()) {
                        // No keep-alive -> close
                        return;
                    }
                }
            }
        } catch (final SocketException e) {
            // IGNORED Most likely socket closed by client
            LOGGER.debug(e.toString(), e);
        } catch (final SocketTimeoutException e) {
            // IGNORED No incoming data for long time. Closing Socket.
            LOGGER.debug(e.toString());
        } catch (final IOException e) {
            LOGGER.error(e.toString(), e);
            e.printStackTrace();
        } catch (final InvalidRequestException e) {
            sendBadRequestResponse(client, null, "Can't understand request", e.getMessage());
        } finally {
            try {
                LOGGER.debug("Closing socket connection");
                client.close();
            } catch (final IOException e) {
                LOGGER.debug("Socket close failed: " + e.toString());
            }

        }
    }

    /**
     * Reads a HTTP request from the given reader.
     *
     * @param br the reader to read from
     * @return a parsed {@link HttpRetryException}
     * @throws IOException             if reading fails
     * @throws InvalidRequestException if something is wrong with the request. E.g.
     *                                 Format error
     */
    private HttpRequest readRequest(final BufferedReader br) throws IOException, InvalidRequestException {
        final List<String> lines = new ArrayList<>();
        String line;
        boolean firstLine = true;
        while ((line = br.readLine()) != null) {
            if (line.isBlank()) {
                // Server should ignore one empty line before the request.
                if (!firstLine) {
                    break;
                }
            } else {
                lines.add(line);
            }
            firstLine = false;
        }

        if (line == null) {
            LOGGER.debug("No Request -- client closed");
            return null;
        }

        if (lines.size() < 2) {
            LOGGER.error("request to short: " + lines);
            return null;
        }

        final HttpRequest.Builder builder = new HttpRequest.Builder();

        final String[] requestParts = lines.get(0).split(" ");
        validateRequest(lines.get(0), requestParts);

        builder.method(requestParts[0])
                .path(URLDecoder.decode(requestParts[1], StandardCharsets.UTF_8))
                .version(requestParts[2]);

        builder.host(lines.get(1).split(" ")[1]);

        lines.stream().skip(2).forEach(s -> {
            final String name = s.substring(0, s.indexOf(":"));
            final String value = s.substring(s.indexOf(":") + 1).trim();
            builder.addHeader(name, value);
        });

        return builder.build();
    }

    private void validateRequest(final String requestLine, final String[] requestParts) throws InvalidRequestException {
        if (requestParts.length != 3) { // NOCS: MagicNumber
            throw new InvalidRequestException("Invalid request line: " + requestLine);
        }
        // check the method
        switch (requestParts[0]) {
        case "OPTIONS":
        case "GET":
        case "HEAD":
        case "POST":
        case "PUT":
        case "DELETE":
        case "TRACE":
        case "CONNECT":
            // OK
            break;
        default:
            throw new InvalidRequestException("Unknown method: " + requestLine);
        }

        // check the Request-URI
        if (!requestParts[1].startsWith("/")) {
            if (requestParts[1].startsWith("http://")) {
                // parse the URI and extract the path, ignore host.
                try {
                    final URI uri = new URI(requestParts[1]);
                    requestParts[1] = uri.getRawPath();
                } catch (final URISyntaxException e) {
                    throw new InvalidRequestException("Invalid Request-URI: " + requestLine);
                }
            } else {
                throw new InvalidRequestException("Invalid Request-URI: " + requestLine);
            }

        }

        // check the protocol
        if (!requestParts[2].startsWith("HTTP/1")) {
            throw new InvalidRequestException("Unknown protocol: " + requestLine);
        }
    }

    private boolean validatePath(final String path) {
        final String[] parts = path.split("[\\/]");
        int depth = 0;

        for (final String string : parts) {
            if ("..".equals(string)) {
                depth--;
            } else {
                depth++;
            }
        }

        return depth >= 0;
    }

    private void handleRequest(final Socket client, final HttpRequest request) {

        final String fn = "/".equals(request.getPath()) ? "/index.html" : request.getPath();

        final String resource = mRootDir + mFileMap.getOrDefault(fn, fn);
        LOGGER.debug("Serving: " + request.getPath() + " -> " + resource);

        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(resource)) {

            if (in != null) {
                sendResponse(client, request, HttpStatus.OK, Collections.emptyMap(), resource, in);
            } else {
                // 404
                send404Response(client, request);
            }
        } catch (final IOException e) {
            LOGGER.debug("Error closing resource: " + e.toString());
        }

    }

    private void sendMethodNotAllowedResponse(final Socket client, final HttpRequest request) throws IOException {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Allow", "GET");

        sendResponse(client, request, HttpStatus.METHOD_NOT_ALLOWED, Collections.emptyMap(), null, null);
    }

    private void sendBadRequestResponse(final Socket client, final HttpRequest request, final String reason,
            final String entity) {
        sendHtmlResponse(client, request, HttpStatus.BAD_REQUEST,
                String.format(HTTP400_FMT, reason, entity));
    }

    private void send404Response(final Socket client, final HttpRequest request) {
        sendHtmlResponse(client, request, HttpStatus.NOT_FOUND,
                String.format(HTTP404_FMT, request.getUrl().toString()));
    }

    private void sendHtmlResponse(final Socket client, final HttpRequest request, final HttpStatus status,
            final String content) {
        try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            sendResponse(client, request, status, Collections.emptyMap(), ".html", in);
        } catch (final IOException e) {
            LOGGER.info("Error creating HTML response: " + e.toString());
        }
    }

    // CSOFF: ParameterNumber
    // WARNING: request might be null
    private void sendResponse(final Socket client, final HttpRequest request, final HttpStatus status,
            final Map<String, String> headers, final String resource, final InputStream in) throws IOException {

        LOGGER.info(
                String.format("%d %s", status.getIntValue(), request != null ? request.getPath() : "INVALID REQUEST"));

        try (HttpResponseStream clientOutput = new HttpResponseStream(client.getOutputStream(), status)) {
            clientOutput.headers(headers);
            if (request != null && request.isKeepAlive()) {
                clientOutput.header("Connection", "keep-alive");
            } else {
                clientOutput.header("Connection", "close");
            }
            clientOutput.header("Cache-Control", "no-cache, no-store, must-revalidate");
            clientOutput.header("Pragma", "no-cache");
            clientOutput.header("Expires", "0");

            if (in != null) {
                final String[] typeInfo = ContentTypes.instance().guess(resource);

                clientOutput.header("Content-Type", typeInfo[0]);
                if (typeInfo[1] != null) {
                    clientOutput.header("Content-Encoding", typeInfo[1]);
                }
                // writeBody adds headers "Content-Length" or "Transfer-Encoding".
                clientOutput.writeBody(in);
            }
        }
    }
    // CSON: ParameterNumber

    @Override
    public void close() {
        if (this.mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reports that something with the HTTP request is wrong.
     */
    private static final class InvalidRequestException extends Exception {

        private static final long serialVersionUID = 1L;

        private InvalidRequestException(final String message) {
            super(message);
        }

    }
}
