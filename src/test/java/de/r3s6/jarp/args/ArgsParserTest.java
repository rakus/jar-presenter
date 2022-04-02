package de.r3s6.jarp.args;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.r3s6.jarp.args.ArgsParser.Argument;
import de.r3s6.jarp.args.ArgsParser.Counter;
import de.r3s6.jarp.args.ArgsParser.Flag;
import de.r3s6.jarp.args.ArgsParser.ValueOption;

class ArgsParserTest {

    public static void showHelp() {
        // Intenionally left blank
    }

    @Test
    void test() throws CmdLineArgExcpetion {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');
        final ValueOption indexOpt = aj.addValueOption('i');
        final Argument jarArg = aj.addArgument("jar");
        final Argument dirArg = aj.addArgument("dir");
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
    void test2() throws CmdLineArgExcpetion {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');
        final ValueOption indexOpt = aj.addValueOption('i');
        final Argument jarArg = aj.addArgument("jar");
        final Argument dirArg = aj.addArgument("dir");
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
    void testFlag() throws CmdLineArgExcpetion {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');

        aj.parse(Arrays.asList("-d"));

        assertEquals(true, debugOpt.getValue());
    }

    @Test
    void testDuplicateFlagException() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d');

        final CmdLineArgExcpetion ex = assertThrows(CmdLineArgExcpetion.class,
                () -> aj.parse(Arrays.asList("-d", "-d")));

        assertEquals("Duplicate option: -d", ex.getMessage());

    }

    @Test
    void testCounter() throws CmdLineArgExcpetion {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final Counter countOpt = aj.addCounter('v');

        aj.parse(Arrays.asList("-v", "-vvv"));

        assertEquals(4, countOpt.getValue());
    }

    @Test
    void testValueOption() throws CmdLineArgExcpetion {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final ValueOption f1 = aj.addValueOption('f');
        final ValueOption f2 = aj.addValueOption('o');

        aj.parse(Arrays.asList("-f", "file1", "-ofile2"));

        assertEquals("file1", f1.getValue());
        assertEquals("file2", f2.getValue());
    }

    @Test
    void testDuplicateValueOptionException() {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addValueOption('f');

        final CmdLineArgExcpetion ex = assertThrows(CmdLineArgExcpetion.class,
                () -> aj.parse(Arrays.asList("-f", "file", "-f", "other file")));

        assertEquals("Duplicate option: -f", ex.getMessage());

    }

    @Test
    void testCombindedOtions() throws CmdLineArgExcpetion {
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
    void testOptionalArguments() throws CmdLineArgExcpetion {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        final List<String> optArgs = new ArrayList<>();
        aj.optionalArgumentList(optArgs);

        aj.parse(Arrays.asList("--", "-i", "demo.html"));

        assertEquals(2, optArgs.size());
        assertEquals("-i", optArgs.get(0));
        assertEquals("demo.html", optArgs.get(1));

    }

    @Test
    void testUnexpectedOptionalArguments() throws CmdLineArgExcpetion {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d');
        aj.addArgument("jar");

        final CmdLineArgExcpetion ex = assertThrows(CmdLineArgExcpetion.class,
                () -> aj.parse(Arrays.asList("-d", "jarp.jar", "superflous")));

        assertEquals("Superflous arguments starting with: superflous", ex.getMessage());
    }

    @Test
    void testMissingArgument() throws CmdLineArgExcpetion {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addFlag('d');
        aj.addArgument("jar");
        aj.addArgument("dir");

        final CmdLineArgExcpetion ex = assertThrows(CmdLineArgExcpetion.class, () -> aj.parse(Arrays.asList("-d")));

        assertEquals("Missing argument(s): jar, dir", ex.getMessage());
    }

    @Test
    void testMissingOptionValue() throws CmdLineArgExcpetion {
        final ArgsParser aj = new ArgsParser(ArgsParserTest::showHelp);
        aj.addValueOption('o');

        final CmdLineArgExcpetion ex = assertThrows(CmdLineArgExcpetion.class, () -> aj.parse(Arrays.asList("-o")));

        assertEquals("Missing value for option: -o", ex.getMessage());
    }
}