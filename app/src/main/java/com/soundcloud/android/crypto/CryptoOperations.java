package com.soundcloud.android.crypto;

import static com.soundcloud.android.crypto.KeyGeneratorWrapper.GENERATED_KEY_SIZE;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EncryptionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoOperations {

    private static final String TAG = "CryptoOps";

    protected static final String DEVICE_KEY = "device_key";

    private final KeyStorage storage;
    private final KeyGeneratorWrapper keyGenerator;
    private final Encryptor encryptor;
    private final SecureRandom secureRandom;
    private final EventBus eventBus;
    private final Scheduler storageScheduler;

    @Inject
    public CryptoOperations(KeyStorage storage, KeyGeneratorWrapper keyGenerator, Encryptor encryptor, EventBus eventBus,
                            @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.keyGenerator = keyGenerator;
        this.eventBus = eventBus;
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

    public void encryptStream(InputStream stream, OutputStream outputStream, Encryptor.EncryptionProgressListener listener) throws IOException, EncryptionException {
        try {
            final DeviceSecret secret = checkAndGetDeviceKey();
            encryptor.encrypt(stream, outputStream, secret, listener);
            eventBus.publish(EventQueue.TRACKING, EncryptionEvent.fromEncryptionSuccess());
        } catch (EncryptionException e) {
            ErrorUtils.handleSilentException("Encryption error", e);
            eventBus.publish(EventQueue.TRACKING, EncryptionEvent.fromEncryptionError());
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
            eventBus.publish(EventQueue.TRACKING,
                    EncryptionEvent.fromKeyGenerationError());
        }
    }
}
