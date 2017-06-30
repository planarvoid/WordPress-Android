package com.soundcloud.android.memento.utils;

import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public final class Logger {

    public static void log(ProcessingEnvironment processingEnv, String msg, Object... args) {
        printMessage(processingEnv, Diagnostic.Kind.NOTE, msg, args);
    }

    public static void log(ProcessingEnvironment processingEnv, Element element, String msg, Object... args) {
        printMessage(processingEnv, Diagnostic.Kind.NOTE, element, msg, args);
    }

    public static void error(ProcessingEnvironment processingEnv, Element element, String message, Object... args) {
        printMessage(processingEnv, Diagnostic.Kind.ERROR, element, message, args);
    }

    public static void fatalError(ProcessingEnvironment processingEnv, String msg, Object... args) {
        printMessage(processingEnv, Diagnostic.Kind.ERROR, "FATAL ERROR: " + msg, args);
    }

    public static void fatalError(ProcessingEnvironment processingEnv, Element element, String msg, Object... args) {
        printMessage(processingEnv, Diagnostic.Kind.ERROR, element, "FATAL ERROR: " + msg, args);
    }

    private static String format(String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        return message;
    }

    private static void printMessage(ProcessingEnvironment processingEnv, Diagnostic.Kind kind, String msg, Object... args) {
        printMessage(processingEnv, kind, null, msg, args);
    }

    private static void printMessage(ProcessingEnvironment processingEnv, Diagnostic.Kind kind, @Nullable Element element, String msg, Object... args) {
        if (kind == Diagnostic.Kind.NOTE && !processingEnv.getOptions().containsKey("debug")) {
            return;
        }
        final String message = format(msg, args);
        processingEnv.getMessager().printMessage(kind, message, element);
    }

    private Logger() {
    }
}
