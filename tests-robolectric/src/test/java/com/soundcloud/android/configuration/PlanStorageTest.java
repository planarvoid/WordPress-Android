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

import java.util.Arrays;

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
        storage.update("plan", "mid-tier");

        expect(storage.get("plan", "none")).toEqual("mid-tier");
    }

    @Test
    public void returnsDefaultIfNotSet() {
        expect(storage.get("plan", "none")).toEqual("none");
    }

    @Test
    public void updateStoresValueList() {
        storage.update("upsells", Arrays.asList("mid-tier", "high-tier"));

        expect(storage.getList("upsells")).toContainExactlyInAnyOrder("mid-tier", "high-tier");
    }

    @Test
    public void updateReplacesEntireValueList() {
        storage.update("upsells", Arrays.asList("mid-tier", "high-tier"));

        storage.update("upsells", Arrays.asList("mid-tier"));

        expect(storage.getList("upsells")).toContainExactly("mid-tier");
    }

    @Test
    public void returnsEmptyListIfNotSet() {
        expect(storage.getList("upsells")).toBeEmpty();
    }

    @Test
    public void clearRemovesStoredValues() {
        storage.update("plan", "mid-tier");

        storage.clear();

        expect(storage.get("plan", "none")).toEqual("none");
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