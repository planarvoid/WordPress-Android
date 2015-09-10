package com.soundcloud.android.crypto;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.strings.Charsets;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class EncryptionTester {
    private static final String TAG = EncryptionTester.class.getSimpleName();

    private final OfflineSettingsStorage offlineSettings;
    private final CryptoOperations cryptoOperations;
    private final Scheduler scheduler;

    @Inject
    public EncryptionTester(OfflineSettingsStorage offlineSettings,
                            CryptoOperations cryptoOperations,
                            @Named(ApplicationModule.LOW_PRIORITY) Scheduler scheduler) {

        this.offlineSettings = offlineSettings;
        this.cryptoOperations = cryptoOperations;
        this.scheduler = scheduler;
    }

    public void runEncryptionTest() {
        if (!offlineSettings.hasRunEncryptionTest()) {
            offlineSettings.setEncryptionTestRun();

            fireAndForget(Observable.create(new Observable.OnSubscribe<Object>() {
                @Override
                public void call(Subscriber<? super Object> subscriber) {
                    encryptSampleData();
                }
            }).subscribeOn(scheduler));
        }
    }

    private void encryptSampleData() {
        final InputStream plainTextInput = new ByteArrayInputStream("Plain Test".getBytes(Charsets.UTF_8));
        final ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();
        try {
            cryptoOperations.encryptStream(plainTextInput, encryptedOutput, null);
        } catch (Exception exception) {
            Log.d(TAG, "Test encryption failed: " + exception.getMessage());
        }
    }
}
