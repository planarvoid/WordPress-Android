package com.soundcloud.android.utils;

import com.soundcloud.android.crypto.Obfuscator;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * SharedPreferences implementation that transparently obfuscates the stored values.
 * Only supports: String, StringSet, Boolean
 */
public class ObfuscatedPreferences implements SharedPreferences {

    private final Obfuscator obfuscator;
    private final SharedPreferences wrappedPrefs;

    private final WeakHashMap<OnSharedPreferenceChangeListener, ObfuscatedOnSharedPreferenceChangeListener> listeners = new WeakHashMap<>();

    @Inject
    public ObfuscatedPreferences(SharedPreferences wrappedPrefs, Obfuscator obfuscator) {
        this.wrappedPrefs = wrappedPrefs;
        this.obfuscator = obfuscator;
    }

    @Nullable
    @Override
    public String getString(String key, String defValue) {
        String obfuscatedValue = wrappedPrefs.getString(obfuscator.obfuscate(key), null);
        if (obfuscatedValue == null) {
            return defValue;
        } else {
            return obfuscator.deobfuscateString(obfuscatedValue);
        }
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Set<String> deobfuscatedValues = new HashSet<>();
        Set<String> obfuscaedValues = wrappedPrefs.getStringSet(obfuscator.obfuscate(key), null);
        if (obfuscaedValues == null) {
            return defValues;
        } else {
            for (String value : obfuscaedValues) {
                deobfuscatedValues.add(obfuscator.deobfuscateString(value));
            }
            return deobfuscatedValues;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return toBoolean(wrappedPrefs.getString(obfuscator.obfuscate(key), null), defValue);
    }

    private boolean toBoolean(String value, boolean defaultValue) {
        return value == null
                ? defaultValue
                : obfuscator.deobfuscateBoolean(value);
    }

    @Override
    public boolean contains(String key) {
        return wrappedPrefs.contains(obfuscator.obfuscate(key));
    }

    @Override
    public Editor edit() {
        return new ObfuscatedEditor();
    }

    @Override
    public Map<String, ?> getAll() {
        throw notImplemented();
    }

    @Override
    public int getInt(String key, int defValue) {
        throw notImplemented();
    }

    @Override
    public long getLong(String key, long defValue) {
        throw notImplemented();
    }

    @Override
    public float getFloat(String key, float defValue) {
        throw notImplemented();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener register) {
        synchronized(this) {
            ObfuscatedOnSharedPreferenceChangeListener listener = new ObfuscatedOnSharedPreferenceChangeListener(register);
            listeners.put(register, listener);
            wrappedPrefs.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener unregister) {
        synchronized(this) {
            for (Map.Entry<OnSharedPreferenceChangeListener, ObfuscatedOnSharedPreferenceChangeListener> entry : listeners.entrySet()) {
                if (entry.getKey().equals(unregister)) {
                    listeners.remove(entry.getKey());
                    wrappedPrefs.unregisterOnSharedPreferenceChangeListener(entry.getValue());
                }
            }
        }
    }

    public class ObfuscatedEditor implements SharedPreferences.Editor {

        private final SharedPreferences.Editor wrappedEditor;

        public ObfuscatedEditor() {
            wrappedEditor = wrappedPrefs.edit();
        }

        @Override
        public Editor putString(String key, String value) {
            wrappedEditor.putString(obfuscator.obfuscate(key), obfuscator.obfuscate(value));
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            Set<String> obfuscatedValues = new HashSet<>();
            for (String value : values) {
                obfuscatedValues.add(obfuscator.obfuscate(value));
            }
            wrappedEditor.putStringSet(obfuscator.obfuscate(key), obfuscatedValues);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            wrappedEditor.putString(obfuscator.obfuscate(key), obfuscator.obfuscate(value));
            return this;
        }

        @Override
        public Editor remove(String key) {
            return wrappedEditor.remove(obfuscator.obfuscate(key));
        }

        @Override
        public Editor clear() {
            return wrappedEditor.clear();
        }

        @Override
        public boolean commit() {
            return wrappedEditor.commit();
        }

        @Override
        public void apply() {
            wrappedEditor.apply();
        }

        @Override
        public Editor putInt(String key, int value) {
            throw notImplemented();
        }

        @Override
        public Editor putLong(String key, long value) {
            throw notImplemented();
        }

        @Override
        public Editor putFloat(String key, float value) {
            throw notImplemented();
        }
    }

    public class ObfuscatedOnSharedPreferenceChangeListener implements OnSharedPreferenceChangeListener {

        private final OnSharedPreferenceChangeListener wrappedListener;

        public ObfuscatedOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener wrappedListener) {
            this.wrappedListener = wrappedListener;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            wrappedListener.onSharedPreferenceChanged(ObfuscatedPreferences.this, obfuscator.deobfuscateString(key));
        }
    }

    private RuntimeException notImplemented() {
        return new UnsupportedOperationException("Not implemented in " + this.getClass().getSimpleName());
    }

}
