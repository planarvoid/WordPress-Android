package com.soundcloud.android.main;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class Launch extends Activity {
    private AccountOperations mAccountOperations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch);
        mAccountOperations = new AccountOperations(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (mAccountOperations.soundCloudAccountExists()) {
                    startActivity(new Intent(Launch.this, MainActivity.class));
                } else {
                    mAccountOperations.addSoundCloudAccountManually(Launch.this);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}