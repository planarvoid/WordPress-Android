package com.soundcloud.java.collections;

import java.util.List;
import java.util.RandomAccess;

class RandomAccessPartition<T> extends Partition<T>
        implements RandomAccess {
    RandomAccessPartition(List<T> list, int size) {
        super(list, size);
    }
}
