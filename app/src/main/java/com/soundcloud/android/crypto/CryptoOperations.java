package com.soundcloud.android.crypto;

import com.soundcloud.android.storage.KeyStorage;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;

import javax.crypto.KeyGenerator;
import javax.inject.Inject;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoOperations {
    private static final String APP_KEY = "device_key";
    private static final int GENERATED_KEY_SIZE = 16;

    private final KeyStorage storage;
    private final SecureRandom secureRandom;

    @Inject
    public CryptoOperations(KeyStorage storage) {
        this.secureRandom = new SecureRandom();
        this.storage = storage;
    }

    public void generateApplicationKeyIfNeeded() {
        if (!storage.contains(APP_KEY)) {
            generateAndStoreAppKey();
        }
    }

    public byte[] getKeyOrGenerateAndStore(String name) {
        if (storage.contains(name)) {
            return storage.get(name).getBytes();
        } else {
            SecureKey generatedKey = generateKey(name);
            storage.put(generatedKey);
            return generatedKey.getBytes();
        }
    }

    private SecureKey generateKey(String name) {
        byte[] generatedKey = new byte[GENERATED_KEY_SIZE];
        secureRandom.nextBytes(generatedKey);
        return new SecureKey(name, generatedKey);
    }

    private void generateAndStoreAppKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128, secureRandom);

            byte[] iv = new byte[GENERATED_KEY_SIZE];
            secureRandom.nextBytes(iv);

            storage.put(new SecureKey(APP_KEY, keyGen.generateKey().getEncoded(), iv));

        } catch (NoSuchAlgorithmException e) {
            Log.d("No provider found to generate AES key");
            ErrorUtils.handleSilentException(e);
        }
    }
}
