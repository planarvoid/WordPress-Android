package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.testsupport.PlatformUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class ObfuscatedPreferencesTest extends PlatformUnitTest {

    public static final String KEY = "my_key";
    public static final String MOCK_OBFUSCATION = "-obfuscated";

    private SharedPreferences wrappedPreferences;
    private ObfuscatedPreferences preferences;

    @Mock private Obfuscator obfuscator;
    @Mock private SharedPreferences.OnSharedPreferenceChangeListener changeListener;

    @Before
    public void setUp() throws Exception {
        configureMockObfuscation();
        wrappedPreferences = sharedPreferences("test", Context.MODE_PRIVATE);
        preferences = new ObfuscatedPreferences(wrappedPreferences, obfuscator);
    }

    @Test
    public void obfuscatesKey() {
        preferences.edit().putString(KEY, "my_value").apply();

        verify(obfuscator).obfuscate(KEY);
        assertThat(wrappedPreferences.contains(mockObfuscate(KEY))).isTrue();
    }

    @Test
    public void obfuscatesStoredString() {
        preferences.edit().putString(KEY, "my_value").apply();

        verify(obfuscator).obfuscate("my_value");
        assertThat(wrappedPreferences.getString(mockObfuscate(KEY), null)).isEqualTo(mockObfuscate("my_value"));
    }

    @Test
    public void deobfuscatesStoredString() {
        preferences.edit().putString(KEY, "my_value").apply();

        assertThat(preferences.getString(KEY, null)).isEqualTo("my_value");
    }

    @Test
    public void defaultStringIsReturnedIfNotStored() {
        assertThat(preferences.getString(KEY, "default")).isEqualTo("default");
    }

    @Test
    public void obfuscatesStoredBoolean() {
        preferences.edit().putBoolean(KEY, true).apply();

        verify(obfuscator).obfuscate(true);
        assertThat(wrappedPreferences.getString(mockObfuscate(KEY), null)).isEqualTo(mockObfuscate("true"));
    }

    @Test
    public void deobfuscatesStoredBoolean() {
        preferences.edit().putBoolean(KEY, true).apply();

        assertThat(preferences.getBoolean(KEY, false)).isTrue();
    }

    @Test
    public void defaultBooleanIsReturnedIfNotStored() {
        assertThat(preferences.getBoolean(KEY, true)).isTrue();
    }

    @Test
    public void obfuscatesStoredStringSet() {
        Set<String> strings = new HashSet<>();
        strings.add("a");
        strings.add("b");

        preferences.edit().putStringSet(KEY, strings).apply();

        verify(obfuscator).obfuscate("a");
        verify(obfuscator).obfuscate("b");
        assertThat(wrappedPreferences.getStringSet(mockObfuscate(KEY), null)).containsOnly(mockObfuscate("a"), mockObfuscate("b"));
    }

    @Test
    public void deobfuscatesStoredStringSet() {
        Set<String> strings = new HashSet<>();
        strings.add("a");
        strings.add("b");

        preferences.edit().putStringSet(KEY, strings).apply();

        assertThat(preferences.getStringSet(KEY, null)).containsOnly("a", "b");
    }

    @Test
    public void defaultStringSetIsReturnedIfNotStored() {
        Set<String> strings = new HashSet<>();
        strings.add("c");

        assertThat(preferences.getStringSet(KEY, strings)).containsOnly("c");
    }

    @Test
    public void registersChangeListener() {
        preferences.registerOnSharedPreferenceChangeListener(changeListener);

        preferences.edit().putBoolean(KEY, true).apply();

        verify(changeListener).onSharedPreferenceChanged(preferences, KEY);
    }

    @Test
    public void unregistersChangeListener() {
        preferences.registerOnSharedPreferenceChangeListener(changeListener);
        preferences.unregisterOnSharedPreferenceChangeListener(changeListener);

        preferences.edit().putBoolean(KEY, true).apply();

        verify(changeListener, never()).onSharedPreferenceChanged(preferences, KEY);
    }

    private void configureMockObfuscation() throws Exception {
        when(obfuscator.obfuscate(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return mockObfuscate(invocation.getArguments()[0].toString());
            }
        });
        when(obfuscator.deobfuscateString(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return mockDeobfuscate(invocation.getArguments()[0].toString());
            }
        });
        when(obfuscator.obfuscate(anyBoolean())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return mockObfuscate(invocation.getArguments()[0].toString());
            }
        });
        when(obfuscator.deobfuscateBoolean(anyString())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return Boolean.parseBoolean(mockDeobfuscate(invocation.getArguments()[0].toString()));
            }
        });
    }

    private String mockObfuscate(String input) {
        return input + MOCK_OBFUSCATION;
    }

    private String mockDeobfuscate(String input) {
        return input.substring(0, input.length() - MOCK_OBFUSCATION.length());
    }

}