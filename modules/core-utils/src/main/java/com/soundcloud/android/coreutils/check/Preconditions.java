package com.soundcloud.android.coreutils.check;

/**
 * Methods from within this class have been copied from
 * Guava 16.0
 */
public final class Preconditions {

    private Preconditions() {}
    /**
     * Checks that {@code expression} is true otherwise throws a {@link java.lang.IllegalArgumentException}
     * @param expression The expressions which should be checked to be true
     * @param errorMessage Error message associated with the exception
     */
    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference
     * @param msg Message to associate with the {@link java.lang.NullPointerException}
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T> T checkNotNull(T reference, String msg) {
        if (reference == null) {
            throw new NullPointerException(msg);
        }
        return reference;
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * @param expression a boolean expression
     * @param msg The message to associated with the {@link java.lang.IllegalStateException}
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression, String msg) {
        if (!expression) {
            throw new IllegalStateException(msg);
        }
    }

}
