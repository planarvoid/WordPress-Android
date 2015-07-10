package com.soundcloud.android.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.schedulers.Schedulers;

import java.io.InputStream;
import java.io.OutputStream;

@RunWith(MockitoJUnitRunner.class)
public class CryptoOperationsTest {

    private CryptoOperations operations;

    @Mock private KeyStorage storage;
    @Mock private InputStream inputStream;
    @Mock private OutputStream outputStream;
    @Mock private Encryptor encryptor;
    @Mock private DeviceSecret deviceSecret;
    @Mock private Encryptor.EncryptionProgressListener listener;

    private final static String KEY_NAME = "some key";

    @Before
    public void setUp() throws Exception {
        operations = new CryptoOperations(storage, encryptor, Schedulers.immediate());
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

}