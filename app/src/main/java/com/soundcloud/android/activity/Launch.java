package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.activity.landing.Home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class Launch extends Activity {
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
                    startActivity(new Intent(Launch.this, Home.class));
                } else {
                    accountOperations.addSoundCloudAccountManually(Launch.this);
                }
            }
        });
    }

}