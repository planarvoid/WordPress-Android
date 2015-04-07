package com.soundcloud.android.settings;

import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;

public class LicensesActivity extends ScActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentFragment(LicensesFragment.create());
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

}