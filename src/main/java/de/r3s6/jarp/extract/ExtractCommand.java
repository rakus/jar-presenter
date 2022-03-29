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

public class ExtractCommand {

    private File targetDir;

    private ExtractCommand() {
        // TODO Auto-generated constructor stub
    }

    public static ExtractCommand create() {
        return new ExtractCommand();
    }

    public ExtractCommand args(final Deque<String> args) {
        int rc = -1;
        String targetDirName;
        if (args.size() != 1) {
            show_help();
            rc = 1;
        } else {
            final String arg = args.poll();
            if ("--help".equals(arg)) {
                show_help();
                rc = 0;
            } else {
                targetDirName = arg;
                targetDir = new File(targetDirName);
            }
        }

        if (rc >= 0) {
            System.exit(rc);
        } else {
            if (!targetDir.isDirectory()) {
                targetDir.mkdirs();
            }
        }

        return this;
    }

    public void execute() {
        try {
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
        System.out.println("java -jar jar-presenter extract <target-dir>");
        System.out.println("  Extract the contained presentation to the given directory.");

    }

}
