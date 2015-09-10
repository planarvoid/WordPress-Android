package com.soundcloud.android.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EncryptionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoOperationsTest extends AndroidUnitTest {

    private CryptoOperations operations;

    @Mock private KeyStorage storage;
    @Mock private InputStream inputStream;
    @Mock private OutputStream outputStream;
    @Mock private Encryptor encryptor;
    @Mock private DeviceSecret deviceSecret;
    @Mock private KeyGeneratorWrapper keyGenerator;
    @Mock private Encryptor.EncryptionProgressListener listener;

    private TestEventBus eventBus;
    private final static String KEY_NAME = "some key";

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        operations = new CryptoOperations(storage, keyGenerator, encryptor, eventBus, Schedulers.immediate());
        when(storage.contains(KEY_NAME)).thenReturn(true);
        when(storage.get(KEY_NAME)).thenReturn(deviceSecret);
    }

    @Test
    public void generateKeyWhenItDoesNotExist() {
        when(storage.contains(KEY_NAME)).thenReturn(false);

        operations.getKeyOrGenerateAndStore("my key");

        verify(storage).put(any(DeviceSecret.class));
        verify(storage, never()).get(KEY_NAME);
    }

    @Test
    public void doNotRegenerateKeyIfItExists() {
        operations.getKeyOrGenerateAndStore(KEY_NAME);

        verify(storage).get(KEY_NAME);
        verify(storage, never()).put(any(DeviceSecret.class));
    }

    @Test
    public void keyLengthShouldBe16() {
        assertThat(operations.getKeyOrGenerateAndStore("my key").length).isEqualTo(16);
    }

    @Test
    public void generateAndStoreDeviceKeyGeneratedAndStoreDeviceKey() {
        when(storage.contains(CryptoOperations.DEVICE_KEY)).thenReturn(false);

        operations.generateAndStoreDeviceKeyIfNeeded();

        InOrder inOrder = inOrder(storage);
        inOrder.verify(storage).put(any(DeviceSecret.class));
        inOrder.verify(storage).get(CryptoOperations.DEVICE_KEY);
    }

    @Test
    public void generateAndStoreDeviceDoesNotRegeneratesKeyWhenAlreadyExist() {
        when(storage.contains(CryptoOperations.DEVICE_KEY)).thenReturn(true);

        operations.generateAndStoreDeviceKeyIfNeeded();

        verify(storage).get(CryptoOperations.DEVICE_KEY);
        verify(storage, never()).put(any(DeviceSecret.class));
    }

    @Test
    public void generateHashForUrnCallsEncryptor() throws EncryptionException {
        final Urn trackUrn = Urn.forTrack(123L);
        operations.generateHashForUrn(trackUrn);

        verify(encryptor).hash(trackUrn);
    }

    @Test
    public void encryptStreamDelegatesToEncryptor() throws Exception {
        when(storage.contains(CryptoOperations.DEVICE_KEY)).thenReturn(true);
        when(storage.get(CryptoOperations.DEVICE_KEY)).thenReturn(deviceSecret);

        operations.encryptStream(inputStream, outputStream, listener);

        verify(encryptor).encrypt(inputStream, outputStream, deviceSecret, listener);
    }

    @Test
    public void encryptStreamShouldRegenerateDeviceKeyIfNotPresent() throws Exception {
        when(storage.contains(CryptoOperations.DEVICE_KEY)).thenReturn(false);

        operations.encryptStream(inputStream, outputStream, listener);

        InOrder inOrder = inOrder(storage);
        inOrder.verify(storage).contains(CryptoOperations.DEVICE_KEY);
        inOrder.verify(storage).put(any(DeviceSecret.class));
    }

    @Test
    public void encryptStreamSendsEncryptionErrorEventWhenEncryptionFailed() throws Exception {
        when(storage.contains(CryptoOperations.DEVICE_KEY)).thenReturn(true);
        when(storage.get(CryptoOperations.DEVICE_KEY)).thenReturn(deviceSecret);
        doThrow(new EncryptionException("Sample enc exception"))
                .when(encryptor).encrypt(inputStream, outputStream, deviceSecret, listener);

        try {
            operations.encryptStream(inputStream, outputStream, listener);
        } catch (EncryptionException e) {
            // expected exception
        }

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        EncryptionEvent event = (EncryptionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(EncryptionEvent.KIND_ENCRYPTION_ERROR);
    }

    @Test
    public void generateKeySendsEncryptionErrorWhenKeyGenerationFailed() throws NoSuchAlgorithmException {
        when(storage.contains(CryptoOperations.DEVICE_KEY)).thenReturn(false);
        when(keyGenerator.generateKey(any(SecureRandom.class)))
                .thenThrow(new NoSuchAlgorithmException("Expected Exception"));

        operations.generateAndStoreDeviceKeyIfNeeded();

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        EncryptionEvent event = (EncryptionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind().equals(EncryptionEvent.KIND_KEY_GENERATION_ERROR));
    }

    @Test
    public void encryptStreamSendsSuccessfulEncryptionEvent() throws IOException, EncryptionException {
        when(storage.contains(CryptoOperations.DEVICE_KEY)).thenReturn(true);
        when(storage.get(CryptoOperations.DEVICE_KEY)).thenReturn(deviceSecret);

        operations.encryptStream(inputStream, outputStream, listener);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        EncryptionEvent event = (EncryptionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind().equals(EncryptionEvent.KIND_SUCCESSUFULL_ENCRYPTION));
    }

}