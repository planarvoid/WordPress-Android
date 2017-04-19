package com.soundcloud.android.utils;

import com.soundcloud.java.optional.Optional;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Dummy activity do replace the dependency of other OS or library activites like when selecting a user account from the system.
 */
public class TestGplusRegistrationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Optional.fromNullable(getIntent().getAction()).ifPresent(action -> {
            switch (action) {
                case "com.google.android.gms.common.account.CHOOSE_ACCOUNT":
                    setAccountPickerResult();
                    break;
                default:
                    throw new IllegalArgumentException("Activity does not handle action=" + action);
            }
        });
        finish();
    }

    private void setAccountPickerResult() {
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, "test123");
        setResult(RESULT_OK, intent);
    }
}
