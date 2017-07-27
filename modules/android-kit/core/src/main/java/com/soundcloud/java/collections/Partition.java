package com.soundcloud.java.collections;

import static com.soundcloud.java.checks.IndexHelper.checkElementIndex;

import java.util.AbstractList;
import java.util.List;

class Partition<T> extends AbstractList<List<T>> {
    final List<T> list;
    final int size;

    Partition(List<T> list, int size) {
        this.list = list;
        this.size = size;
    }

    @Override
    public List<T> get(int index) {
        checkElementIndex(index, size(), "index");
        int start = index * size;
        int end = Math.min(start + size, list.size());
        return list.subList(start, end);
    }

    @Override
    public int size() {
        int div = list.size() / size;
        int rem = list.size() % size;
        if (rem == 0) {
            return div;
        } else {
            return div + 1;
        }
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }
}
