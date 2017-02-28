package com.soundcloud.android.cast.activity;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.SoundCloudApplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public abstract class CastRedirectActivity extends AppCompatActivity {

    @Inject Navigator navigator;

    public CastRedirectActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.startActivity(getRedirectionIntent(navigator));
        finish();
    }

    protected abstract Intent getRedirectionIntent(Navigator navigator);
}
