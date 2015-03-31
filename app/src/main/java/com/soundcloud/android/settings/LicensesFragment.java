package com.soundcloud.android.settings;

import com.soundcloud.android.R;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class LicensesFragment extends PreferenceFragment {

    public static LicensesFragment create() {
        return new LicensesFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.licenses);
    }

}
