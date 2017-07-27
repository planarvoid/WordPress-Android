package com.soundcloud.java.collections;

import com.soundcloud.java.functions.Function;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * Implementation of a transforming random access list. We try to make as many
 * of these methods pass-through to the source list as possible so that the
 * performance characteristics of the source list and transformed list are
 * similar.
 *
 * @see Lists#transform
 */
class TransformingRandomAccessList<F, T>
        extends AbstractList<T> implements RandomAccess, Serializable {

    private static final long serialVersionUID = 0;

    final List<F> fromList;
    final Function<? super F, ? extends T> function;

    TransformingRandomAccessList(@NotNull List<F> fromList, @NotNull Function<? super F, ? extends T> function) {
        this.fromList = fromList;
        this.function = function;
    }

    @Override
    public void clear() {
        fromList.clear();
    }

    @Override
    public T get(int index) {
        return function.apply(fromList.get(index));
    }

    @Override
    public Iterator<T> iterator() {
        return listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new TransformedListIterator<F, T>(fromList.listIterator(index)) {
            @Override
            T transform(F from) {
                return function.apply(from);
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return fromList.isEmpty();
    }

    @Override
    public T remove(int index) {
        return function.apply(fromList.remove(index));
    }

    @Override
    public int size() {
        return fromList.size();
    }
}
