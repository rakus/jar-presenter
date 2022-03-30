package de.r3s6.jarp.build;

import java.util.Deque;

import de.r3s6.jarp.args.ArgsHandler;
import de.r3s6.jarp.args.CmdLineArgExcpetion;

/**
 * Command to build a new jar-presenter file with the classes from the current
 * jar file and a given presentation.
 *
 * @author rks
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
        // -i index-html-name.html
        // <jar-name>
        // <presentation-dir>

        try {
            final ArgsHandler ah = new ArgsHandler(BuildCommand::showHelp);

            final var idxOpt = ah.addValueOption('i');
            final var jarOpt = ah.addArgument("new-jar-name");
            final var dirOpt = ah.addArgument("presentation-dir");

            ah.handle(args);

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
        new JarpBuilder().build(mTargetJarName, mSrcDir, mIndexFile);
    }

    /**
     * Shows the command line help for the BuildCommand.
     */
    public static void showHelp() {

        System.out.println("build - build a NEW presentation jar for given presentation");
        System.out.println("      USAGE: java -jar jar-presenter.jar build <new-jar-name> <presentation-dir>");
        System.out.println("        new-jar-name");
        System.out.println("                 name of the new jar to create");
        System.out.println("        presentation-dir");
        System.out.println("                 directory of the presentation to include in new jar");

    }
}