package com.soundcloud.android.settings;

import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;

public class LicensesActivity extends ScActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, LicensesFragment.create())
                .commit();
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

}