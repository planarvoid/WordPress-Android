package com.soundcloud.android.utils;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashSet;
import java.util.Set;

public class Range implements Parcelable {
    public final int location;
    public final int length;

    /* private */ Range(int start, int length) {
        this.location = start;
        this.length = length;
    }

    public static Range from(int start, int length) {
        return new Range(start, length);
    }

    public static Range from(long start, long length) {
        return new Range((int)start, (int)length);
    }

    public Set<Integer> toIndexSet() {
        HashSet<Integer> indexSet = new HashSet<Integer>();
        for (int i = location; i < length; i++) {
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
        if (low < high) {
            return new Range(low, high - low);
        }
        return null;
    }

    public String toString() {
        return "Range{location: " + location +
                ", length:" + length +
                "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle data = new Bundle();
        data.putInt("location", location);
        data.putInt("length", length);
        dest.writeBundle(data);
    }

    public Range(Parcel in) {
        Bundle data = in.readBundle(getClass().getClassLoader());
        location = data.getInt("location");
        length = data.getInt("length");
    }

    public static final Parcelable.Creator<Range> CREATOR = new Parcelable.Creator<Range>() {
        public Range createFromParcel(Parcel in) {
            return new Range(in);
        }

        public Range[] newArray(int size) {
            return new Range[size];
        }
    };
}