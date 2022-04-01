package de.r3s6.jarp.build;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import de.r3s6.jarp.JarPresenter;

/**
 * Builder to create a new jar-presenter file with the classes from the current
 * jar file and a given presentation.
 *
 * @author rks
 */
public class JarpBuilder {

    /**
     * Build a new jar-presenter jar.
     *
     * @param targetJar       the new jar to create.
     * @param presentationDir directory with presentation to include in the jar
     * @param initialHtml     name of the initil page to open (instead of
     *                        index.html)
     */
    public void build(final String targetJar, final String presentationDir, final String initialHtml) {

        final File targetFile = new File(targetJar);
        if (targetFile.exists()) {
            System.err.println("Target JAR already exists: " + targetJar);
            System.exit(1);
        }

        if (targetFile.toPath().startsWith(Path.of(presentationDir))) {
            System.err.println("Target JAR and presentation locations overlap.");
            System.exit(1);
        }

        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(targetFile))) {

            // copy classes
            copyJarpClasses(jar);

            // copy presentation
            copyPresentation(jar, presentationDir, JarPresenter.PRESENTATION_DIR);

            if (initialHtml != null) {
                final JarEntry tgtEntry = new JarEntry(JarPresenter.FILEMAP_PATH);
                jar.putNextEntry(tgtEntry);
                final PrintWriter prt = new PrintWriter(jar);
                prt.println("/index.html=/" + initialHtml);
                prt.flush();
                jar.closeEntry();
            }

            // Add Manifest
            final JarEntry tgtEntry = new JarEntry("META-INF/MANIFEST.MF");
            jar.putNextEntry(tgtEntry);
            final PrintWriter prt = new PrintWriter(jar);
            prt.println("Manifest-Version: 1.0");
            prt.println("Created-By: jar-presenter");
            prt.println("Main-Class: de.r3s6.jarp.JarPresenter");
            prt.printf("Jarp-build-date: %s\n", OffsetDateTime.now().toString());
            prt.flush();
            jar.closeEntry();

            System.out.println();
            System.out.println("New Jar created: " + targetFile);

        } catch (final IOException | URISyntaxException e) {
            if (targetFile.exists()) {
                targetFile.delete();
            }
        }

    }

    private void copyJarpClasses(final JarOutputStream jarx) throws IOException, URISyntaxException {
        System.out.println("Copying java classes ...");

        final String jarpJarFile = new File(
                JarpBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();

        try (JarFile jarpJar = new JarFile(jarpJarFile)) {
            final Enumeration<JarEntry> enumEntries = jarpJar.entries();
            while (enumEntries.hasMoreElements()) {
                final JarEntry jarEntry = enumEntries.nextElement();
                if (isJarpCode(jarEntry.getName())) {
                    if (jarEntry.isDirectory()) {
                        final JarEntry tgtEntry = new JarEntry(jarEntry.getName() + "/");
                        jarx.putNextEntry(tgtEntry);
                        jarx.closeEntry();
                    } else {
                        final JarEntry tgtEntry = new JarEntry(jarEntry.getName());
                        jarx.putNextEntry(tgtEntry);
                        try (InputStream in = jarpJar.getInputStream(jarEntry)) {
                            in.transferTo(jarx);
                        } finally {
                            jarx.closeEntry();
                        }
                    }
                }
            }
        }
    }

    private boolean isJarpCode(final String entryName) {

        switch (entryName) {
        case "de":
        case "de/r3s6":
        case "de/r3s6/jarp":
            return true;
        default:
            return entryName.startsWith("de/r3s6/jarp") && !entryName.startsWith("de/r3s6/jarp/maven");
        }
    }

    private void copyPresentation(final JarOutputStream jar, final String sourceDir, final String root)
            throws IOException {
        System.out.println("Copying presentation ...");
        final Path searchRoot = Path.of(sourceDir);

        final FileVisitor<Path> visitor = new PackingVisitor(jar, searchRoot, root);

        Files.walkFileTree(searchRoot, visitor);

    }

    private static final class PackingVisitor extends SimpleFileVisitor<Path> {
        private JarOutputStream mJar;
        private Path mSearchRoot;
        private String mSubdir;

        private PackingVisitor(final JarOutputStream jarOut, final Path searchRoot, final String subdir) {
            mJar = jarOut;
            mSearchRoot = searchRoot;
            mSubdir = subdir;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            final Path jarEntryPath = mSearchRoot.relativize(dir);
            final JarEntry entry = new JarEntry(mSubdir + "/" + jarEntryPath + "/");
            mJar.putNextEntry(entry);
            mJar.closeEntry();

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            final Path jarEntryPath = mSearchRoot.relativize(file);

            final JarEntry entry = new JarEntry(mSubdir + '/' + jarEntryPath);
            mJar.putNextEntry(entry);
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file.toFile()))) {
                final byte[] buffer = new byte[1024 * 1024]; // NOCS: MagicNumber
                while (true) {
                    final int count = in.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    mJar.write(buffer, 0, count);
                }
            } finally {
                mJar.closeEntry();
            }

            return FileVisitResult.CONTINUE;
        }
    }

}
