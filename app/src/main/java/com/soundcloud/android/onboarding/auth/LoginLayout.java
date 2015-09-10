package com.soundcloud.android.onboarding.auth;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
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
    @NotNull private LoginHandler loginHandler; // null at creation but must be set before using

    @Bind(R.id.auto_txt_email_address) AutoCompleteTextView emailField;
    @Bind(R.id.txt_password) EditText passwordField;
    @Bind(R.id.btn_login) Button loginButton;

    public LoginLayout(Context context) {
        super(context);
    }

    public LoginLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LoginLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public @NotNull LoginHandler getLoginHandler() {
        return loginHandler;
    }

    public void setLoginHandler(@NotNull LoginHandler loginHandler) {
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

    public void setStateFromBundle(@Nullable Bundle bundle) {
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

        ButterKnife.bind(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), R.layout.onboard_email_dropdown_item, AndroidUtils.listEmails(getContext()));
        emailField.setAdapter(adapter);
        emailField.setThreshold(0);

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_i_forgot_my_password)),
                getResources().getString(R.string.authentication_I_forgot_my_password),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        getLoginHandler().onRecoverPassword(emailField.getText().toString());
                    }
                }, true, false);
    }

    @OnEditorAction(R.id.txt_password) @SuppressWarnings({"SimplifiableIfStatement"})
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

    @OnClick(R.id.btn_login)
    public void onLoginClick() {
        if (emailField.getText().length() == 0 || passwordField.getText().length() == 0) {
            AndroidUtils.showToast(getContext(), R.string.authentication_error_incomplete_fields);
        } else {

            final String email = emailField.getText().toString();
            final String password = passwordField.getText().toString();

            getLoginHandler().onLogin(email, password);
        }
    }

    @OnClick(R.id.btn_cancel)
    public void onCancelClick() {
        getLoginHandler().onCancelLogin();

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(passwordField.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(emailField.getWindowToken(), 0);
    }

    @Override
    protected AuthHandler getAuthHandler() {
        return loginHandler;
    }

    public interface LoginHandler extends AuthHandler {
        void onLogin(String email, String password);

        void onCancelLogin();

        void onRecoverPassword(String email);
    }
}
