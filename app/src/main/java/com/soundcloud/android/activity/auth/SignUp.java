package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SignUp extends AuthLayout {
    private static final String BUNDLE_EMAIL    = "BUNDLE_EMAIL";
    private static final String BUNDLE_PASSWORD = "BUNDLE_PASSWORD";
    private Button mSignUpButton;

    private boolean mEmailValid, mPasswordValid;
    private Drawable mValidDrawable, mPlaceholderDrawable;

    public interface SignUpHandler extends AuthHandler {
        void onSignUp(String email, String password);
        void onCancelSignUp();
        void onShowTermsOfUse();
        void onShowPrivacyPolicy();
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

    private static final int MIN_PASSWORD_LENGTH = 6;

    @Nullable private SignUpHandler mSignUpHandler;

    @Override
    AuthHandler getAuthHandler() {
        return mSignUpHandler;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();
        final SoundCloudApplication app = SoundCloudApplication.fromContext(context);

        final AutoCompleteTextView emailField = (AutoCompleteTextView)  findViewById(R.id.auto_txt_email_address);
        final EditText passwordField = (EditText) findViewById(R.id.txt_choose_a_password);
        final Button   cancelButton       = (Button)   findViewById(R.id.btn_cancel);
        mSignUpButton = (Button)   findViewById(R.id.btn_signup);

        mValidDrawable = getResources().getDrawable(R.drawable.ic_done_dark_sm);
        mPlaceholderDrawable = new ColorDrawable(Color.TRANSPARENT);
        mPlaceholderDrawable.setBounds(0, 0, mValidDrawable.getIntrinsicWidth(), mValidDrawable.getIntrinsicHeight());


        emailField.addTextChangedListener(new InputValidator(emailField) {
            @Override
            boolean validate(String text) {
                mEmailValid = ScTextUtils.isEmail(text);
                return mEmailValid;
            }
        });

        passwordField.addTextChangedListener(new InputValidator(passwordField) {
            @Override
            boolean validate(String text) {
                mPasswordValid = checkPassword(text);
                return mPasswordValid;
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(), android.R.layout.simple_dropdown_item_1line, AndroidUtils.listEmails(getContext()));
        emailField.setAdapter(adapter);
        emailField.setThreshold(0);

        findViewById(R.id.google_plus_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignUpHandler.onGooglePlusAuth();
            }
        });
        findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignUpHandler.onFacebookAuth();
            }
        });

        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @SuppressWarnings({"SimplifiableIfStatement"})
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean done = actionId == EditorInfo.IME_ACTION_DONE;
                boolean pressedEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean downAction = event != null && event.getAction() == KeyEvent.ACTION_DOWN;

                if (done || pressedEnter && downAction) {
                    return mSignUpButton.performClick();
                } else {
                    return false;
                }
            }
        });

        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup_done);

                if (emailField.getText().length() == 0 || passwordField.getText().length() == 0) {
                    AndroidUtils.showToast(context, R.string.authentication_error_incomplete_fields);
                } else if (!ScTextUtils.isEmail(emailField.getText())) {
                    AndroidUtils.showToast(context, R.string.authentication_error_invalid_email);
                } else if (!checkPassword(passwordField.getText())) {
                    AndroidUtils.showToast(context, R.string.authentication_error_password_too_short);
                } else {
                    final String email = emailField.getText().toString();
                    final String password = passwordField.getText().toString();

                    hideKeyboardOnSignup(emailField, passwordField);

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

                hideKeyboardOnSignup(emailField, passwordField);
            }
        });

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_terms_of_use),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (mSignUpHandler != null) {
                            mSignUpHandler.onShowTermsOfUse();
                        }
                    }
                }, false, false);

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.privacy),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (mSignUpHandler != null) {
                            mSignUpHandler.onShowPrivacyPolicy();
                        }
                    }
                }, false, false);

        validateForm();
    }

    private void hideKeyboardOnSignup(AutoCompleteTextView emailField, EditText passwordField) {
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(emailField.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(passwordField.getWindowToken(), 0);
    }

    private void validateForm() {
        mSignUpButton.setEnabled(mEmailValid && mPasswordValid);
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
        EditText emailField          = (EditText) findViewById(R.id.auto_txt_email_address);
        EditText choosePasswordField = (EditText) findViewById(R.id.txt_choose_a_password);

        Bundle bundle = new Bundle();
        bundle.putCharSequence(BUNDLE_EMAIL,    emailField.getText());
        bundle.putCharSequence(BUNDLE_PASSWORD, choosePasswordField.getText());
        return bundle;
    }

    public void setState(@Nullable Bundle bundle) {
        if (bundle == null) return;

        EditText emailField          = (EditText) findViewById(R.id.auto_txt_email_address);
        EditText choosePasswordField = (EditText) findViewById(R.id.txt_choose_a_password);

        emailField.setText(bundle.getCharSequence(BUNDLE_EMAIL));
        choosePasswordField.setText(bundle.getCharSequence(BUNDLE_PASSWORD));
        validateForm();
    }

    private abstract class InputValidator extends ScTextUtils.TextValidator {
        public InputValidator(TextView textView) {
            super(textView);
        }

        abstract boolean validate(String text);

        @Override
        public void validate(TextView textView, String text) {
            if (validate(text)){
                textView.setCompoundDrawablesWithIntrinsicBounds(null, null, mValidDrawable, null);
            } else {
                textView.setCompoundDrawables(null,null, mPlaceholderDrawable,null);
            }
            validateForm();
        }
    }
}
