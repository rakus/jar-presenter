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

import de.r3s6.jarp.args.ArgsHandler;
import de.r3s6.jarp.args.ArgsHandler.Argument;
import de.r3s6.jarp.args.CmdLineArgExcpetion;

public class ExtractCommand {

    private File targetDir;

    private ExtractCommand() {
        // TODO Auto-generated constructor stub
    }

    public static ExtractCommand create() {
        return new ExtractCommand();
    }

    public ExtractCommand args(final Deque<String> args) {

        try {
            final ArgsHandler ah = new ArgsHandler();
            final Argument tgtOpt = ah.addArgument("target-dir");

            ah.handle(args);

            targetDir = new File(tgtOpt.getValue());

        } catch (final CmdLineArgExcpetion e) {
            System.err.println(e.getMessage());
            show_help();
            System.exit(1);
        }

        return this;
    }

    public void execute() {
        try {

            if (!targetDir.isDirectory()) {
                if (!targetDir.mkdir()) {
                    System.err.println("ERROR: Can't create target dir: " + targetDir);
                    System.exit(1);
                }
            }

            final String jarFile = new File(
                    ExtractCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();

            try (JarFile jar = new JarFile(jarFile)) {
                final Enumeration<JarEntry> enumEntries = jar.entries();
                while (enumEntries.hasMoreElements()) {
                    final JarEntry jarEntry = enumEntries.nextElement();
                    if (jarEntry.getName().startsWith("presentation")) {
                        final File tgtFile = new File(targetDir, jarEntry.getName());
                        if (jarEntry.isDirectory()) {
                            tgtFile.mkdir();
                            continue;
                        }
                        System.out.println("Extracting to " + tgtFile);
                        try (final InputStream is = jar.getInputStream(jarEntry);
                                final FileOutputStream fos = new FileOutputStream(tgtFile)) {
                            while (is.available() > 0) { // write contents of 'is' to 'fos'
                                fos.write(is.read());
                            }
                        }
                    }
                }
            }
        } catch (URISyntaxException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(11);
        }
    }

    public static void show_help() {

        System.out.println("extract - extract the contained presentation to the given directory");
        System.out.println("      USAGE: java -jar jar-presenter.jar extract <target-dir>");

    }

}
