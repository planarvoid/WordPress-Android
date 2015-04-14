package com.soundcloud.android.configuration;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ScTestSharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(SoundCloudTestRunner.class)
public class PlanStorageTest {

    public static final String MOCK_ENCRYPTION = "-obfuscated";

    private PlanStorage storage;

    @Mock private Obfuscator obfuscator;

    @Before
    public void setUp() throws Exception {
        storage = new PlanStorage(new ScTestSharedPreferences(), obfuscator);
        configureMockObfuscation();
    }

    @Test
    public void updateStoresValue() {
        storage.update("key1", "some value");

        expect(storage.get("key1", "key2")).toEqual("some value");
    }

    @Test
    public void getReturnsDefaultIfNotSet() {
        expect(storage.get("key", "default")).toEqual("default");
    }

    private void configureMockObfuscation() throws Exception {
        when(obfuscator.obfuscate(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0] + MOCK_ENCRYPTION;
            }
        });
        when(obfuscator.deobfuscateString(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String encrypted = invocation.getArguments()[0].toString();
                return encrypted.substring(0, encrypted.length() - MOCK_ENCRYPTION.length());
            }
        });
    }

}