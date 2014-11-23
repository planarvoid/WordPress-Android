package com.soundcloud.android.crypto;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.storage.KeyStorage;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import javax.crypto.KeyGenerator;
import javax.inject.Inject;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoOperations {

    private static final String TAG = "CryptoOps";
    private static final String DEVICE_KEY = "device_key";
    private static final int GENERATED_KEY_SIZE = 16;

    private final KeyStorage storage;
    private final SecureRandom secureRandom;
    private final Scheduler storageScheduler;

    @Inject
    public CryptoOperations(KeyStorage storage, Scheduler scheduler) {
        this.secureRandom = new SecureRandom();
        this.storage = storage;
        this.storageScheduler = scheduler;
    }

    public void generateApplicationKeyIfNeeded() {
        if (!storage.contains(DEVICE_KEY)) {
            generateAndStoreAppKeyAsync();
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

    private void generateAndStoreAppKeyAsync() {
        fireAndForget(Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                generateAndStoreAppKey();
            }
        }).subscribeOn(storageScheduler));
    }

    private void generateAndStoreAppKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128, secureRandom);

            byte[] iv = new byte[GENERATED_KEY_SIZE];
            secureRandom.nextBytes(iv);

            final SecureKey key = new SecureKey(DEVICE_KEY, keyGen.generateKey().getEncoded(), iv);
            storage.put(key);

        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "No provider found to generate key");
            ErrorUtils.handleSilentException(e);
        }
    }
}
