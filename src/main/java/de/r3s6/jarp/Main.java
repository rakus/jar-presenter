package de.r3s6.jarp;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import de.r3s6.jarp.serve.HttpServerchen;

public class Main {

    public static void main(String[] args) throws IOException {
        serve();
    }

    private static void serve() {
        try (HttpServerchen srv = new HttpServerchen()) {
            int port = srv.getPort();
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        srv.serve();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            Thread serverThread = new Thread(r);
            serverThread.start();

            System.out.println("Serving on http://localhost:" + port);

            openBrowser(port);

            try {
                serverThread.join();
            } catch (InterruptedException e) {
                // IGNORED
            }
        } catch (IOException e) {
            System.err.println("Can't start HttpServerchen: " + e.toString());
        }

    }

    private static void openBrowser(int port) {

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

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectOutput(Redirect.DISCARD);
        processBuilder.redirectError(Redirect.DISCARD);
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
