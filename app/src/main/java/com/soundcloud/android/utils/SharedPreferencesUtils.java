package com.soundcloud.android.utils;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class SharedPreferencesUtils {
    private static final Method sApplyMethod = findApplyMethod();
    private SharedPreferencesUtils() {}

    private static Method findApplyMethod() {
        try {
            return SharedPreferences.Editor.class.getMethod("apply");
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    /**
     * Applies or commits the current editor.
     * Apply (async commit) is only available in SDK >= 9.
     * @param editor the editor to apply
     * @return if applying, true, otherwise return value of commit()
     */
    public static boolean apply(SharedPreferences.Editor editor) {
        if (sApplyMethod != null) {
            try {
                sApplyMethod.invoke(editor);
                return true;
            } catch (InvocationTargetException ignore) {
                // fall through
            } catch (IllegalAccessException ignore) {
                // fall through
            }
        }
        return editor.commit();
    }


    /**
     * Creates a preference which has the current value in the title
     * @param list the list preference
     * @param titleId the title id string resource
     * @param key the preference key
     */
    public static void listWithLabel(final ListPreference list, final int titleId) {
        list.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        CharSequence label = list.getEntries()[list.findIndexOfValue(o.toString())];
                        preference.setTitle(list.getContext().getString(titleId) + " (" + label + ")");
                        return true;
                    }
                }
        );
        CharSequence entry = list.getEntry();
        if (!TextUtils.isEmpty(entry)) {
            list.setTitle(list.getContext().getString(titleId) + " (" + entry + ")");
        }
    }
}
