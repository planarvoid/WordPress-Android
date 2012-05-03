package com.soundcloud.android.record;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Iterator;

public class AmplitudeData implements Iterable<Float>, Parcelable {
    private static final int AVERAGE_RECORDING_TIME = 3 * 60; // 3 minutes

    private float[] data;
    private int pos;

    private final int initialCapacity;

    public AmplitudeData() {
        this(AVERAGE_RECORDING_TIME * CloudRecorder.FPS);
    }

    public AmplitudeData(int initialCapacity ) {
        this.initialCapacity = initialCapacity;
        data = new float[initialCapacity];

    }
    public AmplitudeData(float[] data) {
        initialCapacity = pos = data.length;
        this.data = data;
    }

    public AmplitudeData(Parcel source) {
        initialCapacity = source.readInt();
        pos = source.readInt();
        int length = source.readInt();
        assert pos <= length;
        data = new float[length];
        source.readFloatArray(data);
    }

    public void add(float sample) {
        ensureCapacity(pos+1);
        data[pos++] = sample;
    }

    public void add(float[] samples) {
        ensureCapacity(pos+samples.length);
        System.arraycopy(samples, 0, data, pos, samples.length);
        pos += samples.length;
    }

    public int size() {
        return pos;
    }

    private void ensureCapacity(int capacity) {
        if (capacity > data.length) {
            int newCap = Math.max(data.length << 1, capacity);
            float[] tmp =  new float[newCap];
            System.arraycopy(data, 0, tmp, 0, data.length);
            data = tmp;
        }
    }

    public float get(int index) {
        if (index >= pos) {
            throw new ArrayIndexOutOfBoundsException(index);
        } else {
            return data[index];
        }
    }

    public void clear() {
        data = new float[initialCapacity];
        pos = 0;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public AmplitudeData slice(int start, int size) {
        // TODO: don't copy data
        float[] copy = new float[size];
        System.arraycopy(data, start, copy, 0, size);
        return new AmplitudeData(copy);
    }

    public float getInterpolatedValue(int x, int firstX, int lastX) {
        final int size = size();
        if (size > lastX - firstX) {
            // scaling down, nearest neighbor is fine
            return get((int) Math.min(size - 1, ((float) (x - firstX)) / (lastX - firstX) * size));
        } else {
            // scaling up, do interpolation
            final float fIndex = Math.min(size - 1, size * ((float) (x - firstX)) / (lastX - firstX));
            final float v1 = get((int) Math.floor(fIndex));
            final float v2 = get((int) Math.ceil(fIndex));
            return v1 + (v2 - v1) * (fIndex - ((int) fIndex));
        }
    }

    @Override
    public Iterator<Float> iterator() {
        return new Iterator<Float>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < pos;
            }

            @Override
            public Float next() {
                return data[index++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(initialCapacity);
        dest.writeInt(pos);
        dest.writeInt(data.length);
        dest.writeInt(data.length);
        for (int i=0; i<pos; i++) {
            dest.writeFloat(data[i]);
        }
    }

    public static final Creator<AmplitudeData> CREATOR = new Creator<AmplitudeData>() {
        @Override
        public AmplitudeData createFromParcel(Parcel source) {
            return new AmplitudeData(source);
        }

        @Override
        public AmplitudeData[] newArray(int size) {
            return new AmplitudeData[size];
        }
    };

    public float[] get() {
        return data;
    }

    @Override
    public String toString() {
        return "AmplitudeData{" +
                "pos=" + pos +
                ", initialCapacity=" + initialCapacity +
                '}';
    }
}
