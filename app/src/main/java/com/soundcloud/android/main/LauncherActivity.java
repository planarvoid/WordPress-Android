package com.soundcloud.android.main;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class LauncherActivity extends TrackedActivity {

    private AccountOperations accountOperations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch);
        accountOperations = SoundCloudApplication.fromContext(this).getAccountOperations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (accountOperations.isUserLoggedIn()) {
                    startActivity(new Intent(LauncherActivity.this, MainActivity.class));
                } else {
                    accountOperations.triggerLoginFlow(LauncherActivity.this);
                }
            }
        });
    }

}