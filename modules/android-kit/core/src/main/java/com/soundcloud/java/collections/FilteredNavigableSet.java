package com.soundcloud.java.collections;

import com.soundcloud.java.functions.Predicate;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NavigableSet;

class FilteredNavigableSet<E> extends FilteredSortedSet<E> implements NavigableSet<E> {
    FilteredNavigableSet(NavigableSet<E> unfiltered, Predicate<? super E> predicate) {
        super(unfiltered, predicate);
    }

    NavigableSet<E> unfiltered() {
        return (NavigableSet<E>) unfiltered;
    }

    @Override
    @Nullable
    public E lower(E e) {
        return Iterators.getNext(headSet(e, false).descendingIterator(), null);
    }

    @Override
    @Nullable
    public E floor(E e) {
        return Iterators.getNext(headSet(e, true).descendingIterator(), null);
    }

    @Override
    public E ceiling(E e) {
        return Iterables.getFirst(tailSet(e, true), null);
    }

    @Override
    public E higher(E e) {
        return Iterables.getFirst(tailSet(e, false), null);
    }

    @Override
    public E pollFirst() {
        return Iterables.removeFirstMatching(unfiltered(), predicate);
    }

    @Override
    public E pollLast() {
        return Iterables.removeFirstMatching(unfiltered().descendingSet(), predicate);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return Sets.filter(unfiltered().descendingSet(), predicate);
    }

    @Override
    public Iterator<E> descendingIterator() {
        return Iterators.filter(unfiltered().descendingIterator(), predicate);
    }

    @Override
    public E last() {
        return descendingIterator().next();
    }

    @Override
    public NavigableSet<E> subSet(
            E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return Sets.filter(
                unfiltered().subSet(fromElement, fromInclusive, toElement, toInclusive), predicate);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return Sets.filter(unfiltered().headSet(toElement, inclusive), predicate);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return Sets.filter(unfiltered().tailSet(fromElement, inclusive), predicate);
    }
}
