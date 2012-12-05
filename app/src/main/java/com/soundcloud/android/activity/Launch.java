package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.fetch.ResolveFetchTask;
import com.soundcloud.android.utils.AndroidUtils;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

public class Launch extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getApp().getAccount() == null) {
                    getApp().addAccount(Launch.this);
                    overridePendingTransition(0, 0);
                } else {
                    startActivity(new Intent(Launch.this, Home.class));
                }
            }
        }, 400);
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }
}


