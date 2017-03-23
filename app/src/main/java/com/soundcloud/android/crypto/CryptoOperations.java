package com.soundcloud.android.crypto;

import static com.soundcloud.android.crypto.KeyGeneratorWrapper.GENERATED_KEY_SIZE;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoOperations {

    protected static final String DEVICE_KEY = "device_key";

    private final KeyStorage storage;
    private final KeyGeneratorWrapper keyGenerator;
    private final Encryptor encryptor;
    private final SecureRandom secureRandom;
    private final Scheduler storageScheduler;

    @Inject
    public CryptoOperations(KeyStorage storage, KeyGeneratorWrapper keyGenerator, Encryptor encryptor,
                            @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.keyGenerator = keyGenerator;
        this.secureRandom = new SecureRandom();
        this.encryptor = encryptor;
        this.storage = storage;
        this.storageScheduler = scheduler;
    }

    public byte[] getKeyOrGenerateAndStore(String name) {
        if (storage.contains(name)) {
            return storage.get(name).getKey();
        } else {
            DeviceSecret generatedKey = generateKey(name);
            storage.put(generatedKey);
            return generatedKey.getKey();
        }
    }

    public void generateAndStoreDeviceKeyIfNeeded() {
        fireAndForget(Observable.fromCallable(this::checkAndGetDeviceKey).subscribeOn(storageScheduler));
    }

    public String generateHashForUrn(Urn urn) throws EncryptionException {
        return encryptor.hash(urn);
    }

    public void encryptStream(InputStream stream,
                              OutputStream outputStream,
                              Encryptor.EncryptionProgressListener listener) throws IOException, EncryptionException {
        try {
            final DeviceSecret secret = checkAndGetDeviceKey();
            encryptor.encrypt(stream, outputStream, secret, listener);
        } catch (EncryptionException e) {
            ErrorUtils.handleSilentException("Encryption error", e);
            throw e;
        }
    }

    public void cancelEncryption() {
        encryptor.tryToCancelRequest();
    }

    public synchronized DeviceSecret checkAndGetDeviceKey() {
        if (!storage.contains(DEVICE_KEY)) {
            generateAndStoreDeviceKey();
        }
        return storage.get(DEVICE_KEY);
    }

    public boolean containsDeviceKey() {
        return storage.contains(DEVICE_KEY);
    }

    private DeviceSecret generateKey(String name) {
        byte[] generatedKey = new byte[GENERATED_KEY_SIZE];
        secureRandom.nextBytes(generatedKey);
        return new DeviceSecret(name, generatedKey);
    }

    private void generateAndStoreDeviceKey() {
        try {
            byte[] iv = new byte[GENERATED_KEY_SIZE];
            secureRandom.nextBytes(iv);

            final DeviceSecret key = new DeviceSecret(DEVICE_KEY, keyGenerator.generateKey(secureRandom), iv);
            storage.put(key);
        } catch (NoSuchAlgorithmException e) {
            ErrorUtils.handleSilentException("Key generation error", e);
        }
    }
}
