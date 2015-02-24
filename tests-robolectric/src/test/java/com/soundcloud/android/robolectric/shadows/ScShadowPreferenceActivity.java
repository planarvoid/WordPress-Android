package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.shadows.ShadowPreferenceActivity;

import android.preference.Preference;
import android.preference.PreferenceActivity;

@Implements(PreferenceActivity.class)
public class ScShadowPreferenceActivity extends ShadowPreferenceActivity {

    @Implementation
    public Preference findPreference(java.lang.CharSequence key) {
        return getPreferenceScreen().findPreference(key);
    }


}
