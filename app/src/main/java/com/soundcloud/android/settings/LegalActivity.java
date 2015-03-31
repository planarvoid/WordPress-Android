package com.soundcloud.android.settings;

import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;

public class LegalActivity extends ScActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, LegalFragment.create())
                .commit();
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

}