package com.soundcloud.android.settings;

import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;

public class ConfigurationFeaturesActivity extends ScActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentFragment(ConfigurationFeaturesFragment.create());
    }

}