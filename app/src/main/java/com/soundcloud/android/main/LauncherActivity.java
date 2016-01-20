package com.soundcloud.android.main;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import javax.inject.Inject;

public class LauncherActivity extends TrackedActivity {

    @Inject AccountOperations accountOperations;
    @Inject EventBus eventBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.launch);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (accountOperations.isUserLoggedIn()) {
                    startActivity(getMainActivityIntentWithExtras());
                } else {
                    accountOperations.triggerLoginFlow(LauncherActivity.this);
                }
            }
        });
    }

    private Intent getMainActivityIntentWithExtras() {
        final Intent intent = new Intent(this, MainActivity.class);
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }

        if (!Referrer.hasReferrer(intent)) {
            Referrer.HOME_BUTTON.addToIntent(intent);
        }

        if (!Screen.hasScreen(intent)) {
            Screen.UNKNOWN.addToIntent(intent);
        }

        return intent;
    }

}
