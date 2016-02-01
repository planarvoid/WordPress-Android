package com.soundcloud.android.configuration;

import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class ConfigurationUpdateLightCycle extends DefaultActivityLightCycle<AppCompatActivity> {

    private final ConfigurationManager configurationManager;

    @Inject
    ConfigurationUpdateLightCycle(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    @Override
    public void onStart(AppCompatActivity activity) {
        configurationManager.requestUpdate();
    }
}
