package com.soundcloud.java.collections;

import com.soundcloud.java.functions.Function;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

class TransformedCollection<F, T> extends AbstractCollection<T> {
    final Collection<F> fromCollection;
    final Function<? super F, ? extends T> function;

    TransformedCollection(@NotNull Collection<F> fromCollection, @NotNull Function<? super F, ? extends T> function) {
        this.fromCollection = fromCollection;
        this.function = function;
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
