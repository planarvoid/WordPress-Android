package com.soundcloud.java.collections;

import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.functions.Predicates;
import com.soundcloud.java.primitives.Ints;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class Sets {

    /**
     * Creates a <i>mutable</i> {@code HashSet} instance containing the given
     * elements in unspecified order.
     *
     * <p><b>Note:</b> if {@code E} is an {@link Enum} type, use {@link
     * EnumSet#of(Enum, Enum[])} instead.
     *
     * @param elements the elements that the set should contain
     * @return a new {@code HashSet} containing those elements (minus duplicates)
     */
    @SuppressWarnings("PMD.LooseCoupling") // we want the concrete type
    public static <E> HashSet<E> newHashSet(E... elements) {
        HashSet<E> set = newHashSetWithExpectedSize(elements.length);
        Collections.addAll(set, elements);
        return set;
    }

    /**
     * Creates a {@code HashSet} instance, with a high enough "initial capacity"
     * that it <i>should</i> hold {@code expectedSize} elements without growth.
     * This behavior cannot be broadly guaranteed, but it is observed to be true
     * for OpenJDK 1.6. It also can't be guaranteed that the method isn't
     * inadvertently <i>oversizing</i> the returned set.
     *
     * @param expectedSize the number of elements you expect to add to the
     *        returned set
     * @return a new, empty {@code HashSet} with enough capacity to hold {@code
     *         expectedSize} elements without resizing
     * @throws IllegalArgumentException if {@code expectedSize} is negative
     */
    @SuppressWarnings("PMD.LooseCoupling") // we want the concrete type
    public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
        return new HashSet<>(capacity(expectedSize));
    }

    /**
     * Returns a capacity that is sufficient to keep the set from being resized as
     * long as it grows no larger than expectedSize and the load factor is >= its
     * default (0.75).
     */
    static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
            return expectedSize + 1;
        }
        if (expectedSize < Ints.MAX_POWER_OF_TWO) {
            return expectedSize + expectedSize / 3;
        }
        return Integer.MAX_VALUE; // any large value
    }

    /**
     * Creates a <i>mutable</i> {@code HashSet} instance containing the given
     * elements in unspecified order.
     *
     * <p><b>Note:</b> if {@code E} is an {@link Enum} type, you should create an
     * {@link EnumSet} instead.
     *
     * @param elements the elements that the set should contain
     * @return a new {@code HashSet} containing those elements (minus duplicates)
     */
    @SuppressWarnings("PMD.LooseCoupling") // we want the concrete type
    public static <E> HashSet<E> newHashSet(Iterable<? extends E> elements) {
        return elements instanceof Collection
                ? new HashSet<>(MoreCollections.cast(elements))
                : newHashSet(elements.iterator());
    }

    /**
     * Creates a <i>mutable</i> {@code HashSet} instance containing the given
     * elements in unspecified order.
     *
     * <p><b>Note:</b> if {@code E} is an {@link Enum} type, you should create an
     * {@link EnumSet} instead.
     *
     * @param elements the elements that the set should contain
     * @return a new {@code HashSet} containing those elements (minus duplicates)
     */
    @SuppressWarnings("PMD.LooseCoupling") // we want the concrete type
    public static <E> HashSet<E> newHashSet(Iterator<? extends E> elements) {
        HashSet<E> set = new HashSet<>();
        Iterators.addAll(set, elements);
        return set;
    }

    /**
     * Creates a <i>mutable</i> {@code TreeSet} instance containing the given
     * elements sorted by their natural ordering.
     *
     * <p><b>Note:</b> If {@code elements} is a {@code SortedSet} with an explicit
     * comparator, this method has different behavior than
     * {@link TreeSet#TreeSet(SortedSet)}, which returns a {@code TreeSet} with
     * that comparator.
     *
     * @param elements the elements that the set should contain
     * @return a new {@code TreeSet} containing those elements (minus duplicates)
     */
    @SuppressWarnings("PMD.LooseCoupling") // we want the concrete type
    public static <E extends Comparable> TreeSet<E> newTreeSet(Iterable<? extends E> elements) {
        TreeSet<E> set = new TreeSet<>();
        Iterables.addAll(set, elements);
        return set;
    }

    /**
     * Returns the elements of {@code unfiltered} that satisfy a predicate. The
     * returned set is a live view of {@code unfiltered}; changes to one affect
     * the other.
     *
     * <p>The resulting set's iterator does not support {@code remove()}, but all
     * other set methods are supported. When given an element that doesn't satisfy
     * the predicate, the set's {@code add()} and {@code addAll()} methods throw
     * an {@link IllegalArgumentException}. When methods such as {@code
     * removeAll()} and {@code clear()} are called on the filtered set, only
     * elements that satisfy the filter will be removed from the underlying set.
     *
     * <p>The returned set isn't threadsafe or serializable, even if
     * {@code unfiltered} is.
     *
     * <p>Many of the filtered set's methods, such as {@code size()}, iterate
     * across every element in the underlying set and determine which elements
     * satisfy the filter. When a live view is <i>not</i> needed, it may be faster
     * to copy {@code Iterables.filter(unfiltered, predicate)} and use the copy.
     *
     * <p><b>Warning:</b> {@code predicate} must be <i>consistent with equals</i>,
     * as documented at {@link Predicate#apply}. Do not provide a predicate such
     * as {@code Predicates.instanceOf(ArrayList.class)}, which is inconsistent
     * with equals. (See {@link Iterables#filter(Iterable, Class)} for related
     * functionality.)
     */
    // TODO(kevinb): how to omit that last sentence when building GWT javadoc?
    public static <E> Set<E> filter(Set<E> unfiltered, Predicate<? super E> predicate) {
        if (unfiltered instanceof SortedSet) {
            return filter((SortedSet<E>) unfiltered, predicate);
        }
        if (unfiltered instanceof FilteredSet) {
            // Support clear(), removeAll(), and retainAll() when filtering a filtered
            // collection.
            FilteredSet<E> filtered = (FilteredSet<E>) unfiltered;
            Predicate<E> combinedPredicate = Predicates.and(filtered.predicate, predicate);
            return new FilteredSet<>((Set<E>) filtered.unfiltered, combinedPredicate);
        }

        if (predicate == null) {
            throw new NullPointerException();
        }
        if (unfiltered == null) {
            throw new NullPointerException();
        }
        return new FilteredSet<>(unfiltered, predicate);
    }

    /**
     * An implementation for {@link Set#hashCode()}.
     */
    static int hashCodeImpl(Set<?> s) {
        int hashCode = 0;
        for (Object o : s) {
            if (o != null) {
                hashCode += o.hashCode();
            }
        }
        return hashCode;
    }

    /**
     * An implementation for {@link Set#equals(Object)}.
     */
    @SuppressWarnings("PMD.AvoidCatchingNPE")
    static boolean equalsImpl(Set<?> s, @Nullable Object object) {
        if (s == object) {
            return true;
        }
        if (object instanceof Set) {
            Set<?> o = (Set<?>) object;

            try {
                return s.size() == o.size() && s.containsAll(o);
            } catch (NullPointerException ignored) {
                return false;
            } catch (ClassCastException ignored) {
                return false;
            }
        }
        return false;
    }

    /**
     * Returns the elements of a {@code SortedSet}, {@code unfiltered}, that
     * satisfy a predicate. The returned set is a live view of {@code unfiltered};
     * changes to one affect the other.
     *
     * <p>The resulting set's iterator does not support {@code remove()}, but all
     * other set methods are supported. When given an element that doesn't satisfy
     * the predicate, the set's {@code add()} and {@code addAll()} methods throw
     * an {@link IllegalArgumentException}. When methods such as
     * {@code removeAll()} and {@code clear()} are called on the filtered set,
     * only elements that satisfy the filter will be removed from the underlying
     * set.
     *
     * <p>The returned set isn't threadsafe or serializable, even if
     * {@code unfiltered} is.
     *
     * <p>Many of the filtered set's methods, such as {@code size()}, iterate across
     * every element in the underlying set and determine which elements satisfy
     * the filter. When a live view is <i>not</i> needed, it may be faster to copy
     * {@code Iterables.filter(unfiltered, predicate)} and use the copy.
     *
     * <p><b>Warning:</b> {@code predicate} must be <i>consistent with equals</i>,
     * as documented at {@link Predicate#apply}. Do not provide a predicate such as
     * {@code Predicates.instanceOf(ArrayList.class)}, which is inconsistent with
     * equals. (See {@link Iterables#filter(Iterable, Class)} for related
     * functionality.)
     *
     * @since 11.0
     */
    public static <E> SortedSet<E> filter(SortedSet<E> unfiltered, Predicate<? super E> predicate) {
        return unfiltered instanceof NavigableSet
                ? Sets.filter((NavigableSet<E>) unfiltered, predicate)
                : Sets.filterSortedIgnoreNavigable(unfiltered, predicate);
    }

    static <E> SortedSet<E> filterSortedIgnoreNavigable(
            SortedSet<E> unfiltered, Predicate<? super E> predicate) {
        if (unfiltered instanceof FilteredSet) {
            // Support clear(), removeAll(), and retainAll() when filtering a filtered
            // collection.
            FilteredSet<E> filtered = (FilteredSet<E>) unfiltered;
            Predicate<E> combinedPredicate
                    = Predicates.and(filtered.predicate, predicate);
            return new FilteredSortedSet<>((SortedSet<E>) filtered.unfiltered, combinedPredicate);
        }

        if (predicate == null) {
            throw new NullPointerException();
        }
        if (unfiltered == null) {
            throw new NullPointerException();
        }
        return new FilteredSortedSet<>(unfiltered, predicate);
    }

    /**
     * Returns the elements of a {@code NavigableSet}, {@code unfiltered}, that
     * satisfy a predicate. The returned set is a live view of {@code unfiltered};
     * changes to one affect the other.
     *
     * <p>The resulting set's iterator does not support {@code remove()}, but all
     * other set methods are supported. When given an element that doesn't satisfy
     * the predicate, the set's {@code add()} and {@code addAll()} methods throw
     * an {@link IllegalArgumentException}. When methods such as
     * {@code removeAll()} and {@code clear()} are called on the filtered set,
     * only elements that satisfy the filter will be removed from the underlying
     * set.
     *
     * <p>The returned set isn't threadsafe or serializable, even if
     * {@code unfiltered} is.
     *
     * <p>Many of the filtered set's methods, such as {@code size()}, iterate across
     * every element in the underlying set and determine which elements satisfy
     * the filter. When a live view is <i>not</i> needed, it may be faster to copy
     * {@code Iterables.filter(unfiltered, predicate)} and use the copy.
     *
     * <p><b>Warning:</b> {@code predicate} must be <i>consistent with equals</i>,
     * as documented at {@link Predicate#apply}. Do not provide a predicate such as
     * {@code Predicates.instanceOf(ArrayList.class)}, which is inconsistent with
     * equals. (See {@link Iterables#filter(Iterable, Class)} for related
     * functionality.)
     *
     * @since 14.0
     */
    @SuppressWarnings("unchecked")
    public static <E> NavigableSet<E> filter(NavigableSet<E> unfiltered, Predicate<? super E> predicate) {
        if (unfiltered instanceof FilteredSet) {
            // Support clear(), removeAll(), and retainAll() when filtering a filtered
            // collection.
            FilteredSet<E> filtered = (FilteredSet<E>) unfiltered;
            Predicate<E> combinedPredicate = Predicates.and(filtered.predicate, predicate);
            return new FilteredNavigableSet<>((NavigableSet<E>) filtered.unfiltered, combinedPredicate);
        }

        if (predicate == null) {
            throw new NullPointerException();
        }
        if (unfiltered == null) {
            throw new NullPointerException();
        }
        return new FilteredNavigableSet<>(unfiltered, predicate);
    }


    private Sets() {
        // no instances
    }
}
