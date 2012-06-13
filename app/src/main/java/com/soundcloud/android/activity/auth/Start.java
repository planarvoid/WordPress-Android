package com.soundcloud.android.activity.auth;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Token;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;

public class Start extends AccountAuthenticatorActivity {
    private static final int RECOVER_CODE    = 1;
    private static final int SUGGESTED_USERS = 2;

    public static final String FB_CONNECTED_EXTRA = "facebook_connected";
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.start);

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();

        findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Login_with_facebook);
                startActivityForResult(new Intent(Start.this, Facebook.class), 0);
            }
        });

        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Login);
                startActivityForResult(new Intent(Start.this, Login.class), 0);
            }
        });

        findViewById(R.id.signup_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup);
                startActivityForResult(new Intent(Start.this, SignUp.class), 0);
            }
        });

        Button forgotBtn = (Button) this.findViewById(R.id.forgot_btn);
        forgotBtn.setText(Html.fromHtml("<u>" +
                getResources().getString(R.string.authentication_I_forgot_my_password)
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

            mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (!Start.this.isFinishing()) {
                        findViewById(R.id.animation_holder).setVisibility(View.VISIBLE);
                    }
                }
            }, 350);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SoundCloudApplication)getApplication()).track(Page.Entry_main);
    }

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
                final String error = data.getStringExtra("error");
                if (error == null) {
                    final User user = data.getParcelableExtra("user");
                    final Token token = (Token) data.getSerializableExtra("token");

                    SignupVia via = SignupVia.fromIntent(data);
                    // API signup will already have created the account
                    if (SignupVia.API == via || app.addUserAccount(user, token, via)) {
                        final Bundle result = new Bundle();
                        result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
                        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
                        setAccountAuthenticatorResult(result);

                        sendBroadcast(new Intent(Actions.ACCOUNT_ADDED)
                                .putExtra("user", user)
                                .putExtra("signed_up", via.name));

                        if (via != SignupVia.UNKNOWN) {
                            startActivityForResult(
                                new Intent(this, SuggestedUsers.class).putExtra(FB_CONNECTED_EXTRA, via.isFacebook()),
                                    SUGGESTED_USERS);
                        } else {
                            finish();
                        }
                    } else {
                        CloudUtils.showToast(this, R.string.error_creating_account);
                    }
                } else {
                    CloudUtils.showToast(this, error);
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
