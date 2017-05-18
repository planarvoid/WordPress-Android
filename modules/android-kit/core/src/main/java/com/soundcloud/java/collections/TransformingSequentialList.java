package com.soundcloud.java.collections;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.java.functions.Function;

import java.io.Serializable;
import java.util.AbstractSequentialList;
import java.util.List;
import java.util.ListIterator;

/**
 * Implementation of a sequential transforming list.
 *
 * @see Lists#transform
 */
class TransformingSequentialList<F, T>
        extends AbstractSequentialList<T> implements Serializable {

    private static final long serialVersionUID = 0;

    final List<F> fromList;
    final Function<? super F, ? extends T> function;

    TransformingSequentialList(
            List<F> fromList, Function<? super F, ? extends T> function) {
        this.fromList = checkNotNull(fromList);
        this.function = checkNotNull(function);
    }

    /**
     * The default implementation inherited is based on iteration and removal of
     * each element which can be overkill. That's why we forward this call
     * directly to the backing list.
     */
    @Override
    public void clear() {
        fromList.clear();
    }

    @Override
    public int size() {
        return fromList.size();
    }

    @Override
    public ListIterator<T> listIterator(final int index) {
        return new TransformedListIterator<F, T>(fromList.listIterator(index)) {
            @Override
            T transform(F from) {
                return function.apply(from);
            }
        };
    }
}
