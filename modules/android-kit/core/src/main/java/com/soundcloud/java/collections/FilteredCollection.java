package com.soundcloud.java.collections;

import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.functions.Predicates;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

class FilteredCollection<E> extends AbstractCollection<E> {
    final Collection<E> unfiltered;
    final Predicate<? super E> predicate;

    FilteredCollection(Collection<E> unfiltered,
                       Predicate<? super E> predicate) {
        this.unfiltered = unfiltered;
        this.predicate = predicate;
    }

    FilteredCollection<E> createCombined(Predicate<? super E> newPredicate) {
        return new FilteredCollection<>(unfiltered,
                Predicates.and(predicate, newPredicate));
        // .<E> above needed to compile in JDK 5
    }

    @Override
    public boolean add(E element) {
        if (!predicate.apply(element)) {
            throw new IllegalArgumentException("Could not apply predicate");
        }
        return unfiltered.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        for (E element : collection) {
            if (!predicate.apply(element)) {
                throw new IllegalArgumentException();
            }
        }
        return unfiltered.addAll(collection);
    }

    @Override
    public void clear() {
        Iterables.removeIf(unfiltered, predicate);
    }

    @Override
    public boolean contains(@Nullable Object element) {
        if (MoreCollections.safeContains(unfiltered, element)) {
            @SuppressWarnings("unchecked") // element is in unfiltered, so it must be an E
                    E e = (E) element;
            return predicate.apply(e);
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return MoreCollections.containsAllImpl(this, collection);
    }

    @Override
    public boolean isEmpty() {
        return !Iterables.any(unfiltered, predicate);
    }

    @Override
    public Iterator<E> iterator() {
        return Iterators.filter(unfiltered.iterator(), predicate);
    }

    @Override
    public boolean remove(Object element) {
        return contains(element) && unfiltered.remove(element);
    }

    @Override
    public boolean removeAll(final Collection<?> collection) {
        return Iterables.removeIf(unfiltered, Predicates.and(predicate, Predicates.in(collection)));
    }

    @Override
    public boolean retainAll(final Collection<?> collection) {
        return Iterables.removeIf(unfiltered, Predicates.and(predicate, Predicates.not(Predicates.in(collection))));
    }

    @Override
    public int size() {
        return Iterators.size(iterator());
    }

    @Override
    public Object[] toArray() {
        // creating an ArrayList so filtering happens once
        return Lists.newArrayList(iterator()).toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return Lists.newArrayList(iterator()).toArray(array);
    }
}
