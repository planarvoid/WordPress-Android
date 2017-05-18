package com.soundcloud.java.collections;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.soundcloud.java.functions.Function;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

public class ListsTest {

    private static final Collection<Integer> SOME_COLLECTION = asList(0, 1, 1);
    private static final Iterable<Integer> SOME_ITERABLE = new SomeIterable();

    private List<Integer> randomAccessIntList = new ArrayList<>(asList(1, 2, 3, 4));
    private List<Integer> sequentialIntList = new LinkedList<>(asList(1, 2, 3, 4));

    private List<String> stringList = asList("1", "2", "3", "4");

    private Function<Number, String> testFunction = new SomeFunction();

    @Test
    public void testNewArrayListFromEllipsis() {
        ArrayList<Integer> list = Lists.newArrayList(0, 1, 1);
        assertEquals(SOME_COLLECTION, list);
    }

    @Test
    public void testNewArrayListFromCollection() {
        ArrayList<Integer> list = Lists.newArrayList(SOME_COLLECTION);
        assertEquals(SOME_COLLECTION, list);
    }

    @Test
    public void testNewArrayListFromIterable() {
        ArrayList<Integer> list = Lists.newArrayList(SOME_ITERABLE);
        assertEquals(SOME_COLLECTION, list);
    }

    @Test
    public void testNewArrayListFromIterator() {
        ArrayList<Integer> list = Lists.newArrayList(SOME_COLLECTION.iterator());
        assertEquals(SOME_COLLECTION, list);
    }

    @Test
    public void testNewArrayListFromPrimitiveIntArray() {
        ArrayList<Integer> list = Lists.newArrayList(new int[]{1, 2, 3});
        assertEquals(asList(1, 2, 3), list);
    }

    @Test
    public void testNewLinkedListFromCollection() {
        LinkedList<Integer> list = Lists.newLinkedList(SOME_COLLECTION);
        assertEquals(SOME_COLLECTION, list);
    }

    @Test
    public void testNewLinkedListFromIterable() {
        LinkedList<Integer> list = Lists.newLinkedList(SOME_ITERABLE);
        assertEquals(SOME_COLLECTION, list);
    }

    @Test
    public void testReverseViewRandomAccess() {
        List<Integer> fromList = randomAccessIntList;
        List<Integer> toList = Lists.reverse(fromList);
        assertListReversed(fromList, toList);

    }

    private void assertListReversed(List<Integer> fromList, List<Integer> toList) {
    /* fromList modifications reflected in toList */
        fromList.set(0, 5);
        assertThat(asList(4, 3, 2, 5)).isEqualTo(toList);
        fromList.add(6);
        assertThat(asList(6, 4, 3, 2, 5)).isEqualTo(toList);
        fromList.add(2, 9);
        assertThat(asList(6, 4, 3, 9, 2, 5)).isEqualTo(toList);
        fromList.remove(Integer.valueOf(2));
        assertThat(asList(6, 4, 3, 9, 5)).isEqualTo(toList);
        fromList.remove(3);
        assertThat(asList(6, 3, 9, 5)).isEqualTo(toList);

    /* toList modifications reflected in fromList */
        toList.remove(0);
        assertThat(asList(5, 9, 3)).isEqualTo(fromList);
        toList.add(7);
        assertThat(asList(7, 5, 9, 3)).isEqualTo(fromList);
        toList.add(5);
        assertThat(asList(5, 7, 5, 9, 3)).isEqualTo(fromList);
        toList.remove(Integer.valueOf(5));
        assertThat(asList(5, 7, 9, 3)).isEqualTo(fromList);
        toList.set(1, 8);
        assertThat(asList(5, 7, 8, 3)).isEqualTo(fromList);
        toList.clear();
        assertThat(Collections.emptyList()).isEqualTo(fromList);
    }

    @Test
    public void testReverseViewSequential() {
        List<Integer> fromList = sequentialIntList;
        List<Integer> toList = Lists.reverse(fromList);
        /* fromList modifications reflected in toList */
        assertListReversed(fromList, toList);
    }

    @Test
    public void testTransformHashCodeRandomAccess() {
        List<String> list = Lists.transform(randomAccessIntList, testFunction);
        assertThat(stringList.hashCode()).isEqualTo(list.hashCode());
    }

    @Test
    public void testTransformHashCodeSequential() {
        List<String> list = Lists.transform(sequentialIntList, testFunction);
        assertThat(stringList.hashCode()).isEqualTo(list.hashCode());
    }

    @Test
    public void testTransformModifiableRandomAccess() {
        List<Integer> fromList = randomAccessIntList;
        List<String> list = Lists.transform(fromList, testFunction);
        assertTransformModifiable(list);
    }

    @Test
    public void testTransformModifiableSequential() {
        List<Integer> fromList = sequentialIntList;
        List<String> list = Lists.transform(fromList, testFunction);
        assertTransformModifiable(list);
    }

    private static void assertTransformModifiable(List<String> list) {
        try {
            list.add("5");
            fail("transformed list is addable");
        } catch (UnsupportedOperationException ignored) {
        }
        list.remove(0);
        assertThat(asList("2", "3", "4")).isEqualTo(list);
        list.remove("3");
        assertThat(asList("2", "4")).isEqualTo(list);
        try {
            list.set(0, "5");
            fail("transformed list is setable");
        } catch (UnsupportedOperationException ignored) {
        }
        list.clear();
        assertThat(Collections.emptyList()).isEqualTo(list);
    }

    @Test
    public void testTransformViewRandomAccess() {
        List<Integer> fromList = randomAccessIntList;
        List<String> toList = Lists.transform(fromList, testFunction);
        assertTransformView(fromList, toList);
    }

    @Test
    public void testTransformViewSequential() {
        List<Integer> fromList = sequentialIntList;
        List<String> toList = Lists.transform(fromList, testFunction);
        assertTransformView(fromList, toList);
    }

    private static void assertTransformView(List<Integer> fromList,
                                            List<String> toList) {
    /* fromList modifications reflected in toList */
        fromList.set(0, 5);
        assertThat(asList("5", "2", "3", "4")).isEqualTo(toList);
        fromList.add(6);
        assertThat(asList("5", "2", "3", "4", "6")).isEqualTo(toList);
        fromList.remove(Integer.valueOf(2));
        assertThat(asList("5", "3", "4", "6")).isEqualTo(toList);
        fromList.remove(2);
        assertThat(asList("5", "3", "6")).isEqualTo(toList);

    /* toList modifications reflected in fromList */
        toList.remove(2);
        assertThat(asList(5, 3)).isEqualTo(fromList);
        toList.remove("5");
        assertThat(asList(3)).isEqualTo(fromList);
        toList.clear();
        assertThat(Collections.emptyList()).isEqualTo(fromList);
    }

    @Test
    public void testTransformRandomAccess() {
        List<String> list = Lists.transform(randomAccessIntList, testFunction);
        assertThat(list).isInstanceOf(RandomAccess.class);
    }

    @Test
    public void testTransformSequential() {
        List<String> list = Lists.transform(sequentialIntList, testFunction);
        assertThat(list).isNotInstanceOf(RandomAccess.class);
    }

    @Test
    public void testTransformListIteratorRandomAccess() {
        List<Integer> fromList = randomAccessIntList;
        List<String> list = Lists.transform(fromList, testFunction);
        assertTransformListIterator(list);
    }

    @Test
    public void testTransformListIteratorSequential() {
        List<Integer> fromList = sequentialIntList;
        List<String> list = Lists.transform(fromList, testFunction);
        assertTransformListIterator(list);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testTransformPreservesIOOBEsThrownByFunction() {
        Lists.transform(Arrays.asList("foo", "bar"), new Function<String, String>() {
            @Override
            public String apply(String input) {
                throw new IndexOutOfBoundsException();
            }
        }).toArray();
    }

    private static void assertTransformListIterator(List<String> list) {
        ListIterator<String> iterator = list.listIterator(1);
        assertThat(1).isEqualTo(iterator.nextIndex());
        assertThat("2").isEqualTo(iterator.next());
        assertThat("3").isEqualTo(iterator.next());
        assertThat("4").isEqualTo(iterator.next());
        assertThat(4).isEqualTo(iterator.nextIndex());
        try {
            iterator.next();
            fail("did not detect end of list");
        } catch (NoSuchElementException ignored) {
        }
        assertThat(3).isEqualTo(iterator.previousIndex());
        assertThat("4").isEqualTo(iterator.previous());
        assertThat("3").isEqualTo(iterator.previous());
        assertThat("2").isEqualTo(iterator.previous());
        assertThat(iterator.hasPrevious()).isTrue();
        assertThat("1").isEqualTo(iterator.previous());
        assertThat(iterator.hasPrevious()).isFalse();
        assertThat(-1).isEqualTo(iterator.previousIndex());
        try {
            iterator.previous();
            fail("did not detect beginning of list");
        } catch (NoSuchElementException ignored) {
        }
        iterator.remove();
        assertThat(asList("2", "3", "4")).isEqualTo(list);
        assertThat(list.isEmpty()).isFalse();

        // An UnsupportedOperationException or IllegalStateException may occur.
        try {
            iterator.add("1");
            fail("transformed list iterator is addable");
        } catch (UnsupportedOperationException | IllegalStateException ignored) {
        }
        try {
            iterator.set("1");
            fail("transformed list iterator is settable");
        } catch (UnsupportedOperationException | IllegalStateException ignored) {
        }
    }

    @Test
    public void testTransformIteratorRandomAccess() {
        List<Integer> fromList = randomAccessIntList;
        List<String> list = Lists.transform(fromList, testFunction);
        assertTransformIterator(list);
    }

    @Test
    public void testTransformIteratorSequential() {
        List<Integer> fromList = sequentialIntList;
        List<String> list = Lists.transform(fromList, testFunction);
        assertTransformIterator(list);
    }

    private static void assertTransformIterator(List<String> list) {
        Iterator<String> iterator = list.iterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat("1").isEqualTo(iterator.next());
        assertThat(iterator.hasNext()).isTrue();
        assertThat("2").isEqualTo(iterator.next());
        assertThat(iterator.hasNext()).isTrue();
        assertThat("3").isEqualTo(iterator.next());
        assertThat(iterator.hasNext()).isTrue();
        assertThat("4").isEqualTo(iterator.next());
        assertThat(iterator.hasNext()).isFalse();
        try {
            iterator.next();
            fail("did not detect end of list");
        } catch (NoSuchElementException ignored) {
        }
        iterator.remove();
        assertThat(asList("1", "2", "3")).isEqualTo(list);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPartition_badSize() {
        List<Integer> source = Collections.singletonList(1);
        Lists.partition(source, 0);
    }

    @Test
    public void testPartition_empty() {
        List<Integer> source = Collections.emptyList();
        List<List<Integer>> partitions = Lists.partition(source, 1);
        assertTrue(partitions.isEmpty());
        assertEquals(0, partitions.size());
    }

    @Test
    public void testPartition_1_1() {
        List<Integer> source = Collections.singletonList(1);
        List<List<Integer>> partitions = Lists.partition(source, 1);
        assertEquals(1, partitions.size());
        assertEquals(Collections.singletonList(1), partitions.get(0));
    }

    @Test
    public void testPartition_1_2() {
        List<Integer> source = Collections.singletonList(1);
        List<List<Integer>> partitions = Lists.partition(source, 2);
        assertEquals(1, partitions.size());
        assertEquals(Collections.singletonList(1), partitions.get(0));
    }

    @Test
    public void testPartition_2_1() {
        List<Integer> source = asList(1, 2);
        List<List<Integer>> partitions = Lists.partition(source, 1);
        assertEquals(2, partitions.size());
        assertEquals(Collections.singletonList(1), partitions.get(0));
        assertEquals(Collections.singletonList(2), partitions.get(1));
    }

    @Test
    public void testPartition_3_2() {
        List<Integer> source = asList(1, 2, 3);
        List<List<Integer>> partitions = Lists.partition(source, 2);
        assertEquals(2, partitions.size());
        assertEquals(asList(1, 2), partitions.get(0));
        assertEquals(asList(3), partitions.get(1));
    }

    @Test
    public void testPartitionRandomAccessTrue() {
        List<Integer> source = asList(1, 2, 3);
        List<List<Integer>> partitions = Lists.partition(source, 2);

        assertTrue("partition should be RandomAccess, but not: "
                        + partitions.getClass(),
                partitions instanceof RandomAccess);

        assertTrue("partition[0] should be RandomAccess, but not: "
                        + partitions.get(0).getClass(),
                partitions.get(0) instanceof RandomAccess);

        assertTrue("partition[1] should be RandomAccess, but not: "
                        + partitions.get(1).getClass(),
                partitions.get(1) instanceof RandomAccess);
    }

    @Test
    public void testPartitionRandomAccessFalse() {
        List<Integer> source = new LinkedList<>(asList(1, 2, 3));
        List<List<Integer>> partitions = Lists.partition(source, 2);
        assertFalse(partitions instanceof RandomAccess);
        assertFalse(partitions.get(0) instanceof RandomAccess);
        assertFalse(partitions.get(1) instanceof RandomAccess);
    }

    @Test
    public void testPartition_view() {
        List<Integer> list = asList(1, 2, 3);
        List<List<Integer>> partitions = Lists.partition(list, 3);

        // Changes before the partition is retrieved are reflected
        list.set(0, 3);

        Iterator<List<Integer>> iterator = partitions.iterator();

        // Changes before the partition is retrieved are reflected
        list.set(1, 4);

        List<Integer> first = iterator.next();

        // Changes after are too (unlike Iterables.partition)
        list.set(2, 5);

        assertEquals(asList(3, 4, 5), first);

        // Changes to a sublist also write through to the original list
        first.set(1, 6);
        assertEquals(asList(3, 6, 5), list);
    }

    @Test
    public void testPartitionSize_1() {
        List<Integer> list = asList(1, 2, 3);
        assertEquals(1, Lists.partition(list, Integer.MAX_VALUE).size());
        assertEquals(1, Lists.partition(list, Integer.MAX_VALUE - 1).size());
    }

    @Test
    public void testPartitionSize_2() {
        assertEquals(2, Lists.partition(Collections.nCopies(0x40000001, 1), 0x40000000).size());
    }

    private static class SomeIterable implements Iterable<Integer>, Serializable {
        @Override
        public Iterator<Integer> iterator() {
            return SOME_COLLECTION.iterator();
        }

        private static final long serialVersionUID = 0;
    }

    private static class SomeFunction
            implements Function<Number, String>, Serializable {
        @Override
        public String apply(Number n) {
            return String.valueOf(n);
        }

        private static final long serialVersionUID = 0;
    }
}
