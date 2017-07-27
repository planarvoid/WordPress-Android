package com.soundcloud.java.collections;

import static com.soundcloud.java.checks.IndexHelper.checkElementIndex;
import static com.soundcloud.java.checks.IndexHelper.checkPositionIndex;
import static com.soundcloud.java.checks.IndexHelper.checkPositionIndexes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

class ReverseList<T> extends AbstractList<T> {
    private final List<T> forwardList;

    ReverseList(@NotNull List<T> forwardList) {
        this.forwardList = forwardList;
    }

    List<T> getForwardList() {
        return forwardList;
    }

    private int reverseIndex(int index) {
        int size = size();
        checkElementIndex(index, size, "index");
        return size - 1 - index;
    }

    private int reversePosition(int index) {
        int size = size();
        checkPositionIndex(index, size);
        return size - index;
    }

    @Override
    public void add(int index, @Nullable T element) {
        forwardList.add(reversePosition(index), element);
    }

    @Override
    public void clear() {
        forwardList.clear();
    }

    @Override
    public T remove(int index) {
        return forwardList.remove(reverseIndex(index));
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        subList(fromIndex, toIndex).clear();
    }

    @Override
    public T set(int index, @Nullable T element) {
        return forwardList.set(reverseIndex(index), element);
    }

    @Override
    public T get(int index) {
        return forwardList.get(reverseIndex(index));
    }

    @Override
    public int size() {
        return forwardList.size();
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        checkPositionIndexes(fromIndex, toIndex, size());
        return Lists.reverse(forwardList.subList(
                reversePosition(toIndex), reversePosition(fromIndex)));
    }

    @Override
    public Iterator<T> iterator() {
        return listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        int start = reversePosition(index);
        final ListIterator<T> forwardIterator = forwardList.listIterator(start);
        return new ReverseListIterator<>(forwardIterator);
    }

    private class ReverseListIterator<T> implements ListIterator<T> {

        private final ListIterator<T> forwardIterator;
        boolean canRemoveOrSet;

        public ReverseListIterator(ListIterator<T> forwardIterator) {
            this.forwardIterator = forwardIterator;
        }

        @Override
        public void add(T e) {
            forwardIterator.add(e);
            forwardIterator.previous();
            canRemoveOrSet = false;
        }

        @Override
        public boolean hasNext() {
            return forwardIterator.hasPrevious();
        }

        @Override
        public boolean hasPrevious() {
            return forwardIterator.hasNext();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            canRemoveOrSet = true;
            return forwardIterator.previous();
        }

        @Override
        public int nextIndex() {
            return reversePosition(forwardIterator.nextIndex());
        }

        @Override
        public T previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            canRemoveOrSet = true;
            return forwardIterator.next();
        }

        @Override
        public int previousIndex() {
            return nextIndex() - 1;
        }

        @Override
        public void remove() {
            forwardIterator.remove();
            canRemoveOrSet = false;
        }

        @Override
        public void set(T e) {
            if (!canRemoveOrSet) {
                throw new IllegalStateException();
            }
            forwardIterator.set(e);
        }
    }
}
