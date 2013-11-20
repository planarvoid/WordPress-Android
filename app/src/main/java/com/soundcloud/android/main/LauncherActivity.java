package com.soundcloud.android.main;

import static com.soundcloud.android.rx.observers.RxObserverHelper.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class LauncherActivity extends Activity {
    private AccountOperations mAccountOperations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch);
        mAccountOperations = new AccountOperations(this);
        fireAndForget(UnauthorisedRequestRegistry.getInstance(this).clearObservedUnauthorisedRequestTimestampAsync());

    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (mAccountOperations.soundCloudAccountExists()) {
                    startActivity(new Intent(LauncherActivity.this, MainActivity.class));
                } else {
                    mAccountOperations.addSoundCloudAccountManually(LauncherActivity.this);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}