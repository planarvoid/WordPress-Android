package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Token;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;

public class Start extends AccountAuthenticatorActivity {
    private static final int RECOVER_CODE = 1;
    private static final int SUGGESTED_USERS = 2;

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.start);

        findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivityForResult(new Intent(Start.this, Facebook.class), 0);
            }
        });

        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivityForResult(new Intent(Start.this, Login.class), 0);
            }
        });

        findViewById(R.id.signup_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivityForResult(new Intent(Start.this, SignUp.class), 0);
            }
        });

        Button forgotBtn = (Button) this.findViewById(R.id.forgot_btn);
        forgotBtn.setText(Html.fromHtml("<u>"+getResources().getString(R.string.authentication_I_forgot_my_password)
                + "</u>"));
        forgotBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Start.this, Recover.class), RECOVER_CODE);
            }
        });

        if (bundle == null) {
            findViewById(R.id.animation_holder).setVisibility(View.INVISIBLE);
            Animation animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(700);

            ((ViewGroup) findViewById(R.id.animation_holder)).setLayoutAnimation(
                    new LayoutAnimationController(animation, 0.25f));
            mHandler.postDelayed(mShowAuthControls, 350);
        }
    }

    private Runnable mShowAuthControls = new Runnable() {
        public void run() {
            if (!Start.this.isFinishing()){
                findViewById(R.id.animation_holder).setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case RESULT_OK:
                handleActivityResult(requestCode, data);
                break;
        }
    }

    private void handleActivityResult(int requestCode, Intent data) {
        switch (requestCode) {
            case 0:
                SoundCloudApplication app = (SoundCloudApplication) getApplication();

                User user = data.getParcelableExtra("user");
                Token token = (Token) data.getSerializableExtra("token");

                String signed_up = data.getStringExtra("signed_up");

                // native signup will already have created the account
                if ("native".equals(signed_up) || app.addUserAccount(user, token)) {
                    final Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
                    setAccountAuthenticatorResult(result);
                    if (signed_up != null) {
                        Intent i = new Intent(this, SuggestedUsers.class);
                        i.putExtra("facebook_connected","facebook".equals(signed_up));
                        startActivityForResult(new Intent(this, SuggestedUsers.class), SUGGESTED_USERS);
                    } else {
                        finish();
                    }
                } else {
                    //showError
                }
                break;
            case SUGGESTED_USERS:
                handleSuggestedUsersReturned(data);
                break;

            case RECOVER_CODE:
                handleRecoverResult(this, data);
                break;
        }
    }

    private void handleSuggestedUsersReturned(Intent data) {

        finish();
    }

    static void handleRecoverResult(Context context, Intent data) {
        final boolean success = data.getBooleanExtra("success", false);
        if (success) {
            CloudUtils.showToast(context, R.string.authentication_recover_password_success);
        } else {
            String error = data.getStringExtra("error");
            CloudUtils.showToast(context,
                        error == null ?
                        context.getString(R.string.authentication_recover_password_failure) :
                        context.getString(R.string.authentication_recover_password_failure_reason, error));
        }
    }
}
