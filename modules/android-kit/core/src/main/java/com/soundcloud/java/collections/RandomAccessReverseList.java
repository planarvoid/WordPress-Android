package com.soundcloud.java.collections;

import java.util.List;
import java.util.RandomAccess;

class RandomAccessReverseList<T> extends ReverseList<T>
        implements RandomAccess {
    RandomAccessReverseList(List<T> forwardList) {
        super(forwardList);
    }
}
