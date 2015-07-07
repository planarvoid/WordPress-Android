package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.testsupport.PlatformUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import android.content.Context;

import java.util.Arrays;

public class PlanStorageTest extends PlatformUnitTest {

    public static final String MOCK_ENCRYPTION = "-obfuscated";

    private PlanStorage storage;

    @Mock private Obfuscator obfuscator;

    @Before
    public void setUp() throws Exception {
        storage = new PlanStorage(sharedPreferences("test", Context.MODE_PRIVATE), obfuscator);
        configureMockObfuscation();
    }

    @Test
    public void updateStoresValue() {
        storage.update("plan", "mid-tier");

        assertThat(storage.get("plan", "none")).isEqualTo("mid-tier");
    }

    @Test
    public void returnsDefaultIfNotSet() {
        assertThat(storage.get("plan", "none")).isEqualTo("none");
    }

    @Test
    public void updateStoresValueList() {
        storage.update("upsells", Arrays.asList("mid-tier", "high-tier"));

        assertThat(storage.getList("upsells")).containsOnly("mid-tier", "high-tier");
    }

    @Test
    public void updateReplacesEntireValueList() {
        storage.update("upsells", Arrays.asList("mid-tier", "high-tier"));

        storage.update("upsells", Arrays.asList("mid-tier"));

        assertThat(storage.getList("upsells")).containsExactly("mid-tier");
    }

    @Test
    public void returnsEmptyListIfNotSet() {
        assertThat(storage.getList("upsells")).isEmpty();
    }

    @Test
    public void clearRemovesStoredValues() {
        storage.update("plan", "mid-tier");

        storage.clear();

        assertThat(storage.get("plan", "none")).isEqualTo("none");
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