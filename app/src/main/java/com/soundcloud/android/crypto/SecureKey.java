package com.soundcloud.android.crypto;

public class SecureKey {

    public final static SecureKey EMPTY = new SecureKey();

    private final String name;
    private final byte[] key;
    private final byte[] initVector;

    private SecureKey() {
        name = "empty_key";
        initVector = key = new byte[]{};
    }

    public SecureKey(String name, byte[] key) {
        this(name, key, null);
    }

    public SecureKey(String name, byte[] newKey, byte[] iVector) {
        this.name = name;
        this.key = arrayCopy(newKey);
        this.initVector = arrayCopy(iVector);
    }

    public String getName() {
        return name;
    }

    public byte[] getBytes() {
        return arrayCopy(key);
    }

    public byte[] getInitVector() {
        return arrayCopy(initVector);
    }

    private byte[] arrayCopy(byte[] origin) {
        if (origin == null) {
            return null;
        }

        int length = origin.length;
        byte[] result = new byte[origin.length];
        System.arraycopy(origin, 0, result, 0, length);
        return result;
    }

    public boolean hasInitVector() {
        return initVector != null && initVector.length != 0;
    }
}
