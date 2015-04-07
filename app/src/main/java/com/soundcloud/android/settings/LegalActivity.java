package com.soundcloud.android.settings;

import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;

public class LegalActivity extends ScActivity {

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

}