package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.LeakCanaryWrapper;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import javax.inject.Inject;

public class LicensesFragment extends PreferenceFragment {

    @Inject LeakCanaryWrapper leakCanaryWrapper;

    public static LicensesFragment create() {
        return new LicensesFragment();
    }

    public LicensesFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.licenses);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        leakCanaryWrapper.watch(this);
    }

}
