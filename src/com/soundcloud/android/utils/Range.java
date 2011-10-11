package com.soundcloud.android.utils;

import java.util.HashSet;
import java.util.Set;

public class Range {
    public Range(int start, int length) {
        this.location = start;
        this.length = length;
    }

    public int location;
    public int length;

    public String toString() {
        return "Range{location: " + location +
                ", length:" + length +
                "}";
    }

    public HashSet<Integer> toIndexSet(){
        HashSet<Integer> indexSet = new HashSet<Integer>();
        for (int i = location; i < length; i++){
            indexSet.add(location);
        }
        return indexSet;
    }

    public int end() {
        return location + length;
    }

    public Range intersection(Range range) {
        int low = Math.max(range.location, location);
        int high = Math.min(range.location + length, location + length);
        if (low < high){
            return new Range(low, high -low);
        }
        return null;
    }
}