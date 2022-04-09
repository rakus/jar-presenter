/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.build;

import java.io.IOException;
import java.util.Deque;

import de.r3s6.jarp.args.ArgsParser;
import de.r3s6.jarp.args.CmdLineArgExcpetion;

/**
 * Command to build a new jar-presenter file with the classes from the current
 * jar file and a given presentation.
 *
 * @author Ralf Schandl
 */
public final class BuildCommand {

    private String mTargetJarName;
    private String mSrcDir;
    private String mIndexFile;

    private BuildCommand() {
    }

    /**
     * Creates a BuildCommand.
     *
     * @return the new BuildCommand
     */
    public static BuildCommand create() {
        return new BuildCommand();
    }

    /**
     * Parses the command-specific options.
     *
     * @param args the command line options
     * @return this.
     */
    public BuildCommand args(final Deque<String> args) {

        try {
            final ArgsParser ah = new ArgsParser(BuildCommand::showHelp);

            final var idxOpt = ah.addValueOption('i');
            final var jarOpt = ah.addArgument("new-jar-name");
            final var dirOpt = ah.addArgument("presentation-dir");

            ah.parse(args);

            this.mIndexFile = idxOpt.getValue();
            this.mTargetJarName = jarOpt.getValue();
            this.mSrcDir = dirOpt.getValue();
        } catch (final CmdLineArgExcpetion e) {
            System.err.println(e.getMessage());
            showHelp();
            System.exit(1);
        }

        return this;
    }

    /**
     * Actually executes the command.
     */
    public void execute() {
        try {
            new JarpBuilder().build(mTargetJarName, mSrcDir, mIndexFile);
        } catch (final IOException | IllegalArgumentException e) {
            System.err.println("ERROR: Creating jar failed: " + e);
            System.exit(1);
        }
    }

    /**
     * Shows the command line help for the BuildCommand.
     */
    public static void showHelp() {

        System.out.println("build - build a NEW presentation jar for given presentation");
        System.out.println(
                "      USAGE: java -jar jar-presenter.jar build [-i <start-page>] <new-jar-name> <presentation-dir>");
        System.out.println("        -i <start-page>");
        System.out.println("                 defines the start page of the presentation to be used instead");
        System.out.println("                 of index.html");
        System.out.println("        new-jar-name");
        System.out.println("                 name of the new jar to create");
        System.out.println("        presentation-dir");
        System.out.println("                 directory of the presentation to include in new jar");

    }
}
