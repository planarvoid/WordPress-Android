package com.soundcloud.android.tests.crypto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.crypto.CipherWrapper;
import com.soundcloud.android.crypto.DeviceSecret;
import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.crypto.EncryptionInterruptedException;
import com.soundcloud.android.crypto.Encryptor;

import android.test.InstrumentationTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EncryptionTest extends InstrumentationTestCase {

    // test vectors from AESVS MMT test data for CBC (CBCMMT128.rsp)
    private static final String KEY_1 = "1f8e4973953f3fb0bd6b16662e9a3c17";
    private static final String IV_1 = "2fe2b333ceda8f98f4a99b40d2cd34a8";
    private static final String PLAIN_TEXT_1 = "45cf12964fc824ab76616ae2f4bf0822";
    private static final String CIPHER_TEXT_1 = "0f61c4d44c5147c03c195ad7e2cc12b2";

    private static final String KEY_2 = "3348aa51e9a45c2dbe33ccc47f96e8de";
    private static final String IV_2 = "19153c673160df2b1d38c28060e59b96";
    private static final String PLAINTEXT_2 = "9b7cee827a26575afdbb7c7a329f887238052e3601a7917456ba61251c214763d5e1847a6ad5d54127a399ab07ee3599";
    private static final String CIPHER_TEXT_2 = "d5aed6c9622ec451a15db12819952b6752501cf05cdbf8cda34a457726ded97818e1f127a28d72db5652749f0c6afee5";

    private Encryptor encryptor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        encryptor = new Encryptor(new CipherWrapper());
    }

    public void testEncrypt16BytesInput() throws IOException, EncryptionException {
        encryptAndVerifyEncryption(KEY_1, IV_1, PLAIN_TEXT_1, CIPHER_TEXT_1);
    }

    public void testEncrypt48BytesInput() throws IOException, EncryptionException {
        encryptAndVerifyEncryption(KEY_2, IV_2, PLAINTEXT_2, CIPHER_TEXT_2);
    }

    public void testNextEncryptionAfterFirstWasCancelled() throws IOException, EncryptionException {
        final DeviceSecret secret = new DeviceSecret("KEY", hexStringToBytes(KEY_1), hexStringToBytes(IV_1));
        final InputStream plainTextInput = new ByteArrayInputStream(hexStringToBytes(PLAIN_TEXT_1));
        final ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();

        encryptor.tryToCancelRequest();
        boolean exceptionThrown = false;
        try {
            encryptor.encrypt(plainTextInput, encryptedOutput, secret);
        } catch (EncryptionException | IOException ex){
            assertTrue(ex instanceof EncryptionInterruptedException);
            exceptionThrown = true;
        } finally {
            assertTrue(exceptionThrown);
        }

        encryptor.encrypt(plainTextInput, encryptedOutput, secret);
        assertCipherTextWithoutPadding(encryptedOutput.toByteArray(), hexStringToBytes(CIPHER_TEXT_1));
    }

    public void testDecrypt() throws IOException, EncryptionException {
        final DeviceSecret secret = new DeviceSecret("KEY", hexStringToBytes(KEY_1), hexStringToBytes(IV_1));

        final InputStream plainTextInput = new ByteArrayInputStream(hexStringToBytes(PLAIN_TEXT_1));
        final ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();
        encryptor.encrypt(plainTextInput, encryptedOutput, secret);

        // we cannot verify against test vectors because decryption expects padded input
        final InputStream encryptedInput = new ByteArrayInputStream(encryptedOutput.toByteArray());
        final ByteArrayOutputStream decryptedOutput = new ByteArrayOutputStream();

        encryptor.decrypt(encryptedInput, decryptedOutput, secret);
        assertThat(decryptedOutput.toByteArray(), is(hexStringToBytes(PLAIN_TEXT_1)));
    }

    private void encryptAndVerifyEncryption(String key, String iv, String plainText, String cipherText) throws EncryptionException, IOException {
        final DeviceSecret secret = new DeviceSecret("KEY", hexStringToBytes(key), hexStringToBytes(iv));
        final InputStream plainTextInput = new ByteArrayInputStream(hexStringToBytes(plainText));
        final ByteArrayOutputStream encryptedOutput = new ByteArrayOutputStream();

        encryptor.encrypt(plainTextInput, encryptedOutput, secret);
        assertCipherTextWithoutPadding(encryptedOutput.toByteArray(), hexStringToBytes(cipherText));
    }

    private void assertCipherTextWithoutPadding(byte[] cipherBytesWithPadding, byte[] cipherBytes) {
        byte[] resultCipherBytes = new byte[cipherBytes.length];
        System.arraycopy(cipherBytesWithPadding, 0, resultCipherBytes, 0, cipherBytes.length);

        assertThat(resultCipherBytes, is(cipherBytes));
        assertThat(cipherBytesWithPadding.length - cipherBytes.length, is(16));
    }

    private byte[] hexStringToBytes(String str) {
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}
