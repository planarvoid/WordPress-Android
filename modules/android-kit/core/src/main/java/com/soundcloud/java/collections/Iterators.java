/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.java.collections;

import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.functions.Predicates;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class contains static utility methods that operate on or return objects
 * of type {@link Iterator}. Except as noted, each method has a corresponding
 * {@link Iterable}-based method in the {@link Iterables} class.
 *
 * <p><i>Performance notes:</i> Unless otherwise noted, all of the iterators
 * produced in this class are <i>lazy</i>, which means that they only advance
 * the backing iteration when absolutely necessary.
 *
 * <p>See the Guava User Guide section on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/CollectionUtilitiesExplained#Iterables">
 * {@code Iterators}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 */
public final class Iterators {
    private Iterators() {
    }

    static final UnmodifiableListIterator<Object> EMPTY_LIST_ITERATOR
            = new UnmodifiableListIterator<Object>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public Object previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }
    };

    private static final Iterator<Object> EMPTY_MODIFIABLE_ITERATOR =
            new Iterator<Object>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Object next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    CollectPreconditions.checkRemove(false);
                }
            };

    /**
     * Returns the empty {@code Iterator} that throws
     * {@link IllegalStateException} instead of
     * {@link UnsupportedOperationException} on a call to
     * {@link Iterator#remove()}.
     */
    // Casting to any type is safe since there are no actual elements.
    @SuppressWarnings("unchecked")
    static <T> Iterator<T> emptyModifiableIterator() {
        return (Iterator<T>) EMPTY_MODIFIABLE_ITERATOR;
    }

    /**
     * Returns the empty iterator.
     */
    static <T> UnmodifiableIterator<T> emptyIterator() {
        return emptyListIterator();
    }

    /**
     * Returns the empty iterator.
     */
    // Casting to any type is safe since there are no actual elements.
    @SuppressWarnings("unchecked")
    static <T> UnmodifiableListIterator<T> emptyListIterator() {
        return (UnmodifiableListIterator<T>) EMPTY_LIST_ITERATOR;
    }

    /**
     * Returns the number of elements remaining in {@code iterator}. The iterator
     * will be left exhausted: its {@code hasNext()} method will return
     * {@code false}.
     */
    public static int size(Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    /**
     * Returns {@code true} if {@code iterator} contains {@code element}.
     */
    public static boolean contains(Iterator<?> iterator, @Nullable Object element) {
        return any(iterator, Predicates.equalTo(element));
    }

    /**
     * Traverses an iterator and removes every element that belongs to the
     * provided collection. The iterator will be left exhausted: its
     * {@code hasNext()} method will return {@code false}.
     *
     * @param removeFrom       the iterator to (potentially) remove elements from
     * @param elementsToRemove the elements to remove
     * @return {@code true} if any element was removed from {@code iterator}
     */
    public static boolean removeAll(
            Iterator<?> removeFrom, Collection<?> elementsToRemove) {
        return removeIf(removeFrom, Predicates.in(elementsToRemove));
    }

    /**
     * Removes every element that satisfies the provided predicate from the
     * iterator. The iterator will be left exhausted: its {@code hasNext()}
     * method will return {@code false}.
     *
     * @param removeFrom the iterator to (potentially) remove elements from
     * @param predicate  a predicate that determines whether an element should
     *                   be removed
     * @return {@code true} if any elements were removed from the iterator
     * @since 2.0
     */
    public static <T> boolean removeIf(
            Iterator<T> removeFrom, Predicate<? super T> predicate) {
        if (predicate == null) {
            throw new NullPointerException();
        }
        boolean modified = false;
        while (removeFrom.hasNext()) {
            if (predicate.apply(removeFrom.next())) {
                removeFrom.remove();
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Traverses an iterator and removes every element that does not belong to the
     * provided collection. The iterator will be left exhausted: its
     * {@code hasNext()} method will return {@code false}.
     *
     * @param removeFrom       the iterator to (potentially) remove elements from
     * @param elementsToRetain the elements to retain
     * @return {@code true} if any element was removed from {@code iterator}
     */
    public static boolean retainAll(
            Iterator<?> removeFrom, Collection<?> elementsToRetain) {
        return removeIf(removeFrom, Predicates.not(Predicates.in(elementsToRetain)));
    }

    /**
     * Determines whether two iterators contain equal elements in the same order.
     * More specifically, this method returns {@code true} if {@code iterator1}
     * and {@code iterator2} contain the same number of elements and every element
     * of {@code iterator1} is equal to the corresponding element of
     * {@code iterator2}.
     *
     * <p>Note that this will modify the supplied iterators, since they will have
     * been advanced some number of elements forward.
     */
    public static boolean elementsEqual(
            Iterator<?> iterator1, Iterator<?> iterator2) {
        while (iterator1.hasNext()) {
            if (!iterator2.hasNext()) {
                return false;
            }
            Object o1 = iterator1.next();
            Object o2 = iterator2.next();
            if (!MoreObjects.equal(o1, o2)) {
                return false;
            }
        }
        return !iterator2.hasNext();
    }

    /**
     * Returns a string representation of {@code iterator}, with the format
     * {@code [e1, e2, ..., en]}. The iterator will be left exhausted: its
     * {@code hasNext()} method will return {@code false}.
     */
    public static String toString(Iterator<?> iterator) {
        return MoreCollections.STANDARD_JOINER
                .appendTo(new StringBuilder().append('['), iterator)
                .append(']')
                .toString();
    }

    /**
     * Returns the single element contained in {@code iterator}.
     *
     * @throws NoSuchElementException   if the iterator is empty
     * @throws IllegalArgumentException if the iterator contains multiple
     *                                  elements.  The state of the iterator is unspecified.
     */
    public static <T> T getOnlyElement(Iterator<T> iterator) {
        T first = iterator.next();
        if (!iterator.hasNext()) {
            return first;
        }

        throw new IllegalArgumentException("expected one element but found multiple");
    }

    /**
     * Returns the single element contained in {@code iterator}, or {@code
     * defaultValue} if the iterator is empty.
     *
     * @throws IllegalArgumentException if the iterator contains multiple
     *                                  elements.  The state of the iterator is unspecified.
     */
    @Nullable
    public static <T> T getOnlyElement(Iterator<? extends T> iterator, @Nullable T defaultValue) {
        return iterator.hasNext() ? getOnlyElement(iterator) : defaultValue;
    }

    /**
     * Adds all elements in {@code iterator} to {@code collection}. The iterator
     * will be left exhausted: its {@code hasNext()} method will return
     * {@code false}.
     *
     * @return {@code true} if {@code collection} was modified as a result of this
     * operation
     */
    public static <T> boolean addAll(Collection<T> addTo, Iterator<? extends T> iterator) {
        if (addTo == null) {
            throw new NullPointerException();
        }
        if (iterator == null) {
            throw new NullPointerException();
        }
        boolean wasModified = false;
        while (iterator.hasNext()) {
            wasModified |= addTo.add(iterator.next());
        }
        return wasModified;
    }

    /**
     * Combines multiple iterators into a single iterator. The returned iterator
     * iterates across the elements of each iterator in {@code inputs}. The input
     * iterators are not polled until necessary.
     *
     * <p>The returned iterator supports {@code remove()} when the corresponding
     * input iterator supports it. The methods of the returned iterator may throw
     * {@code NullPointerException} if any of the input iterators is null.
     *
     * <p><b>Note:</b> the current implementation is not suitable for nested
     * concatenated iterators, i.e. the following should be avoided when in a loop:
     * {@code iterator = Iterators.concat(iterator, suffix);}, since iteration over the
     * resulting iterator has a cubic complexity to the depth of the nesting.
     */
    public static <T> Iterator<T> concat(final Iterator<? extends Iterator<? extends T>> inputs) {
        if (inputs == null) {
            throw new NullPointerException();
        }
        return new Iterator<T>() {
            Iterator<? extends T> current = emptyIterator();
            Iterator<? extends T> removeFrom;

            @Override
            public boolean hasNext() {
                // http://code.google.com/p/google-collections/issues/detail?id=151
                // current.hasNext() might be relatively expensive, worth minimizing.
                boolean currentHasNext;
                // checkNotNull eager for GWT
                // note: it must be here & not where 'current' is assigned,
                // because otherwise we'll have called inputs.next() before throwing
                // the first NPE, and the next time around we'll call inputs.next()
                // again, incorrectly moving beyond the error.
                if (current == null) {
                    throw new NullPointerException();
                }
                while (!(currentHasNext = ((Iterator<? extends Iterator<? extends T>>) current).hasNext())
                        && inputs.hasNext()) {
                    current = inputs.next();
                }
                return currentHasNext;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                removeFrom = current;
                return current.next();
            }

            @Override
            public void remove() {
                CollectPreconditions.checkRemove(removeFrom != null);
                removeFrom.remove();
                removeFrom = null;
            }
        };
    }

    /**
     * Divides an iterator into unmodifiable sublists of the given size (the final
     * list may be smaller). For example, partitioning an iterator containing
     * {@code [a, b, c, d, e]} with a partition size of 3 yields {@code
     * [[a, b, c], [d, e]]} -- an outer iterator containing two inner lists of
     * three and two elements, all in the original order.
     *
     * <p>The returned lists implement {@link java.util.RandomAccess}.
     *
     * @param iterator the iterator to return a partitioned view of
     * @param size     the desired size of each partition (the last may be smaller)
     * @return an iterator of immutable lists containing the elements of {@code
     * iterator} divided into partitions
     * @throws IllegalArgumentException if {@code size} is nonpositive
     */
    public static <T> UnmodifiableIterator<List<T>> partition(Iterator<T> iterator, int size) {
        return partitionImpl(iterator, size, false);
    }

    /**
     * Divides an iterator into unmodifiable sublists of the given size, padding
     * the final iterator with null values if necessary. For example, partitioning
     * an iterator containing {@code [a, b, c, d, e]} with a partition size of 3
     * yields {@code [[a, b, c], [d, e, null]]} -- an outer iterator containing
     * two inner lists of three elements each, all in the original order.
     *
     * <p>The returned lists implement {@link java.util.RandomAccess}.
     *
     * @param iterator the iterator to return a partitioned view of
     * @param size     the desired size of each partition
     * @return an iterator of immutable lists containing the elements of {@code
     * iterator} divided into partitions (the final iterable may have
     * trailing null elements)
     * @throws IllegalArgumentException if {@code size} is nonpositive
     */
    public static <T> UnmodifiableIterator<List<T>> paddedPartition(Iterator<T> iterator, int size) {
        return partitionImpl(iterator, size, true);
    }

    private static <T> UnmodifiableIterator<List<T>> partitionImpl(
            final Iterator<T> iterator, final int size, final boolean pad) {
        if (iterator == null) {
            throw new NullPointerException();
        }
        if (!(size > 0)) {
            throw new IllegalArgumentException();
        }
        return new UnmodifiableIterator<List<T>>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public List<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Object[] array = new Object[size];
                int count = 0;
                for (; count < size && iterator.hasNext(); count++) {
                    array[count] = iterator.next();
                }
                for (int i = count; i < size; i++) {
                    array[i] = null; // for GWT
                }

                @SuppressWarnings("unchecked") // we only put Ts in it
                        List<T> list = Collections.unmodifiableList(
                        (List<T>) Arrays.asList(array));
                return pad || count == size ? list : list.subList(0, count);
            }
        };
    }

    /**
     * Returns the elements of {@code unfiltered} that satisfy a predicate.
     */
    public static <T> UnmodifiableIterator<T> filter(
            final Iterator<T> unfiltered, final Predicate<? super T> predicate) {
        if (unfiltered == null) {
            throw new NullPointerException();
        }
        if (predicate == null) {
            throw new NullPointerException();
        }
        return new AbstractIterator<T>() {
            @Override
            protected T computeNext() {
                while (unfiltered.hasNext()) {
                    T element = unfiltered.next();
                    if (predicate.apply(element)) {
                        return element;
                    }
                }
                return endOfData();
            }
        };
    }

    /**
     * Returns all instances of class {@code type} in {@code unfiltered}. The
     * returned iterator has elements whose class is {@code type} or a subclass of
     * {@code type}.
     *
     * @param unfiltered an iterator containing objects of any type
     * @param type       the type of elements desired
     * @return an unmodifiable iterator containing all elements of the original
     * iterator that were of the requested type
     */
    @SuppressWarnings("unchecked") // can cast to <T> because non-Ts are removed
    public static <T> UnmodifiableIterator<T> filter(Iterator<?> unfiltered, Class<T> type) {
        return (UnmodifiableIterator<T>) filter(unfiltered, Predicates.instanceOf(type));
    }

    /**
     * Returns {@code true} if one or more elements returned by {@code iterator}
     * satisfy the given predicate.
     */
    public static <T> boolean any(Iterator<T> iterator, Predicate<? super T> predicate) {
        return indexOf(iterator, predicate) != -1;
    }

    /**
     * Returns {@code true} if every element returned by {@code iterator}
     * satisfies the given predicate. If {@code iterator} is empty, {@code true}
     * is returned.
     */
    public static <T> boolean all(Iterator<T> iterator, Predicate<? super T> predicate) {
        if (predicate == null) {
            throw new NullPointerException();
        }
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (!predicate.apply(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the first element in {@code iterator} that satisfies the given
     * predicate; use this method only when such an element is known to exist. If
     * no such element is found, the iterator will be left exhausted: its {@code
     * hasNext()} method will return {@code false}. If it is possible that
     * <i>no</i> element will match, use {@link #tryFind} or {@link
     * #find(Iterator, Predicate, Object)} instead.
     *
     * @throws NoSuchElementException if no element in {@code iterator} matches
     *                                the given predicate
     */
    public static <T> T find(Iterator<T> iterator, Predicate<? super T> predicate) {
        return filter(iterator, predicate).next();
    }

    /**
     * Returns the first element in {@code iterator} that satisfies the given
     * predicate. If no such element is found, {@code defaultValue} will be
     * returned from this method and the iterator will be left exhausted: its
     * {@code hasNext()} method will return {@code false}. Note that this can
     * usually be handled more naturally using {@code
     * tryFind(iterator, predicate).or(defaultValue)}.
     *
     * @since 7.0
     */
    @Nullable
    public static <T> T find(Iterator<? extends T> iterator, Predicate<? super T> predicate,
                             @Nullable T defaultValue) {
        return getNext(filter(iterator, predicate), defaultValue);
    }

    /**
     * Returns an {@link Optional} containing the first element in {@code
     * iterator} that satisfies the given predicate, if such an element exists. If
     * no such element is found, an empty {@link Optional} will be returned from
     * this method and the iterator will be left exhausted: its {@code
     * hasNext()} method will return {@code false}.
     *
     * <p><b>Warning:</b> avoid using a {@code predicate} that matches {@code
     * null}. If {@code null} is matched in {@code iterator}, a
     * NullPointerException will be thrown.
     *
     * @since 11.0
     */
    public static <T> Optional<T> tryFind(Iterator<T> iterator, Predicate<? super T> predicate) {
        UnmodifiableIterator<T> filteredIterator = filter(iterator, predicate);
        return filteredIterator.hasNext()
                ? Optional.of(filteredIterator.next())
                : Optional.<T>absent();
    }

    /**
     * Returns the index in {@code iterator} of the first element that satisfies
     * the provided {@code predicate}, or {@code -1} if the Iterator has no such
     * elements.
     *
     * <p>More formally, returns the lowest index {@code i} such that
     * {@code predicate.apply(Iterators.get(iterator, i))} returns {@code true},
     * or {@code -1} if there is no such index.
     *
     * <p>If -1 is returned, the iterator will be left exhausted: its
     * {@code hasNext()} method will return {@code false}.  Otherwise,
     * the iterator will be set to the element which satisfies the
     * {@code predicate}.
     *
     * @since 2.0
     */
    public static <T> int indexOf(Iterator<T> iterator, Predicate<? super T> predicate) {
        if (predicate == null) {
            throw new NullPointerException(String.valueOf("predicate"));
        }
        for (int i = 0; iterator.hasNext(); i++) {
            T current = iterator.next();
            if (predicate.apply(current)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns an iterator that applies {@code function} to each element of {@code
     * fromIterator}.
     *
     * <p>The returned iterator supports {@code remove()} if the provided iterator
     * does. After a successful {@code remove()} call, {@code fromIterator} no
     * longer contains the corresponding element.
     */
    public static <F, T> Iterator<T> transform(final Iterator<F> fromIterator,
                                               final Function<? super F, ? extends T> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        return new TransformedIterator<F, T>(fromIterator) {
            @Override
            T transform(F from) {
                return function.apply(from);
            }
        };
    }

    /**
     * Advances {@code iterator} {@code position + 1} times, returning the
     * element at the {@code position}th position.
     *
     * @param position position of the element to return
     * @return the element at the specified position in {@code iterator}
     * @throws IndexOutOfBoundsException if {@code position} is negative or
     *                                   greater than or equal to the number of elements remaining in
     *                                   {@code iterator}
     */
    public static <T> T get(Iterator<T> iterator, int position) {
        CollectPreconditions.checkIndexNonnegative(position);
        int skipped = advance(iterator, position);
        if (!iterator.hasNext()) {
            throw new IndexOutOfBoundsException("position (" + position
                    + ") must be less than the number of elements that remained ("
                    + skipped + ")");
        }
        return iterator.next();
    }

    /**
     * Advances {@code iterator} {@code position + 1} times, returning the
     * element at the {@code position}th position or {@code defaultValue}
     * otherwise.
     *
     * @param position     position of the element to return
     * @param defaultValue the default value to return if the iterator is empty
     *                     or if {@code position} is greater than the number of elements
     *                     remaining in {@code iterator}
     * @return the element at the specified position in {@code iterator} or
     * {@code defaultValue} if {@code iterator} produces fewer than
     * {@code position + 1} elements.
     * @throws IndexOutOfBoundsException if {@code position} is negative
     * @since 4.0
     */
    @Nullable
    public static <T> T get(Iterator<? extends T> iterator, int position, @Nullable T defaultValue) {
        CollectPreconditions.checkIndexNonnegative(position);
        advance(iterator, position);
        return getNext(iterator, defaultValue);
    }

    /**
     * Returns the next element in {@code iterator} or {@code defaultValue} if
     * the iterator is empty.  The {@link Iterables} analog to this method is
     * {@link Iterables#getFirst}.
     *
     * @param defaultValue the default value to return if the iterator is empty
     * @return the next element of {@code iterator} or the default value
     * @since 7.0
     */
    @Nullable
    public static <T> T getNext(Iterator<? extends T> iterator, @Nullable T defaultValue) {
        return iterator.hasNext() ? iterator.next() : defaultValue;
    }

    /**
     * Advances {@code iterator} to the end, returning the last element.
     *
     * @return the last element of {@code iterator}
     * @throws NoSuchElementException if the iterator is empty
     */
    public static <T> T getLast(Iterator<T> iterator) {
        while (true) {
            T current = iterator.next();
            if (!iterator.hasNext()) {
                return current;
            }
        }
    }

    /**
     * Advances {@code iterator} to the end, returning the last element or
     * {@code defaultValue} if the iterator is empty.
     *
     * @param defaultValue the default value to return if the iterator is empty
     * @return the last element of {@code iterator}
     * @since 3.0
     */
    @Nullable
    public static <T> T getLast(Iterator<? extends T> iterator, @Nullable T defaultValue) {
        return iterator.hasNext() ? getLast(iterator) : defaultValue;
    }

    /**
     * Calls {@code next()} on {@code iterator}, either {@code numberToAdvance} times
     * or until {@code hasNext()} returns {@code false}, whichever comes first.
     *
     * @return the number of elements the iterator was advanced
     * @since 13.0 (since 3.0 as {@code Iterators.skip})
     */
    public static int advance(Iterator<?> iterator, int numberToAdvance) {
        if (iterator == null) {
            throw new NullPointerException();
        }
        if (!(numberToAdvance >= 0)) {
            throw new IllegalArgumentException(String.valueOf("numberToAdvance must be nonnegative"));
        }

        int i;
        for (i = 0; i < numberToAdvance && iterator.hasNext(); i++) {
            iterator.next();
        }
        return i;
    }

    // Methods only in Iterators, not in Iterables

    /**
     * Clears the iterator using its remove method.
     */
    static void clear(Iterator<?> iterator) {
        if (iterator == null) {
            throw new NullPointerException();
        }
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    /**
     * Returns a {@code PeekingIterator} backed by the given iterator.
     *
     * <p>Calls to the {@code peek} method with no intervening calls to {@code
     * next} do not affect the iteration, and hence return the same object each
     * time. A subsequent call to {@code next} is guaranteed to return the same
     * object again. For example: <pre>   {@code
     *
     *   PeekingIterator<String> peekingIterator =
     *       Iterators.peekingIterator(Iterators.forArray("a", "b"));
     *   String a1 = peekingIterator.peek(); // returns "a"
     *   String a2 = peekingIterator.peek(); // also returns "a"
     *   String a3 = peekingIterator.next(); // also returns "a"}</pre>
     *
     * <p>Any structural changes to the underlying iteration (aside from those
     * performed by the iterator's own {@link PeekingIterator#remove()} method)
     * will leave the iterator in an undefined state.
     *
     * <p>The returned iterator does not support removal after peeking, as
     * explained by {@link PeekingIterator#remove()}.
     *
     * <p>Note: If the given iterator is already a {@code PeekingIterator},
     * it <i>might</i> be returned to the caller, although this is neither
     * guaranteed to occur nor required to be consistent.  For example, this
     * method <i>might</i> choose to pass through recognized implementations of
     * {@code PeekingIterator} when the behavior of the implementation is
     * known to meet the contract guaranteed by this method.
     *
     * <p>There is no {@link Iterable} equivalent to this method, so use this
     * method to wrap each individual iterator as it is generated.
     *
     * @param iterator the backing iterator. The {@link PeekingIterator} assumes
     *                 ownership of this iterator, so users should cease making direct calls
     *                 to it after calling this method.
     * @return a peeking iterator backed by that iterator. Apart from the
     * additional {@link PeekingIterator#peek()} method, this iterator behaves
     * exactly the same as {@code iterator}.
     */
    public static <T> PeekingIterator<T> peekingIterator(
            Iterator<? extends T> iterator) {
        if (iterator instanceof PeekingImpl) {
            // Safe to cast <? extends T> to <T> because PeekingImpl only uses T
            // covariantly (and cannot be subclassed to add non-covariant uses).
            return (PeekingImpl<T>) iterator;
        }
        return new PeekingImpl<>(iterator);
    }

    /**
     * Implementation of PeekingIterator that avoids peeking unless necessary.
     */
    private static class PeekingImpl<E> implements PeekingIterator<E> {

        private final Iterator<? extends E> iterator;
        private boolean hasPeeked;
        private E peekedElement;

        public PeekingImpl(Iterator<? extends E> iterator) {
            if (iterator == null) {
                throw new NullPointerException();
            }
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return hasPeeked || iterator.hasNext();
        }

        @Override
        public E next() {
            if (!hasPeeked) {
                return iterator.next();
            }
            E result = peekedElement;
            hasPeeked = false;
            peekedElement = null;
            return result;
        }

        @Override
        public void remove() {
            boolean expression = !hasPeeked;
            if (!expression) {
                throw new IllegalStateException(String.valueOf("Can't remove after you've peeked at next"));
            }
            iterator.remove();
        }

        @Override
        public E peek() {
            if (!hasPeeked) {
                peekedElement = iterator.next();
                hasPeeked = true;
            }
            return peekedElement;
        }
    }
}
