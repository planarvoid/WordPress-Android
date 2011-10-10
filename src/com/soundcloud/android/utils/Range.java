package com.soundcloud.android.utils;

import java.util.HashSet;
import java.util.Set;

public class Range {
    public Range(long start, long length) {
        this.start = start;
        this.length = length;
    }

    public long start;
    public long length;

    public String toString() {
        return "Range{start: " + start +
                ", length:" + length +
                "}";
    }

    public Set<Long> toIndexSet(){
        HashSet<Long> indexSet = new HashSet<Long>();
        for (Long i = start; i < length; i++){
            indexSet.add(start);
        }
        return indexSet;
    }

    public Long end() {
        return start + length;
    }
}

