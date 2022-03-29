package de.r3s6.jarp;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import de.r3s6.jarp.extract.ExtractCommand;
import de.r3s6.jarp.serve.ServeCommand;

public class Main {

    public static void main(final String[] args) throws IOException {

        if (args.length == 0) {
            ServeCommand.create().serve();
        } else {
            final Deque<String> argList = new ArrayDeque<>(Arrays.asList(args));
            if ("--help".equals(argList.peekFirst())) {
                showHelp();
                System.exit(0);
            } else if (argList.peekFirst().startsWith("-")) {
                ServeCommand.create().args(argList).serve();
            }
            final String command = argList.poll();
            switch (command) {
            case "serve":
                ServeCommand.create().args(argList).serve();
                break;
            case "extract":
                ExtractCommand.create().args(argList).execute();
                break;
            case "pack":
                System.err.println("NOT YET IMPLEMENTED");
                System.exit(1);
                break;

            default:
                System.err.println("ERROR: Unknown command: " + command);
                System.exit(1);
                break;
            }
        }

    }

    private static void showHelp() {

        System.out.println();
        ServeCommand.show_help();
        System.out.println();
        ExtractCommand.show_help();
        System.out.println();
    }

}
