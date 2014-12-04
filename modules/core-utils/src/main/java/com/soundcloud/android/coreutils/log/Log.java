package com.soundcloud.android.coreutils.log;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Log {

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\.(\\w+)$");
    private static final String LOG_IDENTIFIER = "sclog.";
    private static final String LOGGER_NAME_PATTERN = LOG_IDENTIFIER + "%s";
    private static final int LOGGER_NAME_LENGTH_LIMIT = 23;

    private Log(){}


    public static void error(@NotNull final Logger logger, @NotNull final String message, Object... args) {
        logger.error(message, args);
    }

    public static void error(@NotNull final Logger logger, @NotNull Throwable throwable, @NotNull final String message, Object... args) {
        logger.error(String.format(message, args), throwable);
    }

    public static void info(@NotNull Logger logger, @NotNull String message, @NotNull Object... args) {
        logger.info(message, args);
    }

    public static Logger getLogger(){
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String fullClassNameOfRequestingClass = stackTraceElements[2].getClassName();
        Matcher matcher = CLASS_NAME_PATTERN.matcher(fullClassNameOfRequestingClass);
        if(!matcher.find()){
            return LoggerFactory.getLogger(String.format(LOGGER_NAME_PATTERN, "Unidentified"));
        }
        String className = matcher.group(1);
        if(loggerNameIsTooLong(className)){
            className = truncateClassName(className);
        }
        return LoggerFactory.getLogger(String.format(LOGGER_NAME_PATTERN, className));
    }

    private static String truncateClassName(String className) {
        int charsAllowed = LOGGER_NAME_LENGTH_LIMIT - LOG_IDENTIFIER.length();
        return className.substring(0, charsAllowed);
    }

    private static boolean loggerNameIsTooLong(String className) {
        return String.format(LOGGER_NAME_PATTERN, className).length() > LOGGER_NAME_LENGTH_LIMIT;
    }

}

