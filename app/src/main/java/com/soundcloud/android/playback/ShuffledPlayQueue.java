package com.soundcloud.android.playback;

import com.soundcloud.java.objects.MoreObjects;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

abstract class ShuffledPlayQueue extends SimplePlayQueue {

    private ShuffledPlayQueue(ShuffledList<PlayQueueItem> playQueueItems) {
        super(playQueueItems);
    }

    abstract PlayQueue unshuffle();

    static ShuffledPlayQueue from(final PlayQueue playQueue, int start) {
        return new ShuffledPlayQueue(new ShuffledPlayQueue.ShuffledList<>(playQueue.items(), start)) {
            @Override
            PlayQueue unshuffle() {
                return playQueue;
            }
        };
    }

    @Override
    boolean isShuffled() {
        return true;
    }

    // This is a shuffled view of a list.
    static class ShuffledList<T> implements List<T> {

        private final List<T> actualList;
        private final List<Integer> shuffled2Actual;

        ShuffledList(List<T> actualList, int start) {
            this(actualList, createIndicesMapping(start, actualList.size()));
        }

        ShuffledList(List<T> actualList, List<Integer> shuffledIndices) {
            this.actualList = actualList;
            this.shuffled2Actual = shuffledIndices;
        }

        private static List<Integer> createIndicesMapping(int start, int end) {
            final List<Integer> indices = new ArrayList<>(end);

            for (int i = 0; i < end; i++) {
                indices.add(i, i);
            }

            if (start != end) {
                // do not shuffle when the pivot is at the end.
                Collections.shuffle(indices.subList(start, end));
            }

            return indices;
        }

        private int getActualIndex(int shuffled) {
            return shuffled2Actual.get(shuffled);
        }

        private void addIndex(int actual, int shuffledIndex) {
            shuffled2Actual.add(shuffledIndex, actual);
        }

        private void removeIndex(int shuffled) {
            final Integer removed = shuffled2Actual.remove(shuffled);
            for (int i = 0; i < shuffled2Actual.size(); i++) {
                final Integer actual = shuffled2Actual.get(i);
                if (actual >= removed) {
                    shuffled2Actual.set(i, actual - 1);
                }
            }
        }

        @NonNull
        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                private int index = -1;

                @Override
                public boolean hasNext() {
                    return ++index < size();
                }

                @Override
                public T next() {
                    if (index < size()) {
                        return get(index);
                    } else {
                        return null;
                    }
                }

                @Override
                public void remove() {
                    if (index < size()) {
                        ShuffledList.this.remove(index);
                    }
                }
            };
        }

        @Override
        public boolean add(T object) {
            add(size(), object);
            return true;
        }

        @Override
        public boolean addAll(int location, Collection<? extends T> collection) {
            final Iterator<? extends T> iterator = collection.iterator();
            for (int i = 0; iterator.hasNext(); i++) {
                add(location + i, iterator.next());
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends T> collection) {
            for (T t : collection) {
                add(t);
            }
            return true;
        }

        @Override
        public void clear() {
            actualList.clear();
            shuffled2Actual.clear();
        }

        @Override
        public boolean contains(Object object) {
            return actualList.contains(object);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return actualList.containsAll(collection);
        }

        @Override
        public void add(int shuffledIndex, T object) {
            final int actual = size();
            addIndex(actual, shuffledIndex);

            actualList.add(getActualIndex(shuffledIndex), object);
        }

        @Override
        public T get(int location) {
            return actualList.get(getActualIndex(location));
        }

        @Override
        public boolean remove(Object object) {
            remove(indexOf(object));
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            for (Object o : collection) {
                remove(indexOf(o));
            }
            return true;
        }

        @Override
        public T remove(int location) {
            final int index = getActualIndex(location);
            removeIndex(location);
            return actualList.remove(index);
        }

        @Override
        public int indexOf(Object object) {
            final int actual = actualList.indexOf(object);
            for (int i = 0; i < shuffled2Actual.size(); i++) {
                if (actual == shuffled2Actual.get(i)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean isEmpty() {
            return actualList.isEmpty();
        }

        @Override
        public int size() {
            return actualList.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ShuffledList<?> that = (ShuffledList<?>) o;
            return MoreObjects.equal(actualList, that.actualList) &&
                    MoreObjects.equal(shuffled2Actual, that.shuffled2Actual);
        }

        @Override
        public int hashCode() {
            return MoreObjects.hashCode(actualList, shuffled2Actual);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int lastIndexOf(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<T> listIterator() {
            throw new UnsupportedOperationException();
        }

        @NonNull
        @Override
        public ListIterator<T> listIterator(int location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T set(int location, T object) {
            throw new UnsupportedOperationException();
        }

        @NonNull
        @Override
        public List<T> subList(int start, int end) {
            final ArrayList<T> subList = new ArrayList<>(end - start);
            for (int i = start; i < end; i++) {
                subList.add(get(i));
            }
            return subList;
        }

        @Override
        public Object[] toArray() {
            final Object[] actual = actualList.toArray();
            final Object[] shuffled = new Object[actual.length];

            for (int i = 0; i < shuffled.length; i++) {
                shuffled[i] = actual[getActualIndex(i)];
            }

            return shuffled;
        }

        @NonNull
        @Override
        public <T1> T1[] toArray(T1[] array) {
            throw new UnsupportedOperationException();
        }

    }
}
