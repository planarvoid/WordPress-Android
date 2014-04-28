package com.soundcloud.android.main;

import com.soundcloud.android.R;
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
        accountOperations = new AccountOperations(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (accountOperations.soundCloudAccountExists()) {
                    startActivity(new Intent(LauncherActivity.this, MainActivity.class));
                } else {
                    accountOperations.addSoundCloudAccountManually(LauncherActivity.this);
                }
            }
        });
    }

}