package de.r3s6.jarp;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import de.r3s6.jarp.extract.ExtractCommand;
import de.r3s6.jarp.server.ServerCommand;

public class Main {

    public static void main(final String[] args) throws IOException {

        if (args.length == 0) {
            ServerCommand.create().serve();
        } else {
            final Deque<String> argList = new ArrayDeque<>(Arrays.asList(args));
            if ("--help".equals(argList.peekFirst())) {
                showHelp();
                System.exit(0);
            } else if (argList.peekFirst().startsWith("-") || argList.peekFirst().matches("^\\d*$")) {
                ServerCommand.create().args(argList).serve();
            }
            final String command = argList.poll();
            switch (command) {
            case "server":
                ServerCommand.create().args(argList).serve();
                break;
            case "extract":
                ExtractCommand.create().args(argList).execute();
                break;
            case "pack":
                System.err.println("NOT YET IMPLEMENTED");
                System.exit(1);
                break;
            case "help":
                showHelp();
                System.exit(0);
                break;

            default:
                System.err.println("ERROR: Unknown command: " + command);
                showHelp();
                System.exit(1);
                break;
            }
        }

    }

    private static void showHelp() {

        System.out.println();
        ServerCommand.show_help();
        System.out.println();
        ExtractCommand.show_help();
        System.out.println();
        System.out.println("If no command is given, \"server\" is assumed.");
        System.out.println();

    }

}
