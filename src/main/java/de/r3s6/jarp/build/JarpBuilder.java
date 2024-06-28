/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.build;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import de.r3s6.jarp.JarPresenter;

/**
 * Builder to create a new jar-presenter file with the classes from the current
 * jar file and a given presentation.
 *
 * @author Ralf Schandl
 */
public class JarpBuilder {

    /**
     * Build a new jar-presenter jar.
     *
     * @param targetJar       the new jar to create.
     * @param presentationDir directory with presentation to include in the jar
     * @param title           title of the presentation
     * @param initialHtml     name of the initial page to open (instead of
     *                        index.html)
     * @param force           overwrite a existing jar
     * @throws IOException              on IO problems
     * @throws IllegalArgumentException if expected files don't exist
     */
    public void build(final String targetJar, final String presentationDir, final String title,
            final String initialHtml, final boolean force)
            throws IOException {

        final File targetFile = new File(targetJar);
        if (targetFile.exists()) {
            if (!force) {
                throw new IllegalArgumentException("Target JAR already exists: " + targetJar);
            } else {
                targetFile.delete();
            }
        }

        if (targetFile.toPath().startsWith(Path.of(presentationDir))) {
            throw new IllegalArgumentException("Target JAR and presentation locations overlap.");
        }

        final Properties metadata = createMetaData(presentationDir, title, initialHtml);

        final Manifest manifest = createManifest();

        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(targetFile), manifest)) {

            // copy classes
            copyJarpClasses(jar);

            // copy presentation
            copyPresentation(jar, presentationDir, JarPresenter.PRESENTATION_DIR);

            final JarEntry tgtEntry = new JarEntry(JarPresenter.METADATA_PATH);
            jar.putNextEntry(tgtEntry);
            final PrintWriter prt = new PrintWriter(jar);
            metadata.store(prt, null);
            prt.flush();
            jar.closeEntry();

            System.out.println("New Jar created: " + targetFile);

        } catch (final IOException e) {
            if (targetFile.exists()) {
                targetFile.delete();
            }
            throw e;
        }

    }

    private Manifest createManifest() {

        final Manifest mf = new Manifest();
        final Attributes attr = mf.getMainAttributes();
        attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attr.put(Attributes.Name.IMPLEMENTATION_TITLE, JarPresenter.class.getPackage().getImplementationTitle());
        attr.put(Attributes.Name.IMPLEMENTATION_VERSION, JarPresenter.class.getPackage().getImplementationVersion());
        attr.put(Attributes.Name.MAIN_CLASS, JarPresenter.class.getName());

        attr.put(new Name("Created-By"), "jar-presenter");
        attr.put(new Name("Jarp-Build-Date"), OffsetDateTime.now().toString());

        return mf;
    }

    private Properties createMetaData(final String presentationDir, final String title, final String initialHtml)
            throws IOException {

        final Path metadataFile = Paths.get(presentationDir, JarPresenter.METADATA_BASENAME);

        final Properties props;
        if (Files.exists(metadataFile)) {
            props = loadMetadataProps(metadataFile);
            System.out.println("Metadata file found: " + props);
        } else {
            props = new Properties();
        }

        if (title != null) {
            props.setProperty(JarPresenter.PROP_TITLE, title);
        }
        if (initialHtml != null) {
            props.setProperty(JarPresenter.PROP_STARTPAGE, initialHtml);
        } else if (!props.containsKey(JarPresenter.PROP_STARTPAGE)) {
            final String startPage = getStartPage(presentationDir);
            if (startPage != null) {
                props.setProperty(JarPresenter.PROP_STARTPAGE, startPage);
            } else {
                props.setProperty(JarPresenter.PROP_STARTPAGE, "/index.html");
            }
        }

        final Path startPage = Paths.get(presentationDir, props.getProperty(JarPresenter.PROP_STARTPAGE));

        if (!Files.exists(startPage)) {
            throw new FileNotFoundException(startPage.toString());
        }

        System.out.println("Metadata: " + props);

        return props;
    }

    private String getStartPage(final String presentationDir) {

        final File[] files = Paths.get(presentationDir).toFile()
                .listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".html"));

        if (files.length == 1) {
            return files[0].getName();
        } else if (files.length == 0) {
            System.err.println("No html page found in " + presentationDir);
            System.exit(1);
        } else if (files.length > 1) {
            for (final File file : files) {
                if ("index.html".equals(file.getName())) {
                    return file.getName();
                }
            }
            // No file 'index.html'
            System.err.println("Multiple possible start pages found. Use '-s' to name one");
            for (final File file : files) {
                System.err.println(" - " + file.getName());
            }
            System.exit(1);
        }
        return null;
    }

    private Properties loadMetadataProps(final Path metadataFile) throws IOException {
        try (InputStream in = new FileInputStream(metadataFile.toFile())) {
            final Properties props = new Properties();
            props.load(in);
            return props;
        }
    }

    private void copyJarpClasses(final JarOutputStream jarx) throws IOException {
        System.out.println("Copying java classes ...");

        try {
            final String jarpJarFile = new File(
                    JarpBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();

            try (JarFile jarpJar = new JarFile(jarpJarFile)) {
                final Enumeration<JarEntry> enumEntries = jarpJar.entries();
                while (enumEntries.hasMoreElements()) {
                    final JarEntry jarEntry = enumEntries.nextElement();
                    if (jarEntry.getName().startsWith("de/r3s6/jarp")) {
                        if (jarEntry.isDirectory()) {
                            // no /-suffix needed, as it is already there
                            final JarEntry tgtEntry = new JarEntry(jarEntry.getName());
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
        } catch (final URISyntaxException e) {
            // I think that this should never happen.
            throw new RuntimeException("Unexpected exception: " + e, e);
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
            final String entryPath;
            if (mSearchRoot.equals(dir)) {
                entryPath = mSubdir + "/";
            } else {
                final Path jarEntryPath = mSearchRoot.relativize(dir);
                entryPath = mSubdir + "/" + jarEntryPath + "/";
            }
            final JarEntry entry = new JarEntry(entryPath);
            mJar.putNextEntry(entry);
            mJar.closeEntry();

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            final Path jarEntryPath = mSearchRoot.relativize(file);

            if (!JarPresenter.METADATA_BASENAME.equals(jarEntryPath.toString())) {
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
            }

            return FileVisitResult.CONTINUE;
        }
    }

}
