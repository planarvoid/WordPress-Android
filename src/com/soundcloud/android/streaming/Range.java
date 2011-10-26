package com.soundcloud.android.streaming;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class Range implements Parcelable {
    public final int location;
    public final int length;

    /* private */ Range(int start, int length) {
        if (start < 0) throw new IllegalArgumentException("start must be >=0");
        if (length <= 0) throw new IllegalArgumentException("length must be >0");

        this.location = start;
        this.length = length;
    }

    public static Range from(int start, int length) {
        return new Range(start, length);
    }

    public static Range from(long start, long length) {
        return new Range((int)start, (int)length);
    }

    public Index toIndex() {
        Index index = new Index();
        for (int i = location; i < length+location; i++) {
            index.set(i);
        }
        return index;
    }

    public int end() {
        return location + length;
    }

    public Range intersection(Range range) {
        final int low = Math.max(range.location, location);
        final int high = Math.min(range.end(), end());

        return (low < high) ? new Range(low, high - low) : null;
    }

    public String toString() {
        return "Range{location: " + location +
                ", length:" + length +
                "}";
    }

    public Range chunkRange(int chunkSize) {
       return Range.from(location / chunkSize,
            (int) Math.ceil((double) ((location % chunkSize) + length) / (double) chunkSize));
    }

    public Range byteRange(int chunkSize) {
        return Range.from(location * chunkSize, length * chunkSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Range range = (Range) o;
        return length == range.length && location == range.location;
    }

    @Override
    public int hashCode() {
        int result = location;
        result = 31 * result + length;
        return result;
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