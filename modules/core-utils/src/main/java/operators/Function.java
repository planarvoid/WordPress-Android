package operators;

public interface Function<F, T> {
    /**
     * Returns the result {@link T} after applying a function/operation on {@code input}.
     *
     * @throws NullPointerException if {@code input} is null and this function does not accept null
     *     arguments
     */
    T apply(F input);

}

