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

    public SecureKey(String name, byte[] key, byte[] initVector) {
        this.name = name;
        this.key = key;
        this.initVector = initVector;
    }


    public String getName() {
        return name;
    }

    public byte[] getBytes() {
        return key;
    }

    public byte[] getInitVector() {
        return initVector;
    }

    public boolean hasInitVector() {
        return initVector != null && initVector.length != 0;
    }
}
