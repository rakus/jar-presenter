package de.r3s6.jarp.args;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.r3s6.jarp.args.ArgsHandler.Argument;
import de.r3s6.jarp.args.ArgsHandler.Counter;
import de.r3s6.jarp.args.ArgsHandler.Flag;
import de.r3s6.jarp.args.ArgsHandler.ValueOption;

class ArgsHandlerTest {

    public static void showHelp() {
        // Intentionally empty
    }

    @Test
    void test() throws CmdLineArgExcpetion {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');
        final ValueOption indexOpt = aj.addValueOption('i');
        final Argument jarArg = aj.addArgument("jar");
        final Argument dirArg = aj.addArgument("dir");
        final List<String> optArgs = new ArrayList<>();
        aj.optionalArgumentList(optArgs);

        aj.handle(Arrays.asList("-i", "demo.html", "jarp.jar", "directory"));

        assertEquals(false, debugOpt.getValue());
        assertEquals("demo.html", indexOpt.getValue());
        assertEquals("jarp.jar", jarArg.getValue());
        assertEquals("directory", dirArg.getValue());
        assertTrue(optArgs.isEmpty());

    }

    @Test
    void test2() throws CmdLineArgExcpetion {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');
        final ValueOption indexOpt = aj.addValueOption('i');
        final Argument jarArg = aj.addArgument("jar");
        final Argument dirArg = aj.addArgument("dir");
        final List<String> optArgs = new ArrayList<>();
        aj.optionalArgumentList(optArgs);

        aj.handle(Arrays.asList("-i", "demo.html", "jarp.jar", "directory", "opt1", "opt2"));

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
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');

        aj.handle(Arrays.asList("-d"));

        assertEquals(true, debugOpt.getValue());
    }

    @Test
    void testDuplicateFlagException() {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        aj.addFlag('d');

        final CmdLineArgExcpetion ex = assertThrows(CmdLineArgExcpetion.class,
                () -> aj.handle(Arrays.asList("-d", "-d")));

        assertEquals("Duplicate option: -d", ex.getMessage());

    }

    @Test
    void testCounter() throws CmdLineArgExcpetion {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        final Counter countOpt = aj.addCounter('v');

        aj.handle(Arrays.asList("-v", "-vvv"));

        assertEquals(4, countOpt.getValue());
    }

    @Test
    void testValueOption() throws CmdLineArgExcpetion {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        final ValueOption f1 = aj.addValueOption('f');
        final ValueOption f2 = aj.addValueOption('o');

        aj.handle(Arrays.asList("-f", "file1", "-ofile2"));

        assertEquals("file1", f1.getValue());
        assertEquals("file2", f2.getValue());
    }

    @Test
    void testDuplicateValueOptionException() {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        aj.addValueOption('f');

        final CmdLineArgExcpetion ex = assertThrows(CmdLineArgExcpetion.class,
                () -> aj.handle(Arrays.asList("-f", "file", "-f", "other file")));

        assertEquals("Duplicate option: -f", ex.getMessage());

    }

    @Test
    void testCombindedOtions() throws CmdLineArgExcpetion {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        final Flag debugOpt = aj.addFlag('d');
        final Counter cntOpt = aj.addCounter('v');
        final ValueOption valueOpt = aj.addValueOption('f');

        aj.handle(Arrays.asList("-dvvvffile-name"));

        assertEquals(true, debugOpt.getValue());
        assertEquals(3, cntOpt.getValue());
        assertEquals("file-name", valueOpt.getValue());
    }

    @Test
    void testOptionalArguments() throws CmdLineArgExcpetion {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        final List<String> optArgs = new ArrayList<>();
        aj.optionalArgumentList(optArgs);

        aj.handle(Arrays.asList("--", "-i", "demo.html"));

        assertEquals(2, optArgs.size());
        assertEquals("-i", optArgs.get(0));
        assertEquals("demo.html", optArgs.get(1));

    }

    @Test
    void testUnexpectedOptionalArguments() throws CmdLineArgExcpetion {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        aj.addFlag('d');
        aj.addArgument("jar");

        final CmdLineArgExcpetion ex = assertThrows(CmdLineArgExcpetion.class,
                () -> aj.handle(Arrays.asList("-d", "jarp.jar", "superflous")));

        assertEquals("Superflous arguments starting with: superflous", ex.getMessage());
    }

    @Test
    void testMissingArgument() throws CmdLineArgExcpetion {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        aj.addFlag('d');
        aj.addArgument("jar");
        aj.addArgument("dir");

        final CmdLineArgExcpetion ex = assertThrows(CmdLineArgExcpetion.class, () -> aj.handle(Arrays.asList("-d")));

        assertEquals("Missing argument(s): jar, dir", ex.getMessage());
    }

    @Test
    void testMissingOptionValue() throws CmdLineArgExcpetion {
        final ArgsHandler aj = new ArgsHandler(ArgsHandlerTest::showHelp);
        aj.addValueOption('o');

        final CmdLineArgExcpetion ex = assertThrows(CmdLineArgExcpetion.class, () -> aj.handle(Arrays.asList("-o")));

        assertEquals("Missing value for option: -o", ex.getMessage());
    }
}
