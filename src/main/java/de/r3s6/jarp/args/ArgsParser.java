package de.r3s6.jarp.args;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple command line argument parser.
 *
 * @author rks
 */
public class ArgsParser {

    private static final String OPT_START = "-";
    private static final String LONG_OPT_START = "--";
    private static final String OPT_ARG_DELIM = "--";

    private final HelpCallback mHelpMethod;

    private Map<Character, Option> mOptionMap = new HashMap<>();

    private List<Argument> mArguments = new ArrayList<>();
    private List<String> mAdditionalArguments;

    /**
     * Constructs a ArgsParser.
     *
     * @param helpMethod void method to be called when '--help' encountered
     */
    public ArgsParser(final HelpCallback helpMethod) {
        if (helpMethod == null) {
            throw new NullPointerException("Parameter helpMethod is null");
        }
        this.mHelpMethod = helpMethod;
    }

    /**
     * Add a option flag.
     *
     * A flag is a boolean option to enable something.
     *
     * @param optionChar the option character
     * @return the flag object. Updated when option is found.
     */
    public Flag addFlag(final char optionChar) {
        final Flag flag = new Flag(optionChar);
        mOptionMap.put(Character.valueOf(optionChar), flag);
        return flag;
    }

    /**
     * Adds a counting option.
     *
     * @param optionChar the option character
     * @return the counter object. Updated when option is found.
     */
    public Counter addCounter(final char optionChar) {
        final Counter flag = new Counter(optionChar);
        mOptionMap.put(Character.valueOf(optionChar), flag);
        return flag;
    }

    /**
     * Adds a mValue option, that needs an additional argument.
     *
     * @param optionChar the option character
     * @return the ValueOption object. Updated when option is found.
     */
    public ValueOption addValueOption(final char optionChar) {
        return addValueOption(optionChar, null);
    }

    /**
     * Adds a mValue option with default mValue.
     *
     * @param optionChar   the option character
     * @param defaultValue default mValue if option is not found on the command line
     * @return the ValueOption object. Updated when option is found.
     */
    public ValueOption addValueOption(final char optionChar, final String defaultValue) {
        final ValueOption opt = new ValueOption(optionChar, defaultValue);
        mOptionMap.put(Character.valueOf(optionChar), opt);
        return opt;
    }

    /**
     * Add a required argument.
     *
     * @param name name of the argument. Used when argument could not be filled.
     * @return the Argument options. Updated when argument is found.
     */
    public Argument addArgument(final String name) {
        final Argument arg = new Argument(name);
        mArguments.add(arg);
        return arg;
    }

    /**
     * A list to collect optional arguments.
     *
     * @param list list for arguments. Updated during command line parsing.
     */
    public void optionalArgumentList(final List<String> list) {
        this.mAdditionalArguments = list;
    }

    /**
     * Parse the given command line arguments.
     *
     * @param args the command line arguments
     * @throws CmdLineArgExcpetion on error during parsing. E.g. unknown option.
     */
    public void parse(final String[] args) throws CmdLineArgExcpetion {
        parse(new ArrayDeque<>(Arrays.asList(args)));
    }

    /**
     * Parse the given command line arguments.
     *
     * @param argsList the command line arguments
     * @throws CmdLineArgExcpetion on error during parsing. E.g. unknown option.
     */
    public void parse(final List<String> argsList) throws CmdLineArgExcpetion {
        parse(new ArrayDeque<>(argsList));
    }

    /**
     * Parse the given command line arguments.
     *
     * @param argsQueue the command line arguments
     * @throws CmdLineArgExcpetion on error during parsing. E.g. unknown option.
     */
    public void parse(final Deque<String> argsQueue) throws CmdLineArgExcpetion {

        final Args args = new Args(argsQueue);

        boolean argsOnly = false;

        String param;
        while ((param = args.next()) != null) {
            if (!argsOnly && param.startsWith(OPT_START)) {
                if (OPT_ARG_DELIM.equals(param)) {
                    argsOnly = true;
                } else if ("--help".equals(param)) {
                    mHelpMethod.accept();
                    System.exit(0);
                } else if (param.startsWith(LONG_OPT_START)) {
                    throw new CmdLineArgExcpetion("Invalid option: " + param);
                } else if (param.startsWith(OPT_START)) {
                    final Option o = mOptionMap.get(param.charAt(1));
                    if (o == null) {
                        throw new CmdLineArgExcpetion("Unknown Option: " + param);
                    } else if (!o.settable()) {
                        throw new CmdLineArgExcpetion("Duplicate option: " + param);
                    } else if (o instanceof Flag) {
                        ((Flag) o).found();
                    } else if (o instanceof Counter) {
                        ((Counter) o).found();
                    } else if (o instanceof ValueOption) {
                        final String value = args.fetchArgument();
                        if (value == null) {
                            throw new CmdLineArgExcpetion("Missing value for option: " + param);
                        }
                        ((ValueOption) o).found(value);
                    } else {
                        throw new RuntimeException("Unknown option type");
                    }
                } else {
                    handleArg(param);
                }
            } else {
                handleArg(param);
            }
        }

        if (!mArguments.isEmpty()) {
            throw new CmdLineArgExcpetion("Missing argument(s): "
                    + mArguments.stream().map(Argument::getName).collect(Collectors.joining(", ")));
        }

    }

    private void handleArg(final String param) throws CmdLineArgExcpetion {
        if (mArguments.size() > 0) {
            final Argument arg = mArguments.remove(0);
            arg.found(param);
        } else if (mAdditionalArguments != null) {
            mAdditionalArguments.add(param);
        } else {
            throw new CmdLineArgExcpetion("Superflous arguments starting with: " + param);
        }
    }

    private final class Args {
        private final Deque<String> mArgs;
        private String mRest;

        private Args(final Deque<String> args) {
            this.mArgs = args;
        }

        private String next() {
            final String ret;
            if (mRest != null && mRest.length() != 0) {
                ret = OPT_START + mRest.charAt(0);
                mRest = mRest.substring(1);
            } else {
                if (mArgs.size() != 0) {
                    final String a = mArgs.poll();
                    if (a.startsWith(LONG_OPT_START)) {
                        ret = a;
                    } else if (a.startsWith(OPT_START)) {
                        ret = OPT_START + a.charAt(1);
                        mRest = a.substring(2);
                    } else {
                        ret = a;
                    }
                } else {
                    ret = null;
                }
            }
            return ret;
        }

        private String fetchArgument() {
            final String ret;
            if (mRest != null && mRest.length() != 0) {
                ret = mRest;
                mRest = null;
            } else {
                if (mArgs.size() != 0) {
                    ret = mArgs.poll();
                } else {
                    return null;
                }
            }
            return ret;
        }

    }

    /**
     * Interface for Options.
     *
     * @author rks
     */
    protected interface Option {

        /**
         * Returns the option character.
         *
         * @return the option character
         */
        char getOptionChar();

        /**
         * Whether the mValue of the option might be set.
         *
         * Typiucally returns {@code false} when the option was already set.
         *
         * @return whether the option mValue can be set.
         */
        boolean settable();

    }

    /**
     * Class representing a boolean option.
     */
    public static final class Flag implements Option {
        private final char mCharacter;
        private boolean mValue;

        private Flag(final char character) {
            this.mCharacter = character;
        }

        @Override
        public char getOptionChar() {
            return mCharacter;
        }

        @Override
        public boolean settable() {
            return !mValue;
        }

        /**
         * Gets the mValue of the option.
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
    public static final class Counter implements Option {
        private final char mCharacter;
        private int mValue;

        private Counter(final char character) {
            this.mCharacter = character;
        }

        @Override
        public char getOptionChar() {
            return mCharacter;
        }

        /**
         * Always returns true. {@inheritDoc}
         */
        @Override
        public boolean settable() {
            return true;
        }

        /**
         * Returns the numer of times the option was given on the command line.
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
     * Class representing a mValue option.
     */
    public static final class ValueOption implements Option {
        private final char mCharacter;
        private String mValue;

        private ValueOption(final char character, final String defaultValue) {
            this.mCharacter = character;
            this.mValue = defaultValue;
        }

        @Override
        public char getOptionChar() {
            return mCharacter;
        }

        @Override
        public boolean settable() {
            return mValue == null;
        }

        /**
         * Returns the mValue parsed from the command line parameter or the default
         * mValue (might be {@code null}.
         *
         * @return the option mValue
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
         * Returns the argument mValue.
         *
         * @return the argument mValue
         */
        public String getValue() {
            return mValue;
        }

        private void found(final String value) {
            this.mValue = value;
        }
    }

}
