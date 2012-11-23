package com.soundcloud.android.activity.auth;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.*;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import org.jetbrains.annotations.Nullable;

public class Login extends RelativeLayout {
    public interface LoginHandler {
        void onLogin(String email, String password);
        void onRecover(String email);
        void onFacebookLogin();
    }

    @Nullable private LoginHandler mLoginHandler;

    public Login(Context context) {
        super(context);
    }

    public Login(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Login(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();
        final SoundCloudApplication app = SoundCloudApplication.fromContext(context);

        final EditText emailField     = (EditText) findViewById(R.id.txt_email_address);
        final EditText passwordField  = (EditText) findViewById(R.id.txt_password);
        final Button   loginButton    = (Button)   findViewById(R.id.btn_login);
        final Button   facebookButton = (Button)   findViewById(R.id.facebook_btn);

        emailField.setText(AndroidUtils.suggestEmail(context));

        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @SuppressWarnings({"SimplifiableIfStatement"})
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean done         = actionId == EditorInfo.IME_ACTION_DONE;
                boolean pressedEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean downAction   = event != null && event.getAction() == KeyEvent.ACTION_DOWN;

                if (done || pressedEnter && downAction) {
                    return loginButton.performClick();
                } else {
                    return false;
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailField.getText().length() == 0 || passwordField.getText().length() == 0) {
                    AndroidUtils.showToast(context, R.string.authentication_error_incomplete_fields);
                } else {
                    app.track(Click.Login_Login_done);

                    final String email = emailField.getText().toString();
                    final String password = passwordField.getText().toString();

                    if (mLoginHandler != null) {
                        mLoginHandler.onLogin(email, password);
                    }
                }
            }
        });

        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginHandler != null) {
                    mLoginHandler.onFacebookLogin();
                }
            }
        });

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_I_forgot_my_password),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (mLoginHandler != null) {
                            mLoginHandler.onRecover(emailField.getText().toString());
                        }
                    }
                }, true);
    }

    public LoginHandler getLoginHandler() {
        return mLoginHandler;
    }

    public void setLoginHandler(LoginHandler mLoginHandler) {
        this.mLoginHandler = mLoginHandler;
    }
}
