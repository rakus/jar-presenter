package de.r3s6.jarp.extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Deque;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.r3s6.jarp.JarPresenter;
import de.r3s6.jarp.args.ArgsHandler;
import de.r3s6.jarp.args.ArgsHandler.Argument;
import de.r3s6.jarp.args.CmdLineArgExcpetion;

/**
 * Extract command extracts the presentation from the jar to the current
 * directory in the sub-folder "presentation".
 *
 * @author rks
 */
public final class ExtractCommand {

    private File mTargetDir;

    private ExtractCommand() {
    }

    /**
     * Create a ExtractCommand.
     *
     * @return a new ExtractCommand
     */
    public static ExtractCommand create() {
        return new ExtractCommand();
    }

    /**
     * Processes the command line parameter.
     *
     * @param args the command line parameter.
     * @return this
     */
    public ExtractCommand args(final Deque<String> args) {

        try {
            final ArgsHandler ah = new ArgsHandler(ExtractCommand::showHelp);
            final Argument tgtOpt = ah.addArgument("target-dir");

            ah.handle(args);

            mTargetDir = new File(tgtOpt.getValue());

        } catch (final CmdLineArgExcpetion e) {
            System.err.println(e.getMessage());
            showHelp();
            System.exit(1);
        }

        return this;
    }

    /**
     * Runs the extract command.
     */
    public void execute() {
        try {

            if (!mTargetDir.isDirectory()) {
                if (!mTargetDir.mkdir()) {
                    System.err.println("ERROR: Can't create target dir: " + mTargetDir);
                    System.exit(1);
                }
            }

            final String jarFile = new File(
                    ExtractCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();

            try (JarFile jar = new JarFile(jarFile)) {
                final Enumeration<JarEntry> enumEntries = jar.entries();
                while (enumEntries.hasMoreElements()) {
                    final JarEntry jarEntry = enumEntries.nextElement();
                    if (jarEntry.getName().startsWith(JarPresenter.PRESENTATION_DIR)) {
                        final File tgtFile = new File(mTargetDir, jarEntry.getName());
                        if (jarEntry.isDirectory()) {
                            tgtFile.mkdir();
                            continue;
                        }
                        System.out.println("Extracting to " + tgtFile);
                        try (InputStream is = jar.getInputStream(jarEntry);
                                FileOutputStream fos = new FileOutputStream(tgtFile)) {
                            is.transferTo(fos);
                        }
                    }
                }
            }
        } catch (URISyntaxException | IOException e) {
            System.err.println("Error extracting presentation: " + e);
            System.exit(1);
        }
    }

    /**
     * Shows the command line help for the ExtractCommand.
     */
    public static void showHelp() {

        System.out.println("extract - extract the contained presentation to the given directory");
        System.out.println("      USAGE: java -jar jar-presenter.jar extract <target-dir>");

    }

}
