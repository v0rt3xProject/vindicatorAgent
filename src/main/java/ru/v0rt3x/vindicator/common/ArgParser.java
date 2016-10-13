package ru.v0rt3x.vindicator.common;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgParser {

    public static Args parse(String[] commandLine) {
        return parse(String.format("vindicator %s", Joiner.on(" ").join(commandLine)));
    }

    @SuppressWarnings("ConstantConditions")
    public static Args parse(String commandLine) {
        String cmd = null;
        String keyword = null;

        commandLine += '\0';

        List<String> args = new ArrayList<>();
        Map<String, String> kwargs = new HashMap<>();

        boolean doubleQuotes = false;
        boolean singleQuotes = false;

        boolean commandArgument = true;
        boolean positionalArgument = false;
        boolean keywordArgument = false;
        boolean keywordValue = false;

        boolean escapeCharacter = false;

        StringBuilder currentString = new StringBuilder();

        for (char chr: commandLine.toCharArray()) {
            switch (chr) {
                case '"':
                    doubleQuotes = escapeCharacter == doubleQuotes;
                    currentString.append(escapeCharacter ? "\"" : "");
                    escapeCharacter = false;
                    break;
                case '\'':
                    singleQuotes = escapeCharacter == singleQuotes;
                    escapeCharacter = false;
                    break;
                case '\\':
                    currentString.append(escapeCharacter ? "\\" : "");
                    escapeCharacter = !escapeCharacter;
                    break;
                case '-':
                    if (commandArgument||positionalArgument||keywordArgument) {
                        if (currentString.length() > 0)
                            currentString.append("-");
                    } else if (keywordValue) {
                        if (currentString.length() > 0) {
                            currentString.append("-");
                        } else {
                            kwargs.put(keyword, "");
                            currentString = new StringBuilder();
                            keywordArgument = true;
                            keywordValue = false;
                        }
                    } else {
                        keywordArgument = true;
                    }
                    break;
                case ' ':
                    if (commandArgument) {
                        if (currentString.length() > 0) {
                            cmd = currentString.toString();
                            currentString = new StringBuilder();
                            commandArgument = false;
                        }
                    } else if (doubleQuotes||singleQuotes) {
                        if (!(positionalArgument||keywordArgument||keywordValue))
                            positionalArgument = true;
                        currentString.append(' ');
                    } else {
                        if (positionalArgument) {
                            args.add(currentString.toString());
                            currentString = new StringBuilder();
                            positionalArgument = false;
                        } else if (keywordArgument) {
                            keyword = currentString.toString();
                            currentString = new StringBuilder();
                            keywordArgument = false;
                            keywordValue = true;
                        } else if (keywordValue) {
                            kwargs.put(keyword, currentString.toString());
                            currentString = new StringBuilder();
                            keywordValue = false;
                        }
                    }
                    break;
                case '=':
                    if (keywordArgument) {
                        keyword = currentString.toString();
                        currentString = new StringBuilder();
                        keywordArgument = false;
                        keywordValue = true;
                    } else {
                        currentString.append('=');
                    }
                    break;
                case '\0':
                    if (commandArgument) {
                        cmd = currentString.toString();
                    } else if (keywordArgument) {
                        kwargs.put(currentString.toString(), "");
                    } else if (keywordValue) {
                        kwargs.put(keyword, currentString.toString());
                    } else if (positionalArgument) {
                        args.add(currentString.toString());
                    }
                    break;
                default:
                    if (!(commandArgument||positionalArgument||keywordArgument||keywordValue))
                        positionalArgument = true;
                    currentString.append(chr);
                    break;
            }
        }

        return new Args(cmd, args, kwargs);
    }

    public static class Args {

        private final String cmd;
        private final List<String> args;
        private final Map<String, String> kwargs;

        private Args(String cmd, List<String> args, Map<String, String> kwargs) {
            this.cmd = cmd;
            this.args = args;
            this.kwargs = kwargs;
        }

        public String cmd() {
            return cmd;
        }

        public List<String> args() {
            return args;
        }

        public String args(Integer argumentId) {
            return args.get(argumentId);
        }

        public Map<String, String> kwargs() {
            return kwargs;
        }

        public String kwargs(String keyword, String defaultValue) {
            return kwargs.getOrDefault(keyword, defaultValue);
        }
    }
}
