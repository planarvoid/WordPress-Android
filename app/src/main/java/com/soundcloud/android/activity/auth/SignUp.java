package com.soundcloud.android.activity.auth;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

public class SignUp extends RelativeLayout {
    public interface SignUpHandler {
        void onFacebookLogin();
        void onSignUp(String email, String password);
        void onTermsOfUse();
    }

    public SignUp(Context context) {
        super(context);
    }

    public SignUp(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SignUp(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private static final int MIN_PASSWORD_LENGTH = 4;

    @Nullable private SignUpHandler mSignUpHandler;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();
        final SoundCloudApplication app = SoundCloudApplication.fromContext(context);

        final EditText emailField          = (EditText) findViewById(R.id.txt_email_address);
        final EditText choosePasswordField = (EditText) findViewById(R.id.txt_choose_a_password);
        final EditText repeatPasswordField = (EditText) findViewById(R.id.txt_repeat_your_password);
        final Button   signupButtton       = (Button)   findViewById(R.id.btn_signup);
        final Button   facebookButton      = (Button)   findViewById(R.id.facebook_btn);

        emailField.setText(AndroidUtils.suggestEmail(context));

        repeatPasswordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @SuppressWarnings({"SimplifiableIfStatement"})
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean done         = actionId == EditorInfo.IME_ACTION_DONE;
                boolean pressedEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean downAction   = event != null && event.getAction() == KeyEvent.ACTION_DOWN;

                if (done || pressedEnter && downAction) {
                    return signupButtton.performClick();
                } else {
                    return false;
                }
            }
        });

        signupButtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup_done);

                if (emailField.getText().length() == 0 ||
                        choosePasswordField.getText().length() == 0 ||
                        repeatPasswordField.getText().length() == 0) {
                    AndroidUtils.showToast(context, R.string.authentication_error_incomplete_fields);
                } else if (!ScTextUtils.isEmail(emailField.getText())) {
                    AndroidUtils.showToast(context, R.string.authentication_error_invalid_email);
                } else if (!choosePasswordField.getText().toString().equals(repeatPasswordField.getText().toString())) {
                    AndroidUtils.showToast(context, R.string.authentication_error_password_mismatch);
                } else if (!checkPassword(choosePasswordField.getText())) {
                    AndroidUtils.showToast(context, R.string.authentication_error_password_too_short);
                } else {
                    final String email = emailField.getText().toString();
                    final String password = choosePasswordField.getText().toString();

                    if (mSignUpHandler != null) {
                        mSignUpHandler.onSignUp(email, password);
                    }
                }
            }
        });

        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSignUpHandler != null) {
                    mSignUpHandler.onFacebookLogin();
                }
            }
        });

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_terms_of_use),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (mSignUpHandler != null) {
                            mSignUpHandler.onTermsOfUse();
                        };
                    }
                }, true);
    }

    static boolean checkPassword(CharSequence password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    @Nullable
    public SignUpHandler getSignUpHandler() {
        return mSignUpHandler;
    }

    public void setSignUpHandler(@Nullable SignUpHandler mSignUpHandler) {
        this.mSignUpHandler = mSignUpHandler;
    }
}
