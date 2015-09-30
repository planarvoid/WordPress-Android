package com.soundcloud.android.settings;

import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class LicensesActivity extends ScActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentFragment(LicensesFragment.create());
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setContainerLayout(this);
    }

}