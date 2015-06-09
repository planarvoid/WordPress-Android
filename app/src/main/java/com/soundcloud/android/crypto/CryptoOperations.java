package com.soundcloud.android.crypto;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import javax.crypto.KeyGenerator;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoOperations {

    private static final String TAG = "CryptoOps";

    private static final int KEY_SIZE = 128;
    private static final String ALGORITHM = "AES";
    private static final int GENERATED_KEY_SIZE = 16;

    protected static final String DEVICE_KEY = "device_key";

    private final KeyStorage storage;
    private final Encryptor encryptor;
    private final SecureRandom secureRandom;
    private final Scheduler storageScheduler;

    @Inject
    public CryptoOperations(KeyStorage storage, Encryptor encryptor, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
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
        fireAndForget(Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                checkAndGetDeviceKey();
            }
        }).subscribeOn(storageScheduler));
    }

    public String generateHashForUrn(Urn urn) throws EncryptionException {
        return encryptor.hash(urn);
    }

    public void encryptStream(InputStream stream, OutputStream outputStream) throws IOException, EncryptionException {
        final DeviceSecret secret = checkAndGetDeviceKey();
        encryptor.encrypt(stream, outputStream, secret);
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

    private DeviceSecret generateKey(String name) {
        byte[] generatedKey = new byte[GENERATED_KEY_SIZE];
        secureRandom.nextBytes(generatedKey);
        return new DeviceSecret(name, generatedKey);
    }

    private void generateAndStoreDeviceKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE, secureRandom);

            byte[] iv = new byte[GENERATED_KEY_SIZE];
            secureRandom.nextBytes(iv);

            final DeviceSecret key = new DeviceSecret(DEVICE_KEY, keyGen.generateKey().getEncoded(), iv);
            storage.put(key);

        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "No provider found to generate key");
            ErrorUtils.handleSilentException(e);
        }
    }

}
