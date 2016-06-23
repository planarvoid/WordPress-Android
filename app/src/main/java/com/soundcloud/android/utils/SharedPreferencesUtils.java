package com.soundcloud.android.utils;

import android.preference.ListPreference;
import android.preference.Preference;
import android.text.TextUtils;

public final class SharedPreferencesUtils {

    private SharedPreferencesUtils() {
    }

    /**
     * Creates a preference which has the current value in the title
     *
     * @param list    the list preference
     * @param titleId the title id string resource
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
