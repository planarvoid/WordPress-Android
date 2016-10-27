package com.soundcloud.android.cast;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.SoundCloudApplication;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class CastRedirectActivity extends AppCompatActivity {

    @Inject Navigator navigator;

    public CastRedirectActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.startActivity(navigator.createHomeIntentFromNotification(this));
        finish();
    }

}
