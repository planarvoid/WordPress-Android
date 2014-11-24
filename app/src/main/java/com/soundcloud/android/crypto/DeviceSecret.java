package com.soundcloud.android.crypto;

public class DeviceSecret {

    public final static DeviceSecret EMPTY = new DeviceSecret();

    private final String name;
    private final byte[] key;
    private final byte[] initVector;

    private DeviceSecret() {
        name = "empty_key";
        initVector = key = new byte[]{};
    }

    public DeviceSecret(String name, byte[] key) {
        this(name, key, null);
    }

    public DeviceSecret(String name, byte[] newKey, byte[] iVector) {
        this.name = name;
        this.key = arrayCopy(newKey);
        this.initVector = arrayCopy(iVector);
    }

    public String getName() {
        return name;
    }

    public byte[] getKey() {
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
