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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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
 * @author rks
 */
public class JarpBuilder {

    /**
     * Build a new jar-presenter jar.
     *
     * @param targetJar       the new jar to create.
     * @param presentationDir directory with presentation to include in the jar
     * @param initialHtml     name of the inital page to open (instead of
     *                        index.html)
     * @throws IOException              on IO problems
     * @throws IllegalArgumentException if expected files don't exist
     */
    public void build(final String targetJar, final String presentationDir, final String initialHtml)
            throws IOException {

        final File targetFile = new File(targetJar);
        if (targetFile.exists()) {
            throw new IllegalArgumentException("Target JAR already exists: " + targetJar);
        }

        if (targetFile.toPath().startsWith(Path.of(presentationDir))) {
            throw new IllegalArgumentException("Target JAR and presentation locations overlap.");
        }

        verifyIndexHtml(presentationDir, initialHtml);

        final Manifest manifest = createManifest();

        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(targetFile), manifest)) {

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

            System.out.println();
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

    private void verifyIndexHtml(final String presentationDir, final String initialHtml) throws IOException {

        final Path filemapFile = Paths.get(presentationDir, JarPresenter.FILEMAP_BASENAME);
        Path startPage = Paths.get(presentationDir, "index.html");

        // if initialHtml is set, check if it exists
        if (initialHtml != null) {
            final Path iniHtml = Paths.get(presentationDir, initialHtml);
            if (Files.exists(filemapFile)) {
                throw new IllegalArgumentException(
                        "Filemap file exists and initial HTML given. That doesn't make sense.");
            }
            startPage = iniHtml;
        } else {
            if (Files.exists(filemapFile)) {
                final Map<String, String> filemap = readFilemap(filemapFile.toFile());
                if (filemap.containsKey("/index.html")) {
                    startPage = Path.of(presentationDir, filemap.get("/index.html").substring(1));
                }
            }
        }

        if (!Files.exists(startPage)) {
            throw new FileNotFoundException(startPage.toString());
        }
    }

    private Map<String, String> readFilemap(final File filemapFile) throws IOException {
        try (InputStream in = new FileInputStream(filemapFile)) {
            final Properties props = new Properties();
            props.load(in);
            final Map<String, String> map = new HashMap<>();
            for (final Map.Entry<Object, Object> entry : props.entrySet()) {
                map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            return Collections.unmodifiableMap(map);
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
        } catch (final URISyntaxException e) {
            // I think that this should never happen.
            throw new RuntimeException("Unexpected exception: " + e, e);
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
