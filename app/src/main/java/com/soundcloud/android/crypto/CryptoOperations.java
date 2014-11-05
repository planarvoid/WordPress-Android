package com.soundcloud.android.crypto;

import com.soundcloud.android.storage.KeyStorage;

import javax.inject.Inject;
import java.security.SecureRandom;

public class CryptoOperations {

    private static final int GENERATED_KEY_SIZE = 16;
    private static final byte[] EMPTY_KEY = new byte[]{};

    private final KeyStorage storage;
    private final SecureRandom secureRandom;

    @Inject
    public CryptoOperations(KeyStorage storage) {
        this.secureRandom = new SecureRandom();
        this.storage = storage;
    }

    public byte[] getKeyOrGenerateAndStore(String name) {
        if (storage.contains(name)) {
            return storage.get(name, EMPTY_KEY);
        } else {
            byte[] generatedKey = generateKey();
            storage.put(name, generatedKey);
            return generatedKey;
        }
    }

    private byte[] generateKey() {
        byte[] generatedKey = new byte[GENERATED_KEY_SIZE];
        secureRandom.nextBytes(generatedKey);
        return generatedKey;
    }

}
