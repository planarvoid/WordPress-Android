package com.soundcloud.android.streaming;

import java.util.TreeSet;

class IndexSet extends TreeSet<Integer> {
    public static IndexSet empty() {
        return new IndexSet();
    }
}
