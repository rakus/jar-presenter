/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.server;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import de.r3s6.jarp.args.ArgsParser;
import de.r3s6.jarp.args.ArgsParser.Counter;
import de.r3s6.jarp.args.ArgsParser.Flag;
import de.r3s6.jarp.args.CmdLineArgException;

/**
 * Command that starts a HTTP server and serves the presentation. directory in
 * the sub-folder "presentation".
 *
 * @author Ralf Schandl
 */
public final class ServerCommand {

    private boolean mStartBrowser;
    private int mServerPort;
    private int mVerbosity;
    private boolean mUseTerminal;

    private ServerCommand() {
        // If no GUI available, use terminal
        mUseTerminal = GraphicsEnvironment.isHeadless();
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
     * Shows the command line help for the ServerCommand.
     */
    public static void showHelp() {

        System.out.println("server - starts a web server to serve the presentation");
        System.out.println("      USAGE: java -jar jar-presenter.jar server [-b] [-v] [-t] [port]");
        System.out.println("        -b       immediately start the (default) browser");
        System.out.println("        -v       increase logging output");
        System.out.println("        -t       Terminal mode. Don't start GUI.");
        System.out.println("        port     use given port (default is random)");

    }

    /**
     * Start serving by starting the HTTP server.
     *
     * @param argList the command line parameter.
     */
    public void execute(final List<String> argList) {
        handleArgs(argList);
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

            if (mUseTerminal) {
                final String message = "Serving on " + uri
                        + "\n\nPoint your browser to that address to see the presentation";
                System.out.println();
                System.out.println(message);
                System.out.println();
            } else {
                EventQueue.invokeLater(() -> showGuiDialog(uri));
            }

            if (mStartBrowser) {
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

    /**
     * Processes the command line parameter.
     *
     * @param args the command line parameter.
     */
    private void handleArgs(final List<String> args) {

        boolean wantTerminal = false;
        try {
            final ArgsParser ah = new ArgsParser(ServerCommand::showHelp);

            final Flag browserOpt = ah.addFlag('b');
            final Counter verboseOpt = ah.addCounter('v');
            final Flag terminalOpt = ah.addFlag('t');
            final List<String> optionalArgs = new ArrayList<>();
            ah.optionalArgumentList(optionalArgs);

            ah.parse(args);

            mStartBrowser = browserOpt.getValue();
            mVerbosity = verboseOpt.getValue();
            wantTerminal = terminalOpt.getValue();

            if (optionalArgs.size() == 1) {
                setPort(optionalArgs.get(0));
            } else if (optionalArgs.size() > 1) {
                System.err.println("Superfluous arguments beginning with: " + optionalArgs.get(1));
                showHelp();
                System.exit(1);
            }

        } catch (final CmdLineArgException e) {
            System.err.println(e.getMessage());
            showHelp();
            System.exit(1);
        }

        // mUseTerminal might already be set if no GUI possible
        if (wantTerminal) {
            mUseTerminal = true;
        }
    }

    private void setPort(final String fetchArgument) {
        final int port = Integer.parseInt(fetchArgument);
        if (port < 0 && port > 65535) { // NOCS: MagicNumber
            throw new RuntimeException("Port out of range 0 - 65535");
        }
        mServerPort = port;
    }

    private void reportError(final String... messages) {
        if (mUseTerminal) {
            System.err.println("ERROR: " + String.join(" ", messages));
        } else {
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

        final JButton stopButton = new JButton("Stop Server");
        stopButton.addActionListener(e -> {
            System.exit(0);
        });

        final Icon icon = UIManager.getIcon("OptionPane.informationIcon");

        final JOptionPane optPane = new JOptionPane(msgPane, JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE,
                icon, new Object[] { stopButton });

        final JFrame frame = new JFrame("Jar Presentation Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(optPane, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void openBrowser(final URI uri) {

        /*-
         * Unfortunately Desktop.getDesktop().browse(uri) didn't work on Linux.
         * see https://bugzilla.redhat.com/show_bug.cgi?id=1961119
         * and https://gitlab.gnome.org/GNOME/gtk/-/issues/4278
         * and https://bugs.openjdk.java.net/browse/JDK-8275494
         *
         * So I decided to use 'xdg-open' on linux. As I couldn't test on other
         * operating systems, I decided to also go via command line there.
         */

        final String[] command;

        switch (OsType.DETECTED) {
        case Windows:
            command = new String[] { "cmd", "/C", "start", uri.toString() };
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
