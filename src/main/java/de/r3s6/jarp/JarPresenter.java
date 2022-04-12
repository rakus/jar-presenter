/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.r3s6.jarp.build.BuildCommand;
import de.r3s6.jarp.extract.ExtractCommand;
import de.r3s6.jarp.server.ServerCommand;

/**
 * The jar-presenter main class.
 *
 * @author Ralf Schandl
 */
public final class JarPresenter {

    /** Resource folder of the presentation. */
    public static final String PRESENTATION_DIR = "presentation";
    /**
     * Name of the file that helps mapping request file names to actual file names.
     * Main use: Using a different file than index.html as start page.
     */
    public static final String FILEMAP_BASENAME = "jarp-filemap.properties";
    /**
     * Full path to file map file. See {@link #FILEMAP_BASENAME}.
     */
    public static final String FILEMAP_PATH = PRESENTATION_DIR + "/" + FILEMAP_BASENAME;

    private JarPresenter() {
    }

    /**
     * Main method.
     *
     * @param args command line arguments. Supported argument depend on command.
     */
    public static void main(final String[] args) {

        if (args.length == 0) {
            ServerCommand.create().execute(Collections.emptyList());
        } else {
            if ("--help".equals(args[0])) {
                showHelp();
                System.exit(0);
            } else if (args[0].startsWith("-") || args[0].matches("^\\d*$")) {
                // No command -> default is "server"
                ServerCommand.create().execute(Arrays.asList(args));
            } else {
                final List<String> argList = new ArrayList<>(Arrays.asList(args));
                final String command = argList.remove(0);
                switch (command) {
                case "server":
                    ServerCommand.create().execute(argList);
                    break;
                case "extract":
                    ExtractCommand.create().execute(argList);
                    break;
                case "build":
                    BuildCommand.create().execute(argList);
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

    }

    private static void showHelp() {

        System.out.println();
        ServerCommand.showHelp();
        System.out.println();
        ExtractCommand.showHelp();
        System.out.println();
        BuildCommand.showHelp();
        System.out.println();
        System.out.println("If no command is given, \"server\" is assumed.");
        System.out.println();

    }

}
