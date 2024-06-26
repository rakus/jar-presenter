/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.r3s6.jarp.JarPresenter;
import de.r3s6.jarp.Utilities;

/**
 * A minimalistic HTTP server.
 * <p>
 * Only serves resources available via classpath using GET and handles HEAD
 * requests. No other requests supported. <b>No security!</b>
 * <p>
 * Note that the class {@link HttpResponseMessage} also contains some relevant
 * logic regarding headers and body transfer.
 * <p>
 * BTW: In German the suffix "chen" is used to build the diminutive of
 * something. Like "PeterCHEN" is typically a small child named "Peter".
 *
 * @author Ralf Schandl
 *
 */
public class HttpServerchen implements Closeable {

    private static final Logger LOGGER = Logger.instance();

    private static final char[] HEX_CHARS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    // HTTP date format according to RFC 7231 (7.1.1.1)
    private static final String HTTP_DATE_FMT = "EEE, dd MMM yyyy HH:mm:ss z";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(HTTP_DATE_FMT, Locale.ENGLISH)
            .withZone(ZoneId.of("GMT"));

    private static final String METHOD_GET = "GET";

    private static final String METHOD_HEAD = "HEAD";

    private static final String HDR_CONTENT_ENCODING = "Content-Encoding";

    private static final String HDR_CONTENT_TYPE = "Content-Type";

    private static final String HDR_CONNECTION = "Connection";

    private static final String HDR_ETAG = "ETag";

    private static final String HDR_IF_NONE_MATCH = "If-None-Match";

    private static final String HTTP404_FMT = "<html><head><meta charset=\"utf-8\"><title>Not Found</title></head>"
            + "<body><p>The requested resource could not be found.</p>"
            + "<tt>%s</tt><p><sub>jar presenter</sub></p></body></html>";

    private static final String HTTP400_FMT = "<html><head><meta charset=\"utf-8\"><title>Bad Request</title></head>"
            + "<body><p>%s</p><tt>%s</tt><p><sub>jar presenter</sub></p></body></html>";

    private static final String HTTP500_FMT = "<html><head><meta charset=\"utf-8\"><title>Internal Server Error</title></head>"
            + "<body><p>Embarrassing...</p><tt>%s</tt><p><sub>jar presenter</sub></p></body></html>";

    private static final String HTTP501_FMT = "<html><head><meta charset=\"utf-8\"><title>Not Implemented</title></head>"
            + "<body><p>Support for HTTP Method '%s' not implemented.</p>"
            + "<p>Server only supports GET and HEAD.</p><p><sub>jar presenter</sub></p></body></html>";

    /** Close socket when client send nothing within 60 seconds. */
    private static final int SOCKET_TIMEOUT = 60 * 1000;

    private final ServerSocket mServerSocket;

    private final ClassLoader mClassLoader;

    private final String mRootDir;

    private final String mStartPage;

    private final OffsetDateTime mStartTime;
    private final String mStartTimeFormatted;

    /**
     * Initial bytes to calculate the ETag value. This are unique per started server
     * instance. The same resource will have different ETags in every started
     * server.
     */
    private final byte[] mEtagInitBytes;

    private boolean mShutdown;

    /**
     * Constructs a HttpServerchen.
     *
     * @param port        the port to open. 0 means to choose a random port.
     * @param rootDir     the root dir of the resources to serve.
     * @param classLoader the classLoader to load resources
     * @throws IOException if reading the metadata files produces it.
     */
    public HttpServerchen(final int port, final String rootDir, final ClassLoader classLoader) throws IOException {

        mClassLoader = classLoader;

        // backlog = 0 -> "an implementation specific default will be used"
        mServerSocket = new ServerSocket(port, 0, InetAddress.getByName("localhost"));
        mRootDir = rootDir;

        final String metadataFile = rootDir + '/' + JarPresenter.METADATA_BASENAME;

        final Map<String, String> metadata = Utilities.readPropertyMapResource(metadataFile, classLoader);

        if (metadata.containsKey(JarPresenter.PROP_STARTPAGE) && !metadata.get(JarPresenter.PROP_STARTPAGE).isBlank()) {
            mStartPage = "/" + metadata.get(JarPresenter.PROP_STARTPAGE).trim().replaceFirst("^/+", "");

        } else {
            mStartPage = "/index.html";
        }

        mStartTime = OffsetDateTime.now();
        mStartTimeFormatted = DATE_FORMATTER.format(mStartTime);
        mEtagInitBytes = (mStartTimeFormatted + "-" + port).getBytes();
    }

    /**
     * Constructs a HttpServerchen.
     *
     * @param port    the port to open. 0 means to choose a random port.
     * @param rootDir the root dir of the resources to serve.
     * @throws IOException if reading the metadata files produces it.
     */
    public HttpServerchen(final int port, final String rootDir) throws IOException {
        this(port, rootDir, HttpServerchen.class.getClassLoader());

    }

    /**
     * Constructs a HttpServerchen.
     *
     * @param port the port to open. 0 means to choose a random port.
     * @throws IOException if reading the metadata files produces it.
     */
    public HttpServerchen(final int port) throws IOException {
        this(port, JarPresenter.PRESENTATION_DIR);
    }

    public int getPort() {
        return mServerSocket.getLocalPort();
    }

    /**
     * Shut down the server.
     */
    public void shutdown() {
        mShutdown = true;
        try {
            mServerSocket.close();
        } catch (final IOException e) {
            LOGGER.error("Closing server socket failed.");
        }

    }

    /**
     * Start serving.
     *
     * @throws IOException on socket problems
     */
    public void serve() throws IOException {
        LOGGER.info("Listening on port " + mServerSocket.getLocalPort());
        try {
            while (true) {
                final Socket client = mServerSocket.accept();
                if (!mShutdown) {
                    new Thread(() -> handleClient(client)).start();
                } else {
                    break;
                }
            }
        } catch (final IOException e) {
            if (!mShutdown) {
                throw e;
            }
        }
        LOGGER.info("Shutting down");
        if (!mServerSocket.isClosed()) {
            mServerSocket.close();
        }
    }

    private void handleClient(final Socket client) {

        try {
            client.setSoTimeout(SOCKET_TIMEOUT);
        } catch (final SocketException e) {
            LOGGER.error("Ignoring setting socket timeout failed: " + e);
        }

        final String host = client.getLocalAddress().getCanonicalHostName() + ":" + client.getLocalPort();

        try {
            /*
             * Not try-with-resource, as this would close the client socket before exception
             * handling and hence prevent sending error responses.
             */

            final BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

            while (!client.isClosed() && !client.isInputShutdown()) {
                final HttpRequest req = readRequest(br, host);
                if (req == null) {
                    return;
                }
                LOGGER.request(req);

                if (METHOD_GET.equals(req.getMethod()) || METHOD_HEAD.equals(req.getMethod())) {
                    if (!validatePath(req.getPath())) {
                        sendBadRequestResponse(client, req, "Invalid request path", req.getPath());
                    } else {
                        handleRequest(client, req);

                        if (!req.isKeepAlive()) {
                            // No keep-alive -> close
                            return;
                        }
                    }
                } else {
                    /*-
                     * Send 501 Not Implemented. RFC7231:
                     * 6.6.2.  501 Not Implemented
                     * ...
                     * This is the appropriate response when the server does not recognize the
                     * request method and is not capable of supporting it for any resource.
                     */
                    sendMethodNotImplementedResponse(client, req);
                    /*
                     * Close - there might be additional lines in the input stream we can't handle.
                     * E.g. POST request.
                     */
                    return;
                }

            }
        } catch (final SocketException e) {
            // IGNORED Most likely socket closed by client
            LOGGER.debug(e.toString(), e);
        } catch (final SocketTimeoutException e) {
            // IGNORED No incoming data for long time. Closing Socket.
            // Should we send a HTTP 408 here?
            LOGGER.debug(e.toString());
        } catch (final IOException e) {
            LOGGER.error(e.toString(), e);
        } catch (final InvalidRequestException e) {
            LOGGER.error("Invalid Request Exception", e);
            try {
                sendBadRequestResponse(client, null, "Can't understand request", e.getMessage());
            } catch (final IOException e1) {
                LOGGER.error("Failed to send BAD_REQUEST: " + e1.toString());
                // exiting anyway
            }
        } catch (final RuntimeException e) { // NOCS: IllegalCatch
            LOGGER.error("Internal Server Error", e);
            try {
                send500Response(client, null, e);
            } catch (final IOException e1) {
                LOGGER.error("Failed to send INTERNAL SERVER ERROR: " + e1.toString());
                // exiting anyway
            }
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
     * @param br   the reader to read from
     * @param host the local hostname with port
     * @return a parsed {@link HttpRetryException}
     * @throws IOException             if reading fails
     * @throws InvalidRequestException if something is wrong with the request. E.g.
     *                                 Format error
     */
    private HttpRequest readRequest(final BufferedReader br, final String host)
            throws IOException, InvalidRequestException {
        final List<String> lines = new ArrayList<>();
        String line;
        boolean firstLine = true;
        int linesRead = 0;

        HttpRequest.Builder builder = null;

        while ((line = br.readLine()) != null) {
            LOGGER.logRequestLine(line);
            if (line.isBlank() && !firstLine) {
                break;
            } else {
                linesRead++;
                if (linesRead == 1) {
                    // parse the HTTP Request
                    builder = new HttpRequest.Builder();

                    final String[] requestParts = line.split(" ");
                    validateRequest(line, requestParts);

                    builder.method(requestParts[0])
                            .path(URLDecoder.decode(requestParts[1], StandardCharsets.UTF_8))
                            .version(requestParts[2]);
                } else {
                    // parse a HTTP header
                    final int colonIdx = line.indexOf(":");
                    if (colonIdx <= 0) {
                        throw new InvalidRequestException("Invalid request header line: " + line);
                    }
                    final String name = line.substring(0, colonIdx).trim();
                    if (name.isEmpty()) {
                        throw new InvalidRequestException("Invalid request header line: " + line);
                    }
                    final String value = line.substring(colonIdx + 1).trim();
                    builder.addHeader(name, value);
                }
                lines.add(line);
            }
            firstLine = false;
        }

        if (line == null) {
            LOGGER.debug("No Request -- client closed");
            return null;
        }

        if (builder == null) {
            LOGGER.debug("No Request received");
            return null;
        }

        builder.host(host);

        return builder.build();
    }

    private void validateRequest(final String requestLine, final String[] requestParts) throws InvalidRequestException {
        if (requestParts.length != 3) { // NOCS: MagicNumber
            throw new InvalidRequestException("Invalid request line: " + requestLine);
        }
        // check the method
        switch (requestParts[0]) {
        case METHOD_GET:
        case METHOD_HEAD:
        case "OPTIONS":
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
        final String[] parts = path.split("[\\\\/]");
        int depth = 0;

        for (final String string : parts) {
            switch (string) {
            case "..":
                depth--;
                if (depth < 0) {
                    return false;
                }
                break;
            case "":
            case ".":
                // same dir
                break;
            default:
                depth++;
                break;
            }
        }

        return true;
    }

    private void handleRequest(final Socket client, final HttpRequest request) throws IOException {

        final String fn = "/".equals(request.getPath()) ? mStartPage : request.getPath();
        if (accessProtectedFile(fn)) {
            send404Response(client, request);
            return;
        }

        final String resource = mRootDir + fn;
        LOGGER.debug("Serving: " + request.getPath() + " -> " + resource);

        final String etag = calculateEtag(resource);
        if (etag != null && etag.equals(request.getHeader(HDR_IF_NONE_MATCH))) {
            sendNotModifiedResponse(client, request, Collections.singletonMap(HDR_ETAG, etag));
        } else {

            try (InputStream in = mClassLoader.getResourceAsStream(resource)) {

                if (in != null) {
                    final Map<String, String> headers = new HashMap<>();
                    final String[] typeInfo = ContentTypes.instance().guess(resource);
                    headers.put(HDR_CONTENT_TYPE, typeInfo[0]);
                    if (typeInfo[1] != null) {
                        headers.put(HDR_CONTENT_ENCODING, typeInfo[1]);
                    }
                    if (etag != null) {
                        headers.put(HDR_ETAG, etag);
                    }

                    sendResponse(client, request, HttpStatus.OK, headers, in);
                } else {
                    // 404
                    send404Response(client, request);
                }
            }
        }
    }

    private boolean accessProtectedFile(final String fn) {
        return fn.endsWith("jarp-metadata.properties");
    }

    private String calculateEtag(final String... parts) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(mEtagInitBytes);
            for (final String str : parts) {
                md5.update(str.getBytes());
            }
            final byte[] digest = md5.digest();

            final StringBuilder sb = new StringBuilder(2 * digest.length);
            for (final byte b : digest) {
                // CSOFF: MagicNumber
                sb.append(HEX_CHARS[(b & 0xF0) >> 4]);
                sb.append(HEX_CHARS[(b & 0x0F)]);
                // CSON: MagicNumber
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.debug("MD5 algorithm not supported, no ETag header");
            return null;
        }
    }

    private void sendNotModifiedResponse(final Socket client, final HttpRequest request,
            final Map<String, String> headers)
            throws IOException {
        sendResponse(client, request, HttpStatus.NOT_MODIFIED, headers, null);
    }

    private void sendMethodNotImplementedResponse(final Socket client, final HttpRequest request) throws IOException {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Allow", METHOD_GET + ", " + METHOD_HEAD);
        headers.put(HDR_CONNECTION, "close");

        final String body = String.format(HTTP501_FMT, request.getMethod());

        sendHtmlResponse(client, request, HttpStatus.NOT_IMPLEMENTED, headers, body);
    }

    private void sendBadRequestResponse(final Socket client, final HttpRequest request, final String reason,
            final String entity) throws IOException {
        sendHtmlResponse(client, request, HttpStatus.BAD_REQUEST, Collections.emptyMap(),
                String.format(HTTP400_FMT, reason, entity));
    }

    private void send404Response(final Socket client, final HttpRequest request) throws IOException {
        sendHtmlResponse(client, request, HttpStatus.NOT_FOUND, Collections.emptyMap(),
                String.format(HTTP404_FMT, request.getUrl()));
    }

    private void send500Response(final Socket client, final HttpRequest request, final Throwable thr)
            throws IOException {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            thr.printStackTrace(pw);

            sendHtmlResponse(client, request, HttpStatus.INTERNAL_SERVER_ERROR, Collections.emptyMap(),
                    String.format(HTTP500_FMT, sw.toString()));
        }
    }

    private void sendHtmlResponse(final Socket client, final HttpRequest request, final HttpStatus status,
            final Map<String, String> headers, final String content) throws IOException {
        try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            final Map<String, String> myHeaders = new HashMap<>();
            myHeaders.putAll(headers);
            myHeaders.put(HDR_CONTENT_TYPE, "text/html");
            sendResponse(client, request, status, myHeaders, in);
        } catch (final IOException e) {
            LOGGER.error("Error creating HTML response: " + e.toString());
            throw e;
        }
    }

    // CSOFF: ParameterNumber
    // WARNING: request might be null if we were not able to parse the request
    private void sendResponse(final Socket client, final HttpRequest request, final HttpStatus status,
            final Map<String, String> headers, final InputStream in) throws IOException {

        LOGGER.info(
                String.format("%d %s", status.getIntValue(), request != null ? request.getPath() : "INVALID REQUEST"));

        try (HttpResponseMessage clientOutput = new HttpResponseMessage(request != null ? request.getMethod() : "GET",
                status, client.getOutputStream())) {

            final Map<String, String> respHeaders = new HashMap<>(headers);
            if (request != null && request.isKeepAlive()) {
                respHeaders.putIfAbsent(HDR_CONNECTION, "keep-alive");
            } else {
                respHeaders.putIfAbsent(HDR_CONNECTION, "close");
            }
            respHeaders.put("Date", DATE_FORMATTER.format(OffsetDateTime.now()));

            respHeaders.put("Last-Modified", mStartTimeFormatted);
            clientOutput.headers(respHeaders);

            if (in != null) {
                // writeBody adds headers "Content-Length" or "Transfer-Encoding".
                clientOutput.writeBody(in);
            } else {
                clientOutput.header("Content-Length", "0");
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
