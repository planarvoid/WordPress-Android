package com.soundcloud.java.collections;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.java.functions.Function;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

class TransformedCollection<F, T> extends AbstractCollection<T> {
    final Collection<F> fromCollection;
    final Function<? super F, ? extends T> function;

    TransformedCollection(Collection<F> fromCollection,
                          Function<? super F, ? extends T> function) {
        this.fromCollection = checkNotNull(fromCollection);
        this.function = checkNotNull(function);
    }

    @Override
    public void clear() {
        fromCollection.clear();
    }

    @Override
    public boolean isEmpty() {
        return fromCollection.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.transform(fromCollection.iterator(), function);
    }

    @Override
    public int size() {
        return fromCollection.size();
    }
}
