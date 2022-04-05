package de.r3s6.jarp.server;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import de.r3s6.jarp.args.ArgsParser;
import de.r3s6.jarp.args.ArgsParser.Counter;
import de.r3s6.jarp.args.ArgsParser.Flag;
import de.r3s6.jarp.args.CmdLineArgExcpetion;

/**
 * Command that starts a HTTP server and serves the presentation. directory in
 * the sub-folder "presentation".
 *
 * @author rks
 */
public final class ServerCommand {

    private boolean mStartBrowser;
    private int mServerPort;
    private int mVerbosity;
    private boolean mUseGui;

    private ServerCommand() {
    }

    /**
     * Create a ServerCommand.
     *
     * @return a new ServerCommand
     */
    public static ServerCommand create() {
        return new ServerCommand();
    }

    /**
     * Processes the command line parameter.
     *
     * @param args the command line parameter.
     * @return this
     */
    public ServerCommand args(final Deque<String> args) {
        // -b start default browser
        // -B <browser cmd>
        // -p <port> start Server on this port
        // -v verbose, use multiple times to increase verbosity

        boolean wantGui = false;
        try {
            final ArgsParser ah = new ArgsParser(ServerCommand::showHelp);

            final Flag browserOpt = ah.addFlag('b');
            final Counter verboseOpt = ah.addCounter('v');
            final Flag guiOpt = ah.addFlag('g');
            final List<String> optionalArgs = new ArrayList<>();
            ah.optionalArgumentList(optionalArgs);

            ah.parse(args);

            mStartBrowser = browserOpt.getValue();
            mVerbosity = verboseOpt.getValue();
            wantGui = guiOpt.getValue();

            if (optionalArgs.size() == 1) {
                setPort(optionalArgs.get(0));
            } else if (optionalArgs.size() > 1) {
                System.err.println("Superflous arguments beginning with: " + optionalArgs.get(1));
                showHelp();
                System.exit(1);
            }

        } catch (final CmdLineArgExcpetion e) {
            System.err.println(e.getMessage());
            showHelp();
            System.exit(1);
        }

        // Should we use the gui.

        if ((wantGui || System.console() == null) && !GraphicsEnvironment.isHeadless()) {
            mUseGui = true;
        }

        return this;
    }

    /**
     * Shows the command line help for the ServerCommand.
     */
    public static void showHelp() {

        System.out.println("server - starts a web server to serve the presentation");
        System.out.println("      USAGE: java -jar jar-presenter.jar server [-b] [-v] [-g] [port]");
        System.out.println("        -b       immediately start the (default) browser");
        System.out.println("        -v       increase logging output");
        System.out.println("        -g       Use gui to report that server is running. Default when no");
        System.out.println("                 terminal is attached.");
        System.out.println("        port     use given port (default is random)");

    }

    private void setPort(final String fetchArgument) {
        if (fetchArgument == null) {
            throw new RuntimeException("Missing argument for -p");
        }
        final int port = Integer.parseInt(fetchArgument);
        if (port < 0 && port > 65535) { // NOCS: MagicNumber
            throw new RuntimeException("Port out of range 0 - 65535");
        }
        mServerPort = port;
    }

    /**
     * Start serving by starting the HTTP server.
     */
    public void serve() {
        Logger.instance().verbosity(mVerbosity);

        try (HttpServerchen srv = new HttpServerchen(mServerPort)) {
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

            final URI uri = URI.create("http://localhost:" + port);

            if (mUseGui) {
                new Thread(() -> {
                    showGuiDialog(uri);
                    System.exit(0);
                }).start();

            } else {
                final String message = "Serving on " + uri
                        + "\n\nPoint your browser to that address to see the presentation";
                System.out.println();
                System.out.println(message);
                System.out.println();
            }

            if (mStartBrowser) {
                // Unfortunately Desktop.getDesktop().browse(uri) didn't work and didn't produce
                // any error.
                openBrowser(uri);
            }

            try {
                serverThread.join();
            } catch (final InterruptedException e) {
                // IGNORED
            }
        } catch (final IOException e) {
            reportError("Can't start HttpServerchen: " + e.toString());
            System.exit(1);
        }
        System.exit(0);
    }

    private void reportError(final String... messages) {
        if (mUseGui) {
            // Show gui error message

            final StringBuffer sb = new StringBuffer();
            sb.append("<html><body>");
            for (final String line : messages) {
                sb.append("<p>").append(line).append("</p>");
            }
            sb.append("</body></html>");

            final JEditorPane msgPane = new JEditorPane("text/html", sb.toString());
            msgPane.setEditable(false);
            msgPane.setBackground(new JLabel().getBackground());

            // show
            JOptionPane.showOptionDialog(null, msgPane, "Jar-Presenter", JOptionPane.ERROR_MESSAGE,
                    JOptionPane.ERROR_MESSAGE, null, new String[] { "OK" }, null);
        } else {
            System.err.println("ERROR: " + String.join(" ", messages));
        }

    }

    private void showGuiDialog(final URI uri) {

        final JEditorPane msgPane = new JEditorPane("text/html",
                "<html><body>" + "<p>Serving on <a href=\"" + uri + "\">" + uri + "</a></p>"
                        + "<p>Point your browser to that address to see the presentation.</p>"
                        + "<p>Close this dialog to stop the server.</p>" + "</body></html>");
        msgPane.setEditable(false);
        msgPane.setBackground(new JLabel().getBackground());

        // handle link events
        msgPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                    openBrowser(uri);
                }
            }
        });

        // show
        JOptionPane.showOptionDialog(null, msgPane, "Jar-Presenter", JOptionPane.PLAIN_MESSAGE,
                JOptionPane.INFORMATION_MESSAGE, null, new String[] { "Stop Server" }, null);
    }

    private void openBrowser(final URI uri) {

        final String[] command;

        switch (OsType.DETECTED) {
        case Windows:
            command = new String[] { "start", uri.toString() };
            break;
        case MacOS:
            command = new String[] { "open", uri.toString() };
            break;
        default:
            // Linux and other systems. Assuming "other" is some unix-like flavor.
            command = new String[] { "xdg-open", uri.toString() };
            break;
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectOutput(Redirect.DISCARD);
        processBuilder.redirectError(Redirect.DISCARD);
        try {
            processBuilder.start();
        } catch (final IOException e) {
            reportError("Failed to start command", String.join(" ", command), e.toString());
        }
    }

}
