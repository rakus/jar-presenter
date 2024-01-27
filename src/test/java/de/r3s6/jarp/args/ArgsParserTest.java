package de.r3s6.jarp.args;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.r3s6.jarp.args.ArgsParser.Argument;
import de.r3s6.jarp.args.ArgsParser.CmdLineArgException;
import de.r3s6.jarp.args.ArgsParser.Counter;
import de.r3s6.jarp.args.ArgsParser.Flag;
import de.r3s6.jarp.args.ArgsParser.ValueOption;

class ArgsParserTest {

    private static int sHelpCounter = 0;

    public static void showHelp() {
        sHelpCounter++;
    }

    @BeforeEach
    void reset() {
        sHelpCounter = 0;
    }

    /**
     * This test tests --help and needs the SecurityManager to prevent
     * {@link System#exit}. The SecurityManager is deprecated in Java17 and
     * {@link System#setSecurityManager(SecurityManager)} throws a
     * UnsupportedOperationException in Java18.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_11, max = JRE.JAVA_17)
    void testHelp() throws CmdLineArgException {
        final SecurityManager orgSecurityManager = System.getSecurityManager();
        try {
            System.setSecurityManager(new NoExitSecurityManager());

            final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);

            final ExitCodeException ex = assertThrows(ExitCodeException.class,
                    () -> aj.parse(new String[] { "--help" }));

            assertEquals(1, sHelpCounter);
            assertEquals(0, ex.getExitCode());

        } finally {
            System.setSecurityManager(orgSecurityManager);
        }
    }

    @Test
    void testShortOption() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');
        final ValueOption indexOpt = aj.addValueOption('i');
        final Argument jarArg = aj.addRequiredArgument("jar");
        final Argument dirArg = aj.addRequiredArgument("dir");
        final List<String> optArgs = new ArrayList<>();
        aj.optionalArgumentList(optArgs);

        aj.parse(Arrays.asList("-i", "demo.html", "jarp.jar", "directory"));

        assertEquals(false, debugOpt.getValue());
        assertEquals("demo.html", indexOpt.getValue());
        assertEquals("jarp.jar", jarArg.getValue());
        assertEquals("directory", dirArg.getValue());
        assertTrue(optArgs.isEmpty());

    }

    @Test
    void testSingleDashArgument() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');
        final List<String> optArgs = new ArrayList<>();
        aj.optionalArgumentList(optArgs);

        aj.parse(Arrays.asList("-d", "-", "demo.html"));

        assertEquals(true, debugOpt.getValue());
        assertFalse(optArgs.isEmpty());
        assertEquals("-", optArgs.get(0));
        assertEquals("demo.html", optArgs.get(1));
    }

    @Test
    void testLongOption() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d', "debug");
        final ValueOption indexOpt = aj.addValueOption('i', "index");
        final Argument jarArg = aj.addRequiredArgument("jar");
        final Argument dirArg = aj.addRequiredArgument("dir");
        final List<String> optArgs = new ArrayList<>();
        aj.optionalArgumentList(optArgs);

        aj.parse(Arrays.asList("--index", "demo.html", "jarp.jar", "directory"));

        assertEquals(false, debugOpt.getValue());
        assertEquals("demo.html", indexOpt.getValue());
        assertEquals("jarp.jar", jarArg.getValue());
        assertEquals("directory", dirArg.getValue());
        assertTrue(optArgs.isEmpty());

    }

    @Test
    void testLongOptionCombined() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d', "debug");
        final ValueOption indexOpt = aj.addValueOption('i', "index");
        final Argument jarArg = aj.addRequiredArgument("jar");
        final Argument dirArg = aj.addRequiredArgument("dir");
        final List<String> optArgs = new ArrayList<>();
        aj.optionalArgumentList(optArgs);

        aj.parse(Arrays.asList("--index=demo.html", "jarp.jar", "directory"));

        assertEquals(false, debugOpt.getValue());
        assertEquals("demo.html", indexOpt.getValue());
        assertEquals("jarp.jar", jarArg.getValue());
        assertEquals("directory", dirArg.getValue());
        assertTrue(optArgs.isEmpty());

    }

    @Test
    void testLongOptionCombinedEmptyValue() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final ValueOption indexOpt = aj.addValueOption('i', "index");

        aj.parse(Arrays.asList("--index="));

        assertEquals("", indexOpt.getValue());
    }

    @Test
    void testLongOptionCombinedEmptyName() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class,
                () -> aj.parse(Arrays.asList("--=hello")));

        assertEquals("Superfluous argument in \"--=hello\"", ex.getMessage());
    }

    @Test
    void testOptionalArgs() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');
        final ValueOption indexOpt = aj.addValueOption('i');
        final Argument jarArg = aj.addRequiredArgument("jar");
        final Argument dirArg = aj.addRequiredArgument("dir");
        final List<String> optArgs = new ArrayList<>();
        aj.optionalArgumentList(optArgs);

        aj.parse(Arrays.asList("-i", "demo.html", "jarp.jar", "directory", "opt1", "opt2"));

        assertEquals(false, debugOpt.getValue());
        assertEquals("demo.html", indexOpt.getValue());
        assertEquals("jarp.jar", jarArg.getValue());
        assertEquals("directory", dirArg.getValue());
        assertFalse(optArgs.isEmpty());
        assertEquals("opt1", optArgs.get(0));
        assertEquals("opt2", optArgs.get(1));

    }

    @Test
    void testFlagShort() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d', "debug");

        aj.parse(Arrays.asList("-d"));

        assertEquals(true, debugOpt.getValue());
    }

    @Test
    void testFlagLong() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d', "debug");

        aj.parse(Arrays.asList("--debug"));

        assertEquals(true, debugOpt.getValue());
    }

    @Test
    void testParseStringArray() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d', "debug");

        aj.parse(new String[] { "--debug" });

        assertEquals(true, debugOpt.getValue());
    }

    @ParameterizedTest
    @CsvSource({ "-d,-d", "-d,--debug", "--debug,-d", "--debug,--debug" })
    void testDuplicateFlagException(final String first, final String second) {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d', "debug");

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class,
                () -> aj.parse(Arrays.asList(first, second)));

        assertEquals("Duplicate option: -d/--debug", ex.getMessage());
    }

    @Test
    void testUnknownOptionException() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d');

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class,
                () -> aj.parse(Arrays.asList("-d", "-X")));

        assertEquals("Unknown option: -X", ex.getMessage());
    }

    @Test
    void testUnknownLongOptionException() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d');

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class,
                () -> aj.parse(Arrays.asList("-d", "--unknown")));

        assertEquals("Unknown option: --unknown", ex.getMessage());
    }

    @Test
    void testDuplicateOptionDefinitionException() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d');

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> aj.addFlag('d'));

        assertEquals("Option '-d' already used", ex.getMessage());
    }

    @Test
    void testDuplicateLongOptionDefinitionException() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d', "debug");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aj.addFlag((char) 0, "debug"));

        assertEquals("Option '--debug' already used", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(chars = { ' ', '-', '\t', '\n' })
    void testIllegalShortOptionDefinition(final char optChar) {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aj.addFlag(optChar));

        assertEquals("Invalid short option '" + optChar + "'", ex.getMessage());
    }

    @Test
    void testIllegalOptionDefinitionNulException() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aj.addFlag((char) 0));

        assertEquals("Either short and/or long option needed", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "-long", "l", " long", "lo ng", "long ", "lo\tng", "long=", "long=true" })
    void testIllegalLongOptionDefinition(final String longOpt) {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aj.addFlag('\0', longOpt));

        assertEquals("Invalid long option '" + longOpt + "'", ex.getMessage());
    }

    @Test
    void testCounter() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Counter countOpt = aj.addCounter('v');

        aj.parse(Arrays.asList("-v", "-vvv"));

        assertEquals(4, countOpt.getValue());
    }

    @Test
    void testValueOption() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final ValueOption f1 = aj.addValueOption('f');
        final ValueOption f2 = aj.addValueOption('o');

        aj.parse(Arrays.asList("-f", "file1", "-ofile2"));

        assertEquals("file1", f1.getValue());
        assertEquals("file2", f2.getValue());
    }

    @Test
    void testValueOptionLong() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final ValueOption f1 = aj.addValueOption('f', "file");
        final ValueOption f2 = aj.addValueOption('o', "output");

        aj.parse(Arrays.asList("--file=file1", "--output", "file2"));

        assertEquals("file1", f1.getValue());
        assertEquals("file2", f2.getValue());
    }

    @Test
    void testUnusedValue() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d', "debug");

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class,
                () -> aj.parse(Arrays.asList("--debug=file1")));

        assertEquals("Superfluous argument in \"--debug=file1\"", ex.getMessage());
    }

    @Test
    void testDuplicateValueOptionException() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addValueOption('f');

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class,
                () -> aj.parse(Arrays.asList("-f", "file", "-f", "other file")));

        assertEquals("Duplicate option: -f", ex.getMessage());

    }

    @Test
    void testDuplicateValueOptionExceptionShortLong() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addValueOption('f', "file");

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class,
                () -> aj.parse(Arrays.asList("-f", "file", "--file", "other file")));

        assertEquals("Duplicate option: -f/--file", ex.getMessage());

    }

    @Test
    void testDuplicateValueOptionExceptionShortLongEqual() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addValueOption('f', "file");

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class,
                () -> aj.parse(Arrays.asList("-f", "file", "--file=other file")));

        assertEquals("Duplicate option: -f/--file", ex.getMessage());

    }

    @Test
    void testCombinedOptions() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');
        final Counter cntOpt = aj.addCounter('v');
        final ValueOption valueOpt = aj.addValueOption('f');

        aj.parse(Arrays.asList("-dvvvffile-name"));

        assertEquals(true, debugOpt.getValue());
        assertEquals(3, cntOpt.getValue());
        assertEquals("file-name", valueOpt.getValue());
    }

    @Test
    void testOptionalArguments() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final List<String> optArgs = new ArrayList<>();
        aj.optionalArgumentList(optArgs);

        aj.parse(Arrays.asList("--", "-i", "demo.html"));

        assertEquals(2, optArgs.size());
        assertEquals("-i", optArgs.get(0));
        assertEquals("demo.html", optArgs.get(1));

    }

    @Test
    void testUnexpectedOptionalArguments() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d');
        aj.addRequiredArgument("jar");

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class,
                () -> aj.parse(Arrays.asList("-d", "jarp.jar", "superfluous")));

        assertEquals("Superfluous arguments starting with: superfluous", ex.getMessage());
    }

    @Test
    void testMissingOneArgument() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d');
        aj.addRequiredArgument("jar");

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class, () -> aj.parse(Arrays.asList("-d")));

        assertEquals("Missing argument: jar", ex.getMessage());
    }

    @Test
    void testMissingMultipleArgument() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d');
        aj.addRequiredArgument("jar");
        aj.addRequiredArgument("dir");

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class, () -> aj.parse(Arrays.asList("-d")));

        assertEquals("Missing arguments: jar, dir", ex.getMessage());
    }

    @Test
    void testMissingOptionValue() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addValueOption('o');

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class, () -> aj.parse(Arrays.asList("-o")));

        assertEquals("Missing value for option: -o", ex.getMessage());
    }

    @Test
    void testMissingOptionValueLong() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addValueOption('o', "output");

        final CmdLineArgException ex = assertThrows(CmdLineArgException.class,
                () -> aj.parse(Arrays.asList("--output")));

        assertEquals("Missing value for option: --output", ex.getMessage());
    }

    /**
     * Tests programmers error: Not providing help method.
     */
    @Test
    void testHelpMethodNull() {

        final NullPointerException ex = assertThrows(NullPointerException.class, () -> new ArgsParser(null));

        assertEquals("Parameter helpMethod is null", ex.getMessage());
    }

    /**
     * Tests programmers error: Providing null instead of argument list.
     */
    @Test
    void testArgsNullList() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addValueOption('o', "output");

        final NullPointerException ex = assertThrows(NullPointerException.class,
                () -> aj.parse((List<String>) null));

        assertEquals("argsList must not be null", ex.getMessage());
    }

    /**
     * Tests programmers error: Providing null instead of argument array.
     */
    @Test
    void testArgsNullArray() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addValueOption('o', "output");

        final NullPointerException ex = assertThrows(NullPointerException.class,
                () -> aj.parse((String[]) null));

        assertEquals("args must not be null", ex.getMessage());
    }

    /**
     * Tests programmers error: Providing arguments with null element.
     */
    @Test
    void testArgsElementNull() throws CmdLineArgException {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d', "debug");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> aj.parse(new String[] { "-d", null, "test" }));

        assertEquals("Argument list must not contain null", ex.getMessage());
    }

    private static class NoExitSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(final Permission perm) {
        }

        @Override
        public void checkExit(final int status) {
            throw new ExitCodeException(status);
        }
    }

    private static class ExitCodeException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final int mExitCode;

        public ExitCodeException(final int exitCode) {
            super();
            mExitCode = exitCode;
        }

        public int getExitCode() {
            return mExitCode;
        }

    }
}
