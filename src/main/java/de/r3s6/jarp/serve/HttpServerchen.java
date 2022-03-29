package de.r3s6.jarp.serve;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
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

    private static final String HTTP404_FMT = "<html><head><title>Not Found</title></head>"
            + "<body><p>The requested resource could not be found.</p>"
            + "<tt>%s</tt><p><sub>jar presenter</sub></p></body></html>";

    private final ServerSocket serverSocket;

    private final String rootDir;
    private final Map<String, String> fileMap;

    public HttpServerchen(String rootDir) throws IOException {
        // automatically choose an available port
        serverSocket = new ServerSocket(0);
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
                Properties props = new Properties();
                props.load(in);
                Map<String, String> map = new HashMap<String, String>();
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
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

            // try (Socket client = serverSocket.accept()) {
            // handleClient(client);
            // }
        }
    }

    private void handleClient(Socket client) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    break;
                }
                lines.add(line);
            }

            if (lines.size() < 2) {
                logError("request to short: " + lines);
                return;
            }

            String[] requestParts = lines.get(0).split(" ");
            if (requestParts.length < 3) {
                logError("Invalid request: " + lines.get(0));
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];
            String version = requestParts[2];

            if (version == null || !version.startsWith("HTTP")) {
                logError("Invalid request: " + lines.get(0));
                return;
            }

            String host = lines.get(1).split(" ")[1];

            Map<String, String> headers = new HashMap<>();

            lines.stream().skip(2).forEach(s -> {
                String name = s.substring(0, s.indexOf(":"));
                String value = s.substring(s.indexOf(":") + 1).trim();
                headers.put(name, value);
            });

            URL url = new URL("http://" + host + path);

            // log(String.format("%s %s headers: %s", method, url, headers.toString()));
            logRequest(method, url);
            logHeaders(headers);

            if ("GET".equals(method)) {
                String fn = "/".equals(url.getPath()) ? "/index.html" : url.getPath();

                log("Serving: " + fn);

                String resource = rootDir + fileMap.getOrDefault(fn, fn);

                // resource = resource.substring(1);
                String contentType = guessContentType(resource);

                try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(resource)) {

                    if (in != null) {
                        sendResponse(client, HttpStatus.OK, Collections.emptyMap(), contentType, in);
                    } else {
                        // 404
                        send404Response(client, url);
                    }
                } catch (IOException e) {
                    log("Error closing response: " + e.toString());
                }
            } else {
                sendMethodNotAllowedResponse(client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                log("Socket close failed: " + e.toString());
            }

        }
    }

    private void sendMethodNotAllowedResponse(Socket client) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Allow", "GET");

        sendResponse(client, HttpStatus.METHOD_NOT_ALLOWED, Collections.emptyMap(), null, null);
    }

    private void send404Response(Socket client, URL url) {
        sendHtmlResponse(client, HttpStatus.NOT_FOUND, String.format(HTTP404_FMT, url.toString()));
    }

    private void sendHtmlResponse(Socket client, HttpStatus status, String content) {
        try (InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            sendResponse(client, status, Collections.emptyMap(), "text/html; charset=utf-8", in);
        } catch (IOException e) {
            log("Error creating HTML response: " + e.toString());
        }
    }

    private void sendResponse(Socket client, HttpStatus status, Map<String, String> headers, String contentType,
            InputStream in) {
        logStatus(status);
        try (HttpOutputStream clientOutput = new HttpOutputStream(client.getOutputStream())) {
            clientOutput.println("HTTP/1.0 \r\n" + status);
            for (Entry<String, String> entry : headers.entrySet()) {
                clientOutput.println(entry.getKey() + ": " + entry.getValue());
            }
            clientOutput.println("Cache-Control: no-cache, no-store, must-revalidate");
            clientOutput.println("Pragma: no-cache");
            clientOutput.println("Expires: 0");

            if (in != null) {
                clientOutput.println("ContentType: " + contentType);
                clientOutput.println();
                in.transferTo(clientOutput);
            }
            clientOutput.println();
            clientOutput.println();
            clientOutput.flush();
        } catch (IOException e) {
            log("   response failed: " + e.toString());
        }
    }

    private String guessContentType(String path) {

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

    private void logRequest(String method, URL url) {
        log(String.format("%s %s", method, url));
    }

    private void logHeaders(Map<String, String> headers) {
        log(headers.toString());
    }

    private void logStatus(HttpStatus status) {
        log("  " + status);
    }

    private void logError(String message) {
        logError(message, null);
    }

    private void logError(String msg, Throwable thr) {
        System.err.print("ERROR: ");
        System.err.println(msg);
        if (thr != null) {
            thr.printStackTrace();
        }
    }

    private void log(String msg) {
        log(msg, null);
    }

    private void log(String msg, Throwable thr) {
        System.out.println(msg);
        if (thr != null) {
            thr.printStackTrace(System.out);
        }
    }

    @Override
    public void close() {
        if (this.serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
