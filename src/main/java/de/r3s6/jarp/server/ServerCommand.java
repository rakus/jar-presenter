package de.r3s6.jarp.server;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import de.r3s6.jarp.args.ArgsHandler;
import de.r3s6.jarp.args.ArgsHandler.Counter;
import de.r3s6.jarp.args.ArgsHandler.Flag;
import de.r3s6.jarp.args.CmdLineArgExcpetion;

public class ServerCommand {

    private boolean startBrowser;
    private int serverPort;
    private int verbosity;

    public static ServerCommand create() {
        return new ServerCommand();
    }

    public ServerCommand args(final Deque<String> args) {
        // -b start default browser
        // -B <browser cmd>
        // -p <port> start Server on this port
        // -v verbose, use multiple times to increase verbosity

        try {
            final ArgsHandler ah = new ArgsHandler();

            final Flag browserOpt = ah.addFlag('b');
            final Counter verboseOpt = ah.addCounter('v');
            final List<String> optionalArgs = new ArrayList<>();
            ah.optionalArgumentList(optionalArgs);

            ah.handle(args);

            this.startBrowser = browserOpt.getValue();
            this.verbosity = verboseOpt.getValue();

            if (optionalArgs.size() == 1) {
                setPort(optionalArgs.get(0));
            } else if (optionalArgs.size() > 1) {
                System.err.println("Superflous arguments beginning with: " + optionalArgs.get(1));
                show_help();
                System.exit(1);
            }

        } catch (final CmdLineArgExcpetion e) {
            System.err.println(e.getMessage());
            show_help();
            System.exit(1);
        }
        return this;
    }

    public static void show_help() {

        System.out.println("server - starts a web server to serve the presentation");
        System.out.println("      USAGE: java -jar jar-presenter.jar server [-b] [-v] [port]");
        System.out.println("        -b       immediately start the (default) browser");
        System.out.println("        -v       increase logging output");
        System.out.println("        port     use given port (default is random)");

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
