package com.soundcloud.android.main;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.rx.eventbus.EventBus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class LauncherActivity extends TrackedActivity {

    private AccountOperations accountOperations;

    public LauncherActivity() {
    }

    public LauncherActivity(AccountOperations accountOperations, EventBus eventBus) {
        super(eventBus);
        this.accountOperations = accountOperations;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch);
        getEventBus().publish(EventQueue.TRACKING, ForegroundEvent.open(Screen.UNKNOWN, Referrer.HOME_BUTTON));
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (getAccountOperations().isUserLoggedIn()) {
                    startActivity(getMainActivityIntentWithExtras());
                } else {
                    getAccountOperations().triggerLoginFlow(LauncherActivity.this);
                }
            }
        });
    }

    private AccountOperations getAccountOperations() {
        return accountOperations == null ? SoundCloudApplication.fromContext(this).getAccountOperations() : accountOperations;
    }

    private Intent getMainActivityIntentWithExtras() {
        final Intent intent = new Intent(this, MainActivity.class);
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        return intent;
    }

}