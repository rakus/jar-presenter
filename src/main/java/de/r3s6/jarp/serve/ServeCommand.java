package de.r3s6.jarp.serve;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Deque;

import de.r3s6.jarp.ArgsHandler;

public class ServeCommand {

    private boolean startBrowser;
    private int serverPort;
    private int verbosity;

    public static ServeCommand create() {
        return new ServeCommand();
    }

    public ServeCommand args(final Deque<String> args) {
        // -b start default browser
        // -B <browser cmd>
        // -p <port> start Server on this port
        // -v verbose, use multiple times to increase verbosity

        final ArgsHandler ah = new ArgsHandler(args);
        String param;
        while ((param = ah.next()) != null) {
            switch (param) {
            case "-b":
                startBrowser = true;
                break;
            case "-p":
                setPort(ah.fetchArgument());
                break;
            case "-v":
                verbosity++;
                ;
                break;
            default:
                System.err.println("Unknown parameter " + param);
                // FALLTHROUGH
            case "--help":
                show_help();
                System.exit(1);

            }
        }

        return this;
    }

    public static void show_help() {

        System.out.println("java -jar jar-presenter serve [-p port] [-b] [-v]");
        System.out.println("  Start web server to serve the presentation.");
        System.out.println("    -p port  use given port (default is random)");
        System.out.println("    -b       immediately start the (default) browser");
        System.out.println("    -v       increase loggign output");

    }

    private void setPort(final String fetchArgument) {
        if (fetchArgument == null) {
            throw new RuntimeException("Missing argument for -p");
        }
        final int port = Integer.parseInt(fetchArgument);
        if (port < 0 && port > 65535) {
            throw new RuntimeException("Port out of range 0 - 65535");
        }
        serverPort = port;
    }

    public void serve() {
        Logger.instance().verbosity(verbosity);
        try (HttpServerchen srv = new HttpServerchen(serverPort)) {
            final int port = srv.getPort();
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        srv.serve();
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            final Thread serverThread = new Thread(r);
            serverThread.start();

            System.out.println("Serving on http://localhost:" + port);

            if (startBrowser) {
                openBrowser(port);
            }

            try {
                serverThread.join();
            } catch (final InterruptedException e) {
                // IGNORED
            }
        } catch (final IOException e) {
            System.err.println("Can't start HttpServerchen: " + e.toString());
        }

    }

    private void openBrowser(final int port) {

        final String url = "http://localhost:" + port;
        String[] command;

        switch (OsType.DETECTED) {
        case Windows:
            command = new String[] { "start", url };
            break;
        case MacOS:
            command = new String[] { "open", url };
            break;
        default:
            // Linux and other unixoid systems
            command = new String[] { "nohup", "xdg-open", url };
            break;
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectOutput(Redirect.DISCARD);
        processBuilder.redirectError(Redirect.DISCARD);
        try {
            processBuilder.start();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}
