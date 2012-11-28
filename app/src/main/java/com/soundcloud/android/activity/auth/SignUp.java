package com.soundcloud.android.activity.auth;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
    private static final String BUNDLE_EMAIL    = "BUNDLE_EMAIL";
    private static final String BUNDLE_PASSWORD = "BUNDLE_PASSWORD";
    private static final String BUNDLE_CONFIRM  = "BUNDLE_CONFIRM";

    public interface SignUpHandler {
        void onSignUp(String email, String password);
        void onCancelSignUp();
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
        final Button   signUpButton       = (Button)   findViewById(R.id.btn_signup);
        final Button   cancelButton       = (Button)   findViewById(R.id.btn_cancel);

        emailField.setText(AndroidUtils.suggestEmail(context));

        repeatPasswordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @SuppressWarnings({"SimplifiableIfStatement"})
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean done = actionId == EditorInfo.IME_ACTION_DONE;
                boolean pressedEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean downAction = event != null && event.getAction() == KeyEvent.ACTION_DOWN;

                if (done || pressedEnter && downAction) {
                    return signUpButton.performClick();
                } else {
                    return false;
                }
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
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

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getSignUpHandler() != null) {
                    getSignUpHandler().onCancelSignUp();
                }

                InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(emailField.getWindowToken(), 0);
                imm.hideSoftInputFromWindow(choosePasswordField.getWindowToken(), 0);
                imm.hideSoftInputFromWindow(repeatPasswordField.getWindowToken(), 0);
            }
        });

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_terms_of_use),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (mSignUpHandler != null) {
                            mSignUpHandler.onTermsOfUse();
                        }
                        ;
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

    public Bundle getStateBundle() {
        EditText emailField          = (EditText) findViewById(R.id.txt_email_address);
        EditText choosePasswordField = (EditText) findViewById(R.id.txt_choose_a_password);
        EditText repeatPasswordField = (EditText) findViewById(R.id.txt_repeat_your_password);

        Bundle bundle = new Bundle();
        bundle.putCharSequence(BUNDLE_EMAIL,    emailField.getText());
        bundle.putCharSequence(BUNDLE_PASSWORD, choosePasswordField.getText());
        bundle.putCharSequence(BUNDLE_CONFIRM,  repeatPasswordField.getText());
        return bundle;
    }

    public void setState(@Nullable Bundle bundle) {
        if (bundle == null) return;

        EditText emailField          = (EditText) findViewById(R.id.txt_email_address);
        EditText choosePasswordField = (EditText) findViewById(R.id.txt_choose_a_password);
        EditText repeatPasswordField = (EditText) findViewById(R.id.txt_repeat_your_password);

        emailField.setText(bundle.getCharSequence(BUNDLE_EMAIL));
        choosePasswordField.setText(bundle.getCharSequence(BUNDLE_PASSWORD));
        repeatPasswordField.setText(bundle.getCharSequence(BUNDLE_CONFIRM));
    }
}
