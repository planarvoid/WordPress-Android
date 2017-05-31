package com.soundcloud.android.cast.activity;

import com.soundcloud.android.SoundCloudApplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

public abstract class CastRedirectActivity extends AppCompatActivity {

    public CastRedirectActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.startActivity(getRedirectionIntent());
        finish();
    }

    protected abstract Intent getRedirectionIntent();
}
