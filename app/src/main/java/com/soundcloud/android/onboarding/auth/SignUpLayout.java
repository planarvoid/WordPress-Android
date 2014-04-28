package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
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

public class SignUpLayout extends AuthLayout {
    private static final String BUNDLE_EMAIL    = "BUNDLE_EMAIL";
    private static final String BUNDLE_PASSWORD = "BUNDLE_PASSWORD";
    private Button signUpButton;

    private boolean emailValid, passwordValid;
    private Drawable validDrawable, placeholderDrawable;

    public interface SignUpHandler extends AuthHandler {
        void onSignUp(String email, String password);
        void onCancelSignUp();
        void onShowTermsOfUse();
        void onShowPrivacyPolicy();
    }
    public SignUpLayout(Context context) {
        super(context);
    }

    public SignUpLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SignUpLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private static final int MIN_PASSWORD_LENGTH = 6;

    @Nullable private SignUpHandler signUpHandler;

    @Override
    AuthHandler getAuthHandler() {
        return signUpHandler;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();

        final AutoCompleteTextView emailField = (AutoCompleteTextView)  findViewById(R.id.auto_txt_email_address);
        final EditText passwordField = (EditText) findViewById(R.id.txt_choose_a_password);
        final Button   cancelButton       = (Button)   findViewById(R.id.btn_cancel);
        signUpButton = (Button)   findViewById(R.id.btn_signup);

        validDrawable = getResources().getDrawable(R.drawable.ic_done_dark_sm);
        placeholderDrawable = new ColorDrawable(Color.TRANSPARENT);
        placeholderDrawable.setBounds(0, 0, validDrawable.getIntrinsicWidth(), validDrawable.getIntrinsicHeight());


        emailField.addTextChangedListener(new InputValidator(emailField) {
            @Override
            boolean validate(String text) {
                emailValid = ScTextUtils.isEmail(text);
                return emailValid;
            }
        });

        passwordField.addTextChangedListener(new InputValidator(passwordField) {
            @Override
            boolean validate(String text) {
                passwordValid = checkPassword(text);
                return passwordValid;
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(), R.layout.onboard_email_dropdown_item, AndroidUtils.listEmails(getContext()));
        emailField.setAdapter(adapter);
        emailField.setThreshold(0);

        findViewById(R.id.google_plus_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpHandler.onGooglePlusAuth();
            }
        });
        findViewById(R.id.facebook_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpHandler.onFacebookAuth();
            }
        });

        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

                    if (signUpHandler != null) {
                        signUpHandler.onSignUp(email, password);
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
                        if (signUpHandler != null) {
                            signUpHandler.onShowTermsOfUse();
                        }
                    }
                }, false, false);

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.privacy),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (signUpHandler != null) {
                            signUpHandler.onShowPrivacyPolicy();
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
        signUpButton.setEnabled(emailValid && passwordValid);
    }

    static boolean checkPassword(CharSequence password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    @Nullable
    public SignUpHandler getSignUpHandler() {
        return signUpHandler;
    }

    public void setSignUpHandler(@Nullable SignUpHandler mSignUpHandler) {
        this.signUpHandler = mSignUpHandler;
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
                textView.setCompoundDrawablesWithIntrinsicBounds(null, null, validDrawable, null);
            } else {
                textView.setCompoundDrawables(null,null, placeholderDrawable,null);
            }
            validateForm();
        }
    }
}
