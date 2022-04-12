/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.r3s6.jarp.JarPresenter;
import de.r3s6.jarp.args.ArgsParser;
import de.r3s6.jarp.args.ArgsParser.Argument;
import de.r3s6.jarp.args.ArgsParser.Flag;
import de.r3s6.jarp.args.CmdLineArgException;

/**
 * Extract command extracts the presentation from the jar to a given directory
 * in the sub-folder "presentation".
 *
 * @author Ralf Schandl
 */
public final class ExtractCommand {

    private File mTargetDir;

    /**
     * Whether to overwrite existing files ('-f').
     */
    private boolean mForce;

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
     * Shows the command line help for the ExtractCommand.
     */
    public static void showHelp() {

        System.out.println("extract - extract the contained presentation to the given directory");
        System.out.println("      USAGE: java -jar jar-presenter.jar extract [-f] <target-dir>");
        System.out.println("        -f   Overwrite existing files.");

    }

    /**
     * Runs the extract command.
     *
     * @param argList the command line parameter.
     */
    public void execute(final List<String> argList) {
        handleArgs(argList);

        try {

            final String jarFile = new File(
                    ExtractCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();

            try (JarFile check = new JarFile(jarFile)) {
                if (check.getEntry(JarPresenter.PRESENTATION_DIR) == null) {
                    System.err.println("This jar doesn't contain a presentation. Nothing to do.");
                    return;
                }
            }

            if (!mTargetDir.isDirectory()) {
                if (!mTargetDir.mkdir()) {
                    System.err.println("ERROR: Can't create target dir: " + mTargetDir);
                    System.exit(1);
                }
            }

            final String preziPrefix = JarPresenter.PRESENTATION_DIR + "/";
            final int preziPrefixLength = preziPrefix.length();

            try (JarFile jar = new JarFile(jarFile)) {
                final Enumeration<JarEntry> enumEntries = jar.entries();
                while (enumEntries.hasMoreElements()) {
                    final JarEntry jarEntry = enumEntries.nextElement();
                    if (jarEntry.getName().startsWith(preziPrefix)) {
                        final String tgtFileName = jarEntry.getName().substring(preziPrefixLength);
                        final File tgtFile = new File(mTargetDir, tgtFileName);
                        if (jarEntry.isDirectory()) {
                            tgtFile.mkdirs();
                            continue;
                        }
                        System.out.println("Extracting to " + tgtFile);
                        if (tgtFile.exists() && !mForce) {
                            System.err.println("File exists -- use '-f' to overwrite: " + tgtFile);
                            System.exit(1);
                        }
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
     * Processes the command line parameter.
     *
     * @param argList the command line parameter.
     */
    private void handleArgs(final List<String> argList) {

        try {
            final ArgsParser ah = new ArgsParser(ExtractCommand::showHelp);
            final Flag force = ah.addFlag('f');
            final Argument tgtOpt = ah.addArgument("target-dir");

            ah.parse(argList);

            mTargetDir = new File(tgtOpt.getValue());
            mForce = force.getValue();

        } catch (final CmdLineArgException e) {
            System.err.println(e.getMessage());
            showHelp();
            System.exit(1);
        }
    }

}
