package de.r3s6.jarp.args;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArgsHandler {

    private Map<Character, Option> optionMap = new HashMap<>();

    private List<Argument> arguments = new ArrayList<>();
    private List<String> additionalArguments;

    public Flag addFlag(final char optionChar) {
        final Flag flag = new Flag(optionChar);
        optionMap.put(Character.valueOf(optionChar), flag);
        return flag;
    }

    public Counter addCounter(final char optionChar) {
        final Counter flag = new Counter(optionChar);
        optionMap.put(Character.valueOf(optionChar), flag);
        return flag;
    }

    public ValueOption addValueOption(final char optionChar) {
        return addValueOption(optionChar, null);
    }

    public ValueOption addValueOption(final char optionChar, final String defaultValue) {
        final ValueOption opt = new ValueOption(optionChar, defaultValue);
        optionMap.put(Character.valueOf(optionChar), opt);
        return opt;
    }

    public Argument addArgument(final String name) {
        final Argument arg = new Argument(name);
        arguments.add(arg);
        return arg;
    }

    public void optionalArgumentList(final List<String> list) {
        this.additionalArguments = list;
    }

    public void handle(final String[] args) throws CmdLineArgExcpetion {
        handle(new ArrayDeque<>(Arrays.asList(args)));
    }

    public void handle(final List<String> argsList) throws CmdLineArgExcpetion {
        handle(new ArrayDeque<>(argsList));
    }

    public void handle(final Deque<String> argsQueue) throws CmdLineArgExcpetion {

        final Args args = new Args(argsQueue);

        boolean argsOnly = false;

        String param;
        while ((param = args.next()) != null) {
            if (!argsOnly && param.startsWith("-")) {
                if ("--".equals(param)) {
                    argsOnly = true;
                } else if ("--help".equals(param)) {
                    throw new CmdLineArgExcpetion("Help");
                } else if (param.startsWith("--")) {
                    throw new CmdLineArgExcpetion("Invalid option: " + param);
                } else if (param.startsWith("-")) {
                    final Option o = optionMap.get(param.charAt(1));
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

        if (!arguments.isEmpty()) {
            throw new CmdLineArgExcpetion("Missing argument(s): "
                    + arguments.stream().map(Argument::getName).collect(Collectors.joining(", ")));
        }

    }

    private void handleArg(final String param) throws CmdLineArgExcpetion {
        if (arguments.size() > 0) {
            final Argument arg = arguments.remove(0);
            arg.found(param);
        } else if (additionalArguments != null) {
            additionalArguments.add(param);
        } else {
            throw new CmdLineArgExcpetion("Superflous arguments starting with: " + param);
        }
    }

    private class Args {
        private final Deque<String> args;
        private String rest;

        private Args(final Deque<String> args) {
            super();
            this.args = args;
        }

        private String next() {
            String ret;
            if (rest != null && rest.length() != 0) {
                ret = "-" + rest.charAt(0);
                rest = rest.substring(1);
            } else {
                if (args.size() != 0) {
                    final String a = args.poll();
                    if (a.startsWith("--")) {
                        ret = a;
                    } else if (a.startsWith("-")) {
                        ret = "-" + a.charAt(1);
                        rest = a.substring(2);
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
            String ret;
            if (rest != null && rest.length() != 0) {
                ret = rest;
                rest = null;
            } else {
                if (args.size() != 0) {
                    ret = args.poll();
                } else {
                    return null;
                }
            }
            return ret;
        }

    }

    protected static interface Option {

        char getOptionChar();

        boolean settable();

    }

    public static class Flag implements Option {
        private final char character;
        private boolean value = false;

        public Flag(final char character) {
            this.character = character;
        }

        @Override
        public char getOptionChar() {
            return character;
        }

        @Override
        public boolean settable() {
            return !value;
        }

        public boolean getValue() {
            return value;
        }

        private void found() {
            value = true;
        }
    }

    public static class Counter implements Option {
        private final char character;
        private int value = 0;

        public Counter(final char character) {
            this.character = character;
        }

        @Override
        public char getOptionChar() {
            return character;
        }

        @Override
        public boolean settable() {
            return true;
        }

        public int getValue() {
            return value;
        }

        private void found() {
            value++;
        }
    }

    public static class ValueOption implements Option {
        private final char character;
        private String value;

        public ValueOption(final char character, final String defaultValue) {
            this.character = character;
            this.value = defaultValue;
        }

        @Override
        public char getOptionChar() {
            return character;
        }

        @Override
        public boolean settable() {
            return value == null;
        }

        public String getValue() {
            return value;
        }

        private void found(final String value) {
            this.value = value;
        }
    }

    public static class Argument {
        private final String name;
        private String value;

        public Argument(final String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        private void found(final String value) {
            this.value = value;
        }
    }

}
