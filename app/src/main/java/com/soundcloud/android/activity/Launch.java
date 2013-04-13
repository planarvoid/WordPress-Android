package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.landing.Home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class Launch extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (getApp().getAccount() == null) {
                    getApp().addAccount(Launch.this);
                } else {
                    startActivity(new Intent(Launch.this, Home.class));
                }
            }
        });
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }
}