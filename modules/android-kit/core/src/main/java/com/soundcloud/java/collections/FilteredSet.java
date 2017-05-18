package com.soundcloud.java.collections;

import com.soundcloud.java.functions.Predicate;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

class FilteredSet<E> extends FilteredCollection<E> implements Set<E> {
    FilteredSet(Set<E> unfiltered, Predicate<? super E> predicate) {
        super(unfiltered, predicate);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return Sets.equalsImpl(this, object);
    }

    @Override
    public int hashCode() {
        return Sets.hashCodeImpl(this);
    }
}
