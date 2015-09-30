package com.soundcloud.android.settings;

import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class LegalActivity extends ScActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentFragment(LegalFragment.create());
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