/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.args;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Simple command line argument parser.
 *
 * <p>
 * <b>Usage</b>
 * <p>
 * First create a new ArgParser instance with the Runnable to run when the
 * option '--help' is found.
 * <p>
 * Then add flags ({@link #addFlag(char, String)}), counters
 * ({@link #addCounter(char, String)}) and/or value options
 * ({@link #addValueOption(char, String)}) as needed. This calls returns objects
 * that are filled when corresponding options are found during parsing.
 *
 * <dl>
 * <dt>flag</dt>
 * <dd>Switch to enable something. Like '-l' to enable long output.</dd>
 * <dt>counter</dt>
 * <dd>Counts the occurrences of the option on the command line. Typically used
 * to set verbosity. Like '-v' verbose, '-vv' more verbose, '-vvv' very
 * verbose.</dd>
 * <dt>value</dt>
 * <dd>Option with argument. Like '-o output-file'.</dd>
 * </dl>
 *
 * If the program has required arguments, they can be defined with
 * {@link #addRequiredArgument(String)}. This are named arguments. They must be
 * provided on the command line. If they are missing an error message with the
 * name is given.
 * <p>
 * To allow optional arguments, provide a List to collect them with
 * {@link #optionalArgumentList(List)}.
 * <p>
 * Finally call {@link #parse(List)} or {@link #parse(String[])} to parse the
 * command line arguments. This will throw a {@link CmdLineArgException} if any
 * problem is detected.
 * <p>
 * After parsing the objects returned from option definitions or arguments are
 * filled according to the parsed options and arguments.
 * <p>
 * <b>Option Handling</b>
 * <p>
 * A short option is a '-' followed by a short option character. If the option
 * requires an argument, it may be written directly after the option character
 * or as the next parameter ( '-ooutput.txt' == '-o output.txt').
 * <p>
 * It is possible to specify several short options after one '-', as long as all
 * (except possibly the last) do not require an argument ( '-d -v' == '-dv' and
 * '-d -v -o output.txt' == '-dvooutput.txt).
 * <p>
 * A long option begins with '--' followed by the long option name. If the
 * option requires an argument, it may be written directly after the long option
 * name, separated by '=', or as the next argument ('--output=output.txt' ==
 * '--output output.txt').
 * <p>
 * <b>Exceptions</b>
 * <p>
 * A {@link CmdLineArgException} is thrown if some error is detected during
 * parsing the command line arguments. Like a duplicate option or a missing
 * required argument etc. Its message is intended to be shown to the user and
 * the program should exit after that.
 * <p>
 * {@link IllegalArgumentException}s are thrown on configuration or usage
 * errors. E.g. if a duplicate option is defined or if the argument list
 * contains a null element (which should be impossible if the list comes from
 * the command line).
 * <p>
 * {@link NullPointerException}s are thrown if expected method arguments are
 * {@code null}. Like using a {@code null} for the help function or for the
 * argument list to parse.
 *
 * <p>
 * <b>WARNING: A instance of ArgParser is NOT REUSABLE!</b>
 *
 * @author Ralf Schandl
 */
public class ArgsParser {

    private static final String OPT_START = "-";
    private static final String LONG_OPT_START = "--";
    private static final String OPT_ARG_DELIM = "--";

    private final Runnable mHelpMethod;

    private Map<String, Option> mOptionMap = new HashMap<>();

    private List<Argument> mArguments = new ArrayList<>();
    private List<String> mAdditionalArguments;

    /**
     * Constructs a ArgsParser.
     *
     * @param helpMethod {@link Runnable} to call when '--help' encountered.
     *                   Required.
     */
    public ArgsParser(final Runnable helpMethod) {
        mHelpMethod = Objects.requireNonNull(helpMethod, "Parameter helpMethod is null");
    }

    /**
     * Add a option flag.
     *
     * A flag is a boolean option to enable something.
     *
     * @param optionChar single character used for short options (like 'd' = '-d').
     *                   Allowed: any character except '-' and whitespace. If
     *                   {@code (char)0} no short option is defined.
     * @param optionWord string used for long option (like "debug" = "--debug").
     *                   Must not start with '-' and may contain any character
     *                   except '=', '\' and whitespace. If {@code null} no long
     *                   option is defined.
     *
     * @return the flag object. Updated when option is found.
     *
     * @throws IllegalArgumentException if optionChar or optionWord contain invalid
     *                                  values or on duplicate option definition
     */
    public Flag addFlag(final char optionChar, final String optionWord) {
        return registerOption(new Flag(optionChar, optionWord));
    }

    /**
     * Add a option flag.
     *
     * A flag is a boolean option to enable something.
     * <p>
     * Equivalent to {@code addFlag(optionChar, null)}.
     *
     * @param optionChar single character used for short options (like 'd' = '-d').
     *                   Allowed: any character except '-' and whitespace. If
     *                   {@code (char)0} no short option is defined.
     *
     * @return the flag object. Updated when option is found.
     *
     * @throws IllegalArgumentException if optionChar is invalid or on duplicate
     *                                  option definition
     */
    public Flag addFlag(final char optionChar) {
        return addFlag(optionChar, null);
    }

    /**
     * Adds a counting option.
     *
     * A counting option can be used multiple time on the command line and the
     * number of occurrences are counted.
     *
     * @param optionChar single character used for short options (like 'd' = '-d').
     *                   Allowed: any character except '-' and whitespace. If
     *                   {@code (char)0} no short option is defined.
     * @param optionWord string used for long option (like "debug" = "--debug").
     *                   Must not start with '-' and may contain any character
     *                   except '=', '\' and whitespace. If {@code null} no long
     *                   option is defined.
     *
     * @return the counter object. Updated when option is found.
     *
     * @throws IllegalArgumentException if optionChar or optionWord contain invalid
     *                                  values or on duplicate option definition
     */
    public Counter addCounter(final char optionChar, final String optionWord) {
        return registerOption(new Counter(optionChar, optionWord));
    }

    /**
     * Adds a counting option.
     *
     * A counting option can be used multiple time on the command line and the
     * number of occurrences are counted.
     * <p>
     * Equivalent to {@code addFlag(optionChar, null)}.
     *
     * @param optionChar single character used for short options (like 'd' = '-d').
     *                   Allowed: any character except '-' and whitespace. If
     *                   {@code (char)0} no short option is defined.
     *
     * @return the counter object. Updated when option is found.
     *
     * @throws IllegalArgumentException if optionChar is invalid or on duplicate
     *                                  option definition
     */
    public Counter addCounter(final char optionChar) {
        return addCounter(optionChar, null);
    }

    /**
     * Adds a value option, that needs an additional argument.
     *
     * @param optionChar single character used for short options (like 'd' = '-d').
     *                   Allowed: any character except '-' and whitespace. If
     *                   {@code (char)0} no short option is defined.
     * @param optionWord string used for long option (like "debug" = "--debug").
     *                   Must not start with '-' and may contain any character
     *                   except '=', '\' and whitespace. If {@code null} no long
     *                   option is defined.
     *
     * @return the ValueOption object. Updated when option is found.
     *
     * @throws IllegalArgumentException if optionChar or optionWord contain invalid
     *                                  values or on duplicate option definition
     */
    public ValueOption addValueOption(final char optionChar, final String optionWord) {
        return registerOption(new ValueOption(optionChar, optionWord));
    }

    /**
     * Adds a value option, that needs an additional argument.
     * <p>
     * Equivalent to {@code addValueOption(optionChar, null)}.
     *
     *
     * @param optionChar single character used for short options (like 'd' = '-d').
     *                   Allowed: any character except '-' and whitespace. If
     *                   {@code (char)0} no short option is defined.
     *
     * @return the ValueOption object. Updated when option is found.
     *
     * @throws IllegalArgumentException if optionChar is invalid or on duplicate
     *                                  option definition
     */
    public ValueOption addValueOption(final char optionChar) {
        return addValueOption(optionChar, null);
    }

    private <T extends ArgsParser.Option> T registerOption(final T option) {
        final List<String> optionChar = option.getOptions();

        for (final String o : optionChar) {
            if (mOptionMap.containsKey(o)) {
                throw new IllegalArgumentException("Option '" + o + "' already used");
            }
            mOptionMap.put(o, option);
        }

        return option;
    }

    /**
     * Add a required argument.
     *
     * @param name name of the argument. Used for error message when argument could
     *             not be filled.
     * @return the Argument options. Updated when argument is found.
     */
    public Argument addRequiredArgument(final String name) {
        final Argument arg = new Argument(Objects.requireNonNull(name));
        mArguments.add(arg);
        return arg;
    }

    /**
     * Sets the list to collect optional arguments.
     *
     * @param optArgList list for arguments. Updated during command line parsing. If
     *                   {@code null}, no optional arguments are supported.
     */
    public void optionalArgumentList(final List<String> optArgList) {
        this.mAdditionalArguments = optArgList;
    }

    /**
     * Parse the given command line arguments.
     *
     * @param args the command line arguments.
     * @throws CmdLineArgException      on error during parsing. E.g. unknown
     *                                  option.
     * @throws NullPointerException     if args is {@code null}
     * @throws IllegalArgumentException if args contains a {@code null}
     */
    public void parse(final String[] args) throws CmdLineArgException {
        Objects.requireNonNull(args, "args must not be null");
        parse(Arrays.asList(args));
    }

    /**
     * Parse the given command line arguments.
     *
     * @param argsList the command line arguments
     * @throws CmdLineArgException      on error during parsing. E.g. unknown
     *                                  option.
     * @throws NullPointerException     if args is {@code null}
     * @throws IllegalArgumentException if args contains a {@code null}
     */
    public void parse(final List<String> argsList) throws CmdLineArgException {
        Objects.requireNonNull(argsList, "argsList must not be null");
        if (argsList.contains(null)) {
            throw new IllegalArgumentException("Argument list must not contain null");
        }

        final Args args = new Args(argsList);

        boolean argsOnly = false;

        String param;
        while ((param = args.next()) != null) {
            if (!argsOnly && param.startsWith(OPT_START)) {
                if (OPT_ARG_DELIM.equals(param)) {
                    argsOnly = true;
                } else if ("--help".equals(param)) {
                    mHelpMethod.run();
                    System.exit(0);
                } else {
                    final Option o = mOptionMap.get(param);
                    if (o == null) {
                        throw new CmdLineArgException("Unknown option: " + param);
                    } else if (!o.isSettable()) {
                        throw new CmdLineArgException("Duplicate option: " + String.join("/", o.getOptions()));

                    } else if (o instanceof Flag) {
                        ((Flag) o).found();
                    } else if (o instanceof Counter) {
                        ((Counter) o).found();
                    } else if (o instanceof ValueOption) {
                        final String value = args.fetchArgument();
                        if (value == null) {
                            throw new CmdLineArgException("Missing value for option: " + param);
                        }
                        ((ValueOption) o).found(value);
                    } else {
                        throw new IllegalStateException("Unknown option type - BUG");
                    }
                }
            } else {
                handleArg(param);
            }
        }

        if (!mArguments.isEmpty()) {
            throw new CmdLineArgException("Missing argument" + (mArguments.size() > 1 ? "s: " : ": ")
                    + mArguments.stream().map(Argument::getName).collect(Collectors.joining(", ")));
        }
    }

    private void handleArg(final String param) throws CmdLineArgException {
        if (!mArguments.isEmpty()) {
            final Argument arg = mArguments.remove(0);
            arg.found(param);
        } else if (mAdditionalArguments != null) {
            mAdditionalArguments.add(param);
        } else {
            throw new CmdLineArgException("Superfluous arguments starting with: " + param);
        }
    }

    /**
     * Implements the actual argument handling.
     */
    private final class Args {

        private static final String EMPTY = "";

        private final Iterator<String> mArgs;

        /*
         * Rest while combined short arguments are processed. E.g. -xyz results in
         * option '-x' and leftover 'yz'
         */
        private String mLeftOver = EMPTY;

        /**
         * After parsing '--file=filename', this contains [ "--file", "filename" ]. Else
         * it's {@code null}.
         */
        private String[] mEqualArgument;

        private Args(final List<String> args) {
            this.mArgs = args.iterator();
        }

        /**
         * Returns the next part of the arguments or {@code null} if all arguments are
         * consumed.
         * <p>
         * It splits combined short options. So '-xyz' will be splitted and returned as
         * "-x", "-y" and "-z".
         *
         * @return next argument or {@code null}
         * @throws CmdLineArgException if a argument for a long option with equal sign
         *                             ("--option=value") was not used.
         */
        private String next() throws CmdLineArgException {
            if (mEqualArgument != null) {
                throw new CmdLineArgException("Superfluous argument in \"" + String.join("=", mEqualArgument) + "\"");
            }
            final String ret;
            if (!mLeftOver.isEmpty()) {
                ret = OPT_START + mLeftOver.charAt(0);
                mLeftOver = mLeftOver.substring(1);
            } else {
                if (mArgs.hasNext()) {
                    final String a = mArgs.next();
                    if (a.startsWith(LONG_OPT_START)) {
                        final String[] parts = a.split("=", 2);
                        if (parts.length == 2) {
                            mEqualArgument = parts;
                            ret = parts[0];
                        } else {
                            ret = a;
                        }
                    } else if (a.startsWith(OPT_START)) {
                        ret = OPT_START + a.charAt(1);
                        mLeftOver = a.substring(2);
                    } else {
                        ret = a;
                    }
                } else {
                    ret = null;
                }
            }
            return ret;
        }

        /**
         * Returns the argument for a previous value option.
         * <p>
         * The argument could be the leftover from a combined short option
         * ("-ooutput-file") or from a combined long option ("--output=output-file") or
         * the next argument on the command line.
         *
         * @return the argument or {@code null} if no arguments left
         */
        private String fetchArgument() {
            final String ret;
            if (mEqualArgument != null) {
                // get the value from "--option=value"
                ret = mEqualArgument[1];
                mEqualArgument = null;
            } else if (!mLeftOver.isEmpty()) {
                // get leftover from "-oValue"
                ret = mLeftOver;
                mLeftOver = EMPTY;
            } else {
                // take next arg if available
                if (mArgs.hasNext()) {
                    ret = mArgs.next();
                } else {
                    ret = null;
                }
            }
            return ret;
        }
    }

    /**
     * Interface for Options.
     */
    private abstract static class Option {

        private final List<String> mOptions;

        private Option(final char shotOption, final String longOption) {
            final List<String> opts = new ArrayList<>();
            if (shotOption != 0) {
                if (shotOption == '-' || Character.isWhitespace(shotOption)) {
                    throw new IllegalArgumentException("Invalid short option '" + shotOption + "'");
                }
                opts.add("-" + shotOption);
            }
            if (longOption != null) {
                if (longOption.startsWith("-") || longOption.length() < 2 || longOption.matches(".*[\\s=\\\\].*")) {
                    throw new IllegalArgumentException("Invalid long option '" + longOption + "'");
                }

                opts.add("--" + longOption);
            }
            if (opts.isEmpty()) {
                throw new IllegalArgumentException("Either short and/or long option needed");
            }
            mOptions = Collections.unmodifiableList(opts);

        }

        /**
         * Returns the options command line representations including leading dash(es).
         *
         * @return the option representations
         */
        protected List<String> getOptions() {
            return mOptions;
        }

        /**
         * Whether the value is settable and can be processed. For duplicate option
         * detection.
         *
         * Returns {@code true} when the option was already set, except for
         * {@link Counter} options.
         *
         * @return whether the option can be processed.
         */
        protected abstract boolean isSettable();

    }

    /**
     * Class representing a boolean option.
     */
    public static final class Flag extends Option {
        private boolean mValue;

        private Flag(final char character, final String word) {
            super(character, word);
        }

        @Override
        protected boolean isSettable() {
            return !mValue;
        }

        /**
         * Gets the value of the option.
         *
         * @return whether the option was given on the command line.
         */
        public boolean getValue() {
            return mValue;
        }

        private void found() {
            mValue = true;
        }
    }

    /**
     * Class representing a counting option.
     */
    public static final class Counter extends Option {
        private int mValue;

        private Counter(final char character, final String word) {
            super(character, word);
        }

        /**
         * Always returns false. {@inheritDoc}
         */
        @Override
        protected boolean isSettable() {
            return true;
        }

        /**
         * Returns the number of times the option was given on the command line.
         *
         * @return the option count
         */
        public int getValue() {
            return mValue;
        }

        private void found() {
            mValue++;
        }
    }

    /**
     * Class representing a value option.
     */
    public static final class ValueOption extends Option {
        private String mValue;

        private ValueOption(final char character, final String word) {
            super(character, word);
        }

        @Override
        protected boolean isSettable() {
            return mValue == null;
        }

        /**
         * Returns the value parsed from the command line option.
         *
         * @return the option value or {@code null} when option was not given.
         */
        public String getValue() {
            return mValue;
        }

        private void found(final String value) {
            this.mValue = value;
        }
    }

    /**
     * Class representing a mandatory argument.
     */
    public static final class Argument {
        private final String mName;
        private String mValue;

        private Argument(final String name) {
            this.mName = name;
        }

        String getName() {
            return mName;
        }

        /**
         * Returns the argument value.
         *
         * @return the argument value
         */
        public String getValue() {
            return mValue;
        }

        private void found(final String value) {
            this.mValue = value;
        }
    }

    /**
     * Reports a problem while parsing the command line arguments.
     *
     * @author Ralf Schandl
     */
    public final class CmdLineArgException extends Exception {

        private static final long serialVersionUID = 1L;

        private CmdLineArgException(final String message) {
            super(message);
        }

    }
}
