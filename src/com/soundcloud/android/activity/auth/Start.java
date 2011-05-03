package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.User;
import com.soundcloud.api.Token;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
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
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_start);

        findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivityForResult(new Intent(Start.this, Facebook.class), 0);
            }
        });

        this.findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivityForResult(new Intent(Start.this, Login.class), 0);
            }
        });

        this.findViewById(R.id.signup_btn).setOnClickListener(new View.OnClickListener() {
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
                startActivityForResult(new Intent(Start.this, Recover.class), 0);
            }
        });

        if (savedInstanceState == null){
            findViewById(R.id.animation_holder).setVisibility(View.INVISIBLE);
            Animation animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(700);

            ((ViewGroup) findViewById(R.id.animation_holder)).setLayoutAnimation(
                    new LayoutAnimationController(animation, 0.25f));
            mHandler.postDelayed(mShowAuthControls, 500);
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
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data + ")");
        if (resultCode == RESULT_OK) {
            SoundCloudApplication app = (SoundCloudApplication) getApplication();

            User user = data.getParcelableExtra("user");
            Token token = (Token) data.getSerializableExtra("token");

            if (app.addUserAccount(user, token)) {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
                setAccountAuthenticatorResult(result);

                finish();
            } else {
                //showError
            }
        }
    }
}
