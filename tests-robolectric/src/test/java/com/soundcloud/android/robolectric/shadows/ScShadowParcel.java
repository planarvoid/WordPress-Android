package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.shadows.ShadowParcel;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Implements(Parcel.class)
public class ScShadowParcel extends ShadowParcel {

    private static final int VAL_NULL = -1;
    private static final int VAL_STRING = 0;
    private static final int VAL_INTEGER = 1;
    private static final int VAL_MAP = 2;
    private static final int VAL_BUNDLE = 3;
    private static final int VAL_PARCELABLE = 4;
    private static final int VAL_SHORT = 5;
    private static final int VAL_LONG = 6;
    private static final int VAL_FLOAT = 7;
    private static final int VAL_DOUBLE = 8;
    private static final int VAL_BOOLEAN = 9;
    private static final int VAL_CHARSEQUENCE = 10;
    private static final int VAL_LIST = 11;
    private static final int VAL_SPARSEARRAY = 12;
    private static final int VAL_BYTEARRAY = 13;
    private static final int VAL_STRINGARRAY = 14;
    private static final int VAL_IBINDER = 15;
    private static final int VAL_PARCELABLEARRAY = 16;
    private static final int VAL_OBJECTARRAY = 17;
    private static final int VAL_INTARRAY = 18;
    private static final int VAL_LONGARRAY = 19;
    private static final int VAL_BYTE = 20;
    private static final int VAL_SERIALIZABLE = 21;
    private static final int VAL_SPARSEBOOLEANARRAY = 22;
    private static final int VAL_BOOLEANARRAY = 23;
    private static final int VAL_CHARSEQUENCEARRAY = 24;

    @Implementation
    @SuppressWarnings("unchecked")
    public void writeSerializable(Serializable s) {
        getParcelData().add(s);
    }

    @Implementation
    public Serializable readSerializable() {
        try {
            // crazy hack to increment the private index field on ShadowParcel
            Field indexField = ShadowParcel.class.getDeclaredField("index");
            indexField.setAccessible(true);
            int index = getIndex();
            Serializable result = index < getParcelData().size() ? (Serializable) getParcelData().get(index) : null;
            indexField.setInt(this, index + 1);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public final void writeSparseArray(SparseArray<Object> val) {
        if (val == null) {
            writeInt(-1);
            return;
        }
        int N = val.size();
        writeInt(N);
        int i = 0;
        while (i < N) {
            writeInt(val.keyAt(i));
            writeValue(val.valueAt(i));
            i++;
        }
    }

    public final SparseArray readSparseArray(ClassLoader loader) {
        int N = readInt();
        if (N < 0) {
            return null;
        }
        SparseArray sa = new SparseArray(N);
        readSparseArrayInternal(sa, N, loader);
        return sa;
    }

    private void readSparseArrayInternal(SparseArray outVal, int N,
                                         ClassLoader loader) {
        while (N > 0) {
            int key = readInt();
            Object value = readValue(loader);
            //Log.i(TAG, "Unmarshalling key=" + key + " value=" + value);
            outVal.append(key, value);
            N--;
        }
    }

    @Implementation
    public final void writeMap(Map val) {
        writeMapInternal((Map<String, Object>) val);
    }

    /**
     * Flatten a Map into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.  The Map keys must be String objects.
     */
    private void writeMapInternal(Map<String, Object> val) {
        if (val == null) {
            writeInt(-1);
            return;
        }
        Set<Map.Entry<String, Object>> entries = val.entrySet();
        writeInt(entries.size());
        for (Map.Entry<String, Object> e : entries) {
            writeValue(e.getKey());
            writeValue(e.getValue());
        }
    }

    @Implementation
    public final void readMap(Map outVal, ClassLoader loader) {
        int N = readInt();
        readMapInternal(outVal, N, loader);
    }

    @Implementation
    public final HashMap readHashMap(ClassLoader loader) {
        int N = readInt();
        if (N < 0) {
            return null;
        }
        HashMap m = new HashMap(N);
        readMapInternal(m, N, loader);
        return m;
    }

    private void readMapInternal(Map outVal, int N,
                                 ClassLoader loader) {
        while (N > 0) {
            Object key = readValue(loader);
            Object value = readValue(loader);
            outVal.put(key, value);
            N--;
        }
    }

    @Implementation
    public final void writeCharSequence(CharSequence val) {
        writeString(val.toString());
    }

    @Implementation
    public final CharSequence readCharSequence() {
        return readString();
    }

    @Implementation
    public final void writeList(List val) {
        if (val == null) {
            writeInt(-1);
            return;
        }
        int N = val.size();
        int i = 0;
        writeInt(N);
        while (i < N) {
            writeValue(val.get(i));
            i++;
        }
    }

    @Implementation
    public final void readList(List outVal, ClassLoader loader) {
        int N = readInt();
        readListInternal(outVal, N, loader);
    }

    private void readListInternal(List outVal, int N,
                                  ClassLoader loader) {
        while (N > 0) {
            Object value = readValue(loader);
            //Log.d(TAG, "Unmarshalling value=" + value);
            outVal.add(value);
            N--;
        }
    }

    @Implementation
    public final void writeBooleanArray(boolean[] val) {
        if (val != null) {
            int N = val.length;
            writeInt(N);
            for (int i = 0; i < N; i++) {
                writeInt(val[i] ? 1 : 0);
            }
        } else {
            writeInt(-1);
        }
    }

    @Implementation
    public final void readBooleanArray(boolean[] val) {
        int N = readInt();
        if (N == val.length) {
            for (int i = 0; i < N; i++) {
                val[i] = readInt() != 0;
            }
        } else {
            throw new RuntimeException("bad array lengths");
        }
    }

    @Implementation
    public final void writeByteArray(byte[] b) {
        throw new NotImplementedException();
    }

    @Implementation
    public final void writeCharSequenceArray(CharSequence[] val) {
        if (val != null) {
            int N = val.length;
            writeInt(N);
            for (int i = 0; i < N; i++) {
                writeCharSequence(val[i]);
            }
        } else {
            writeInt(-1);
        }
    }

    @Implementation
    public final <T extends Parcelable> void writeParcelableArray(T[] value,
                                                                  int parcelableFlags) {
        if (value != null) {
            int N = value.length;
            writeInt(N);
            for (int i = 0; i < N; i++) {
                writeParcelable(value[i], parcelableFlags);
            }
        } else {
            writeInt(-1);
        }
    }

    @Implementation
    public final void writeArray(Object[] val) {
        if (val == null) {
            writeInt(-1);
            return;
        }
        int N = val.length;
        int i = 0;
        writeInt(N);
        while (i < N) {
            writeValue(val[i]);
            i++;
        }
    }

    @Implementation
    public final Object[] readArray(ClassLoader loader) {
        int N = readInt();
        if (N < 0) {
            return null;
        }
        Object[] l = new Object[N];
        readArrayInternal(l, N, loader);
        return l;
    }

    private void readArrayInternal(Object[] outVal, int N,
                                   ClassLoader loader) {
        for (int i = 0; i < N; i++) {
            Object value = readValue(loader);
            //Log.d(TAG, "Unmarshalling value=" + value);
            outVal[i] = value;
        }
    }

    @Implementation
    public final ArrayList readArrayList(ClassLoader loader) {
        int N = readInt();
        if (N < 0) {
            return null;
        }
        ArrayList l = new ArrayList(N);
        readListInternal(l, N, loader);
        return l;
    }

    @Implementation
    public final String[] readStringArray() {
        String[] array = null;

        int length = readInt();
        if (length >= 0) {
            array = new String[length];

            for (int i = 0; i < length; i++) {
                array[i] = readString();
            }
        }

        return array;
    }

    @Implementation
    public final boolean[] createBooleanArray() {
        int N = readInt();
        // >>2 as a fast divide-by-4 works in the build*Array() functions
        // because dataAvail() will never return a negative number.  4 is
        // the size of a stored boolean in the stream.
        if (N >= 0) {
            boolean[] val = new boolean[N];
            for (int i = 0; i < N; i++) {
                val[i] = readInt() != 0;
            }
            return val;
        } else {
            return null;
        }
    }

    @Implementation
    public final byte[] createByteArray() {
        throw new NotImplementedException();
    }

    @Implementation
    public final CharSequence[] readCharSequenceArray() {
        CharSequence[] array = null;

        int length = readInt();
        if (length >= 0) {
            array = new CharSequence[length];

            for (int i = 0; i < length; i++) {
                array[i] = readCharSequence();
            }
        }

        return array;
    }

    @Implementation
    public final int[] createIntArray() {
        int N = readInt();
        if (N >= 0) {
            int[] val = new int[N];
            for (int i = 0; i < N; i++) {
                val[i] = readInt();
            }
            return val;
        } else {
            return null;
        }
    }

    @Implementation
    public final long[] createLongArray() {
        int N = readInt();
        // >>3 because stored longs are 64 bits
        if (N >= 0) {
            long[] val = new long[N];
            for (int i = 0; i < N; i++) {
                val[i] = readLong();
            }
            return val;
        } else {
            return null;
        }
    }

    @Implementation
    public final Parcelable[] readParcelableArray(ClassLoader loader) {
        int N = readInt();
        if (N < 0) {
            return null;
        }
        Parcelable[] p = new Parcelable[N];
        for (int i = 0; i < N; i++) {
            p[i] = readParcelable(loader);
        }
        return p;
    }

    @Implementation
    public final SparseBooleanArray readSparseBooleanArray() {
        int N = readInt();
        if (N < 0) {
            return null;
        }
        SparseBooleanArray sa = new SparseBooleanArray(N);
        readSparseBooleanArrayInternal(sa, N);
        return sa;
    }

    private void readSparseBooleanArrayInternal(SparseBooleanArray outVal, int N) {
        while (N > 0) {
            int key = readInt();
            boolean value = this.readByte() == 1;
            //Log.i(TAG, "Unmarshalling key=" + key + " value=" + value);
            outVal.append(key, value);
            N--;
        }
    }


    /**
     * Read a typed object from a parcel.  The given class loader will be
     * used to load any enclosed Parcelables.  If it is null, the default class
     * loader will be used.
     */
    @Implementation
    public final Object readValue(ClassLoader loader) {
        int type = readInt();

        switch (type) {
            case VAL_NULL:
                return null;

            case VAL_STRING:
                return readString();

            case VAL_INTEGER:
                return readInt();

            case VAL_MAP:
                return readHashMap(loader);

            case VAL_PARCELABLE:
                return readParcelable(loader);

            case VAL_SHORT:
                return (short) readInt();

            case VAL_LONG:
                return readLong();

            case VAL_FLOAT:
                return readFloat();

            case VAL_DOUBLE:
                return readDouble();

            case VAL_BOOLEAN:
                return readInt() == 1;

            case VAL_CHARSEQUENCE:
                return readCharSequence();

            case VAL_LIST:
                return readArrayList(loader);

            case VAL_BOOLEANARRAY:
                return createBooleanArray();

            case VAL_BYTEARRAY:
                return createByteArray();

            case VAL_STRINGARRAY:
                return readStringArray();

            case VAL_CHARSEQUENCEARRAY:
                return readCharSequenceArray();

            case VAL_IBINDER:
                return readStrongBinder();

            case VAL_OBJECTARRAY:
                return readArray(loader);

            case VAL_INTARRAY:
                return createIntArray();

            case VAL_LONGARRAY:
                return createLongArray();

            case VAL_BYTE:
                return readByte();

            case VAL_SERIALIZABLE:
                return readSerializable();

            case VAL_PARCELABLEARRAY:
                return readParcelableArray(loader);

            case VAL_SPARSEARRAY:
                return readSparseArray(loader);

            case VAL_SPARSEBOOLEANARRAY:
                return readSparseBooleanArray();

            case VAL_BUNDLE:
                return readBundle(loader); // loading will be deferred

            default:
                int off = getIndex() - 4;
                throw new RuntimeException(
                        "Parcel " + this + ": Unmarshalling unknown type code " + type + " at offset " + off);
        }
    }

    @Implementation
    public final void writeValue(Object v) {
        if (v == null) {
            writeInt(VAL_NULL);
        } else if (v instanceof String) {
            writeInt(VAL_STRING);
            writeString((String) v);
        } else if (v instanceof Integer) {
            writeInt(VAL_INTEGER);
            writeInt((Integer) v);
        } else if (v instanceof Map) {
            writeInt(VAL_MAP);
            writeMap((Map) v);
        } else if (v instanceof Bundle) {
            // Must be before Parcelable
            writeInt(VAL_BUNDLE);
            writeBundle((Bundle) v);
        } else if (v instanceof Parcelable) {
            writeInt(VAL_PARCELABLE);
            writeParcelable((Parcelable) v, 0);
        } else if (v instanceof Short) {
            writeInt(VAL_SHORT);
            writeInt(((Short) v).intValue());
        } else if (v instanceof Long) {
            writeInt(VAL_LONG);
            writeLong((Long) v);
        } else if (v instanceof Float) {
            writeInt(VAL_FLOAT);
            writeFloat((Float) v);
        } else if (v instanceof Double) {
            writeInt(VAL_DOUBLE);
            writeDouble((Double) v);
        } else if (v instanceof Boolean) {
            writeInt(VAL_BOOLEAN);
            writeInt((Boolean) v ? 1 : 0);
        } else if (v instanceof CharSequence) {
            // Must be after String
            writeInt(VAL_CHARSEQUENCE);
            writeCharSequence((CharSequence) v);
        } else if (v instanceof List) {
            writeInt(VAL_LIST);
            writeList((List) v);
        } else if (v instanceof SparseArray) {
            writeInt(VAL_SPARSEARRAY);
            writeSparseArray((SparseArray) v);
        } else if (v instanceof boolean[]) {
            writeInt(VAL_BOOLEANARRAY);
            writeBooleanArray((boolean[]) v);
        } else if (v instanceof byte[]) {
            writeInt(VAL_BYTEARRAY);
            writeByteArray((byte[]) v);
        } else if (v instanceof String[]) {
            writeInt(VAL_STRINGARRAY);
            writeStringArray((String[]) v);
        } else if (v instanceof CharSequence[]) {
            // Must be after String[] and before Object[]
            writeInt(VAL_CHARSEQUENCEARRAY);
            writeCharSequenceArray((CharSequence[]) v);
        } else if (v instanceof IBinder) {
            writeInt(VAL_IBINDER);
            writeStrongBinder((IBinder) v);
        } else if (v instanceof Parcelable[]) {
            writeInt(VAL_PARCELABLEARRAY);
            writeParcelableArray((Parcelable[]) v, 0);
        } else if (v instanceof Object[]) {
            writeInt(VAL_OBJECTARRAY);
            writeArray((Object[]) v);
        } else if (v instanceof int[]) {
            writeInt(VAL_INTARRAY);
            writeIntArray((int[]) v);
        } else if (v instanceof long[]) {
            writeInt(VAL_LONGARRAY);
            writeLongArray((long[]) v);
        } else if (v instanceof Byte) {
            writeInt(VAL_BYTE);
            writeInt((Byte) v);
        } else if (v instanceof Serializable) {
            // Must be last
            writeInt(VAL_SERIALIZABLE);
            writeSerializable((Serializable) v);
        } else {
            throw new RuntimeException("Parcel: unable to marshal value " + v);
        }
    }
}
