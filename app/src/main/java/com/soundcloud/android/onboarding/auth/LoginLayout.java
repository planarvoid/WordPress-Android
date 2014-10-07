package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
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

public class LoginLayout extends AuthLayout {
    private static final String BUNDLE_EMAIL = "BUNDLE_EMAIL";
    private static final String BUNDLE_PASSWORD = "BUNDLE_PASSWORD";
    @Nullable private LoginHandler loginHandler;

    public LoginLayout(Context context) {
        super(context);
    }

    public LoginLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LoginLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public LoginHandler getLoginHandler() {
        return loginHandler;
    }

    public void setLoginHandler(LoginHandler loginHandler) {
        this.loginHandler = loginHandler;
    }

    public Bundle getStateBundle() {
        EditText emailField = (EditText) findViewById(R.id.auto_txt_email_address);
        EditText passwordField = (EditText) findViewById(R.id.txt_password);

        Bundle bundle = new Bundle();
        bundle.putCharSequence(BUNDLE_EMAIL, emailField.getText());
        bundle.putCharSequence(BUNDLE_PASSWORD, passwordField.getText());
        return bundle;
    }

    public void setState(@Nullable Bundle bundle) {
        if (bundle == null) {
            return;
        }

        EditText emailField = (EditText) findViewById(R.id.auto_txt_email_address);
        EditText passwordField = (EditText) findViewById(R.id.txt_password);

        emailField.setText(bundle.getCharSequence(BUNDLE_EMAIL));
        passwordField.setText(bundle.getCharSequence(BUNDLE_PASSWORD));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();

        final AutoCompleteTextView emailField = (AutoCompleteTextView) findViewById(R.id.auto_txt_email_address);
        final EditText passwordField = (EditText) findViewById(R.id.txt_password);
        final Button loginButton = (Button) findViewById(R.id.btn_login);
        final Button cancelButton = (Button) findViewById(R.id.btn_cancel);

        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @SuppressWarnings({"SimplifiableIfStatement"})
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean done = actionId == EditorInfo.IME_ACTION_DONE;
                boolean pressedEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean downAction = event != null && event.getAction() == KeyEvent.ACTION_DOWN;

                if (done || pressedEnter && downAction) {
                    return loginButton.performClick();
                } else {
                    return false;
                }
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(), R.layout.onboard_email_dropdown_item, AndroidUtils.listEmails(getContext()));
        emailField.setAdapter(adapter);
        emailField.setThreshold(0);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailField.getText().length() == 0 || passwordField.getText().length() == 0) {
                    AndroidUtils.showToast(context, R.string.authentication_error_incomplete_fields);
                } else {

                    final String email = emailField.getText().toString();
                    final String password = passwordField.getText().toString();

                    if (getLoginHandler() != null) {
                        getLoginHandler().onLogin(email, password);
                    }
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getLoginHandler() != null) {
                    getLoginHandler().onCancelLogin();
                }

                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(passwordField.getWindowToken(), 0);
                imm.hideSoftInputFromWindow(emailField.getWindowToken(), 0);
            }
        });

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_i_forgot_my_password)),
                getResources().getString(R.string.authentication_I_forgot_my_password),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (getLoginHandler() != null) {
                            getLoginHandler().onRecover(emailField.getText().toString());
                        }
                    }
                }, true, false);
    }

    @Override
    AuthHandler getAuthHandler() {
        return loginHandler;
    }

    public interface LoginHandler extends AuthHandler {
        void onLogin(String email, String password);

        void onCancelLogin();

        void onRecover(String email);
    }
}
