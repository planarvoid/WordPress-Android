package com.soundcloud.java.collections;

import com.soundcloud.java.functions.Predicate;

import java.util.Comparator;
import java.util.SortedSet;

class FilteredSortedSet<E> extends FilteredSet<E> implements SortedSet<E> {

    private final SortedSet<E> sortedUnfiltered;

    FilteredSortedSet(SortedSet<E> unfiltered, Predicate<? super E> predicate) {
        super(unfiltered, predicate);
        this.sortedUnfiltered = unfiltered;
    }

    @Override
    public Comparator<? super E> comparator() {
        return sortedUnfiltered.comparator();
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return new FilteredSortedSet<>(sortedUnfiltered.subSet(fromElement, toElement),
                predicate);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return new FilteredSortedSet<>(sortedUnfiltered.headSet(toElement), predicate);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return new FilteredSortedSet<>(sortedUnfiltered.tailSet(fromElement), predicate);
    }

    @Override
    public E first() {
        return iterator().next();
    }

    @Override
    public E last() {
        SortedSet<E> tempSet = sortedUnfiltered;
        while (true) {
            E element = tempSet.last();
            if (predicate.apply(element)) {
                return element;
            }
            tempSet = tempSet.headSet(element);
        }
    }
}
