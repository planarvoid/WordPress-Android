package com.soundcloud.android.crypto;

import javax.crypto.KeyGenerator;
import javax.inject.Inject;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class KeyGeneratorWrapper {

    private static final int KEY_SIZE = 128;
    private static final String ALGORITHM = "AES";

    static final int GENERATED_KEY_SIZE = 16;

    @Inject
    public KeyGeneratorWrapper() {}

    public byte[] generateKey(SecureRandom secureRandom) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE, secureRandom);

        return keyGen.generateKey().getEncoded();
    }
}
