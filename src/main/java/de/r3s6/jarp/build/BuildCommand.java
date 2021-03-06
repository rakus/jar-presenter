/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.build;

import java.io.IOException;
import java.util.List;

import de.r3s6.jarp.args.ArgsParser;
import de.r3s6.jarp.args.ArgsParser.Argument;
import de.r3s6.jarp.args.ArgsParser.Flag;
import de.r3s6.jarp.args.ArgsParser.ValueOption;
import de.r3s6.jarp.args.CmdLineArgException;

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
    /** Whether to overwrite an existing jar. */
    private boolean mForce;

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
     * Shows the command line help for the BuildCommand.
     */
    public static void showHelp() {

        System.out.println("build - build a NEW presentation jar for given presentation");
        System.out.println(
                "      USAGE: java -jar jar-presenter.jar build [-f] [-i <start-page>] <new-jar-name> <presentation-dir>");
        System.out.println("        -f       overwrite existing jar");
        System.out.println("        -i <start-page>");
        System.out.println("                 defines the start page of the presentation to be used instead");
        System.out.println("                 of index.html");
        System.out.println("        new-jar-name");
        System.out.println("                 name of the new jar to create");
        System.out.println("        presentation-dir");
        System.out.println("                 directory of the presentation to include in new jar");
    }

    /**
     * Actually executes the command.
     *
     * @param argList the command line options
     */
    public void execute(final List<String> argList) {
        handleArgs(argList);
        try {
            new JarpBuilder().build(mTargetJarName, mSrcDir, mIndexFile, mForce);
        } catch (final IOException | IllegalArgumentException e) {
            System.err.println("ERROR: Creating jar failed: " + e);
            System.exit(1);
        }
    }

    /**
     * Parses the command-specific options.
     *
     * @param argList the command line options
     */
    private void handleArgs(final List<String> argList) {

        try {
            final ArgsParser ah = new ArgsParser(BuildCommand::showHelp);

            final ValueOption idxOpt = ah.addValueOption('i');
            final Flag forceOpt = ah.addFlag('f');
            final Argument jarOpt = ah.addArgument("new-jar-name");
            final Argument dirOpt = ah.addArgument("presentation-dir");

            ah.parse(argList);

            mIndexFile = idxOpt.getValue();
            mTargetJarName = jarOpt.getValue();
            mSrcDir = dirOpt.getValue();
            mForce = forceOpt.getValue();

        } catch (final CmdLineArgException e) {
            System.err.println(e.getMessage());
            showHelp();
            System.exit(1);
        }

    }

}
