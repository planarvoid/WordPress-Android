package com.soundcloud.android.record;

import com.soundcloud.android.utils.IOUtils;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.FloatMath;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class AmplitudeData implements Iterable<Float>, Parcelable {
    private static final int AVERAGE_RECORDING_TIME = 3 * 60; // 3 minutes

    private float[] data;
    private int pos;

    private final int initialCapacity;

    public AmplitudeData() {
        this(AVERAGE_RECORDING_TIME * SoundRecorder.PIXELS_PER_SECOND);
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

    public void add(float... samples) {
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

    @Override
    public Iterator<Float> iterator() {
        return new Iterator<Float>() {
            int index;

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
        // parcels shouldn't save pre-recording data
        dest.writeInt(initialCapacity);
        dest.writeInt(pos);
        dest.writeInt(data.length);
        dest.writeInt(data.length); // required for readFloatArray
        for (int i = 0; i < pos; i++) {
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

    public void store(File out) throws IOException {
        Log.d(SoundRecorder.TAG, "writing amplitude data to " + out);
        FileOutputStream fos = new FileOutputStream(out);
        Parcel p = Parcel.obtain();
        writeToParcel(p, 0);
        fos.write(p.marshall());
        fos.close();
    }

    public static AmplitudeData fromFile(File in) throws IOException {
        byte[] bytes = IOUtils.readInputStreamAsBytes(new FileInputStream(in));
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);
        return new AmplitudeData(parcel);
    }

    @Override
    public String toString() {
        return "AmplitudeData{" +
                "pos=" + pos +
                ", initialCapacity=" + initialCapacity +
                '}';
    }

    public void truncate(int size) {
        int newSize = size;
        if (newSize > 0) {
            float[] newData = new float[newSize];
            System.arraycopy(data, 0, newData, 0, newSize);
            data = newData;
            pos = newSize;
        }
    }

    public void set(AmplitudeData adata) {
        data = adata.get();
        pos = adata.pos;
    }
}
