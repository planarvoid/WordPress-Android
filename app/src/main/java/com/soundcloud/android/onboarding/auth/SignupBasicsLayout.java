package com.soundcloud.android.onboarding.auth;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import com.soundcloud.android.R;
import com.soundcloud.android.crop.util.VisibleForTesting;
import com.soundcloud.android.profile.BirthdayInfo;
import com.soundcloud.android.profile.MonthPickerDialogFragment;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SignupBasicsLayout extends FrameLayout
        implements GenderPickerDialogFragment.Callback, MonthPickerDialogFragment.Callback {

    private static final String BUNDLE_EMAIL = "BUNDLE_EMAIL";
    private static final String BUNDLE_PASSWORD = "BUNDLE_PASSWORD";
    private static final String BUNDLE_BIRTH_MONTH_NAME = "BUNDLE_BIRTH_MONTH_NAME";
    private static final String BUNDLE_BIRTH_MONTH_VALUE = "BUNDLE_BIRTH_MONTH_VALUE";
    private static final String BUNDLE_BIRTH_YEAR = "BUNDLE_BIRTH_YEAR";
    private static final String BUNDLE_GENDER = "BUNDLE_GENDER";
    private static final String BUNDLE_CUSTOM_GENDER = "BUNDLE_CUSTOM_GENDER";
    private static final int MIN_PASSWORD_LENGTH = 6;

    public static final String SELECT_MONTH_DIALOG_TAG = "select_month";
    public static final String INDICATE_GENDER_DIALOG_TAG = "indicate_gender";

    @NotNull private SignUpBasicsHandler signUpHandler; // null at creation, but must be populated before using

    @InjectView(R.id.auto_txt_email_address) AutoCompleteTextView emailField;
    @InjectView(R.id.txt_choose_a_password) EditText passwordField;
    @InjectView(R.id.btn_signup) Button signUpButton;
    @InjectView(R.id.txt_choose_month) TextView monthOptionTextView;
    @InjectView(R.id.txt_enter_year) EditText yearEditText;
    @InjectView(R.id.txt_choose_gender) TextView genderOptionTextView;
    @InjectView(R.id.after_enter_gender_vr) View customGenderDivider;
    @InjectView(R.id.txt_enter_custom_gender) EditText customGenderEditText;

    private boolean emailValid, passwordValid;
    private Drawable validDrawable, placeholderDrawable;
    @Nullable private GenderOption selectedGenderOption;

    public SignupBasicsLayout(Context context) {
        super(context);
    }

    public SignupBasicsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SignupBasicsLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @NotNull
    public SignUpBasicsHandler getSignUpHandler() {
        return signUpHandler;
    }

    public void setSignUpHandler(@NotNull SignUpBasicsHandler handler) {
        this.signUpHandler = handler;
    }

    @NotNull
    public Bundle getStateBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_EMAIL, emailField.getText().toString());
        bundle.putString(BUNDLE_PASSWORD, passwordField.getText().toString());
        bundle.putString(BUNDLE_BIRTH_MONTH_NAME, monthOptionTextView.getText().toString());
        bundle.putInt(BUNDLE_BIRTH_MONTH_VALUE, getMonth());
        bundle.putString(BUNDLE_BIRTH_YEAR, yearEditText.getText().toString());
        bundle.putSerializable(BUNDLE_GENDER, selectedGenderOption);
        bundle.putString(BUNDLE_CUSTOM_GENDER, customGenderEditText.getText().toString());
        return bundle;
    }

    public void setStateFromBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            return;
        }

        emailField.setText(bundle.getCharSequence(BUNDLE_EMAIL));
        passwordField.setText(bundle.getCharSequence(BUNDLE_PASSWORD));
        monthOptionTextView.setText(bundle.getString(BUNDLE_BIRTH_MONTH_NAME));
        monthOptionTextView.setTag(R.id.month_of_year_tag, bundle.getInt(BUNDLE_BIRTH_MONTH_VALUE, 0));
        yearEditText.setText(bundle.getCharSequence(BUNDLE_BIRTH_YEAR));
        setCurrentGender((GenderOption) bundle.getSerializable(BUNDLE_GENDER));
        customGenderEditText.setText(bundle.getCharSequence(BUNDLE_CUSTOM_GENDER));
        validateForm();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);

        setupValidDrawables();
        setupPasswordField();
        setupEmailField();

        clickifyTermsOfUse();
        clickifyPrivacy();

        validateForm();
    }

    private void clickifyPrivacy() {
        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.privacy),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        signUpHandler.onShowPrivacyPolicy();
                    }
                }, false, false);
    }

    private void clickifyTermsOfUse() {
        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_terms_of_use),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        signUpHandler.onShowTermsOfUse();
                    }
                }, false, false);
    }

    private void setupPasswordField() {
        passwordField.addTextChangedListener(new PasswordValidator());
    }

    private void setupEmailField() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), R.layout.onboard_email_dropdown_item, AndroidUtils.listEmails(getContext()));
        emailField.setAdapter(adapter);
        emailField.setThreshold(0);
        emailField.addTextChangedListener(new EmailValidator());
    }

    private void setupValidDrawables() {
        validDrawable = getResources().getDrawable(R.drawable.ic_done_dark_sm);
        placeholderDrawable = new ColorDrawable(Color.TRANSPARENT);
        placeholderDrawable.setBounds(0, 0, validDrawable.getIntrinsicWidth(), validDrawable.getIntrinsicHeight());
    }

    @OnEditorAction(R.id.txt_choose_a_password)
    @SuppressWarnings({"SimplifiableIfStatement"})
    boolean onPasswordEdit(int actionId, KeyEvent event) {
        boolean done = actionId == EditorInfo.IME_ACTION_DONE;
        boolean pressedEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
        boolean downAction = event != null && event.getAction() == KeyEvent.ACTION_DOWN;

        if (done || pressedEnter && downAction) {
            return signUpButton.performClick();
        } else {
            return false;
        }
    }

    @OnClick(R.id.txt_choose_month)
    public void onMonthClick() {
        final FragmentActivity activity = signUpHandler.getFragmentActivity();
        DialogFragment fragment = MonthPickerDialogFragment.build(getMonth());
        fragment.show(activity.getSupportFragmentManager(), SELECT_MONTH_DIALOG_TAG);
    }

    @Override
    public void onMonthSelected(String monthName, int monthOfYear) {
        monthOptionTextView.setTag(R.id.month_of_year_tag, monthOfYear);
        monthOptionTextView.setText(monthName);
        validateForm();
    }

    @OnTextChanged(R.id.txt_enter_year)
    public void yearTextListener() {
        validateForm();
    }

    @OnClick(R.id.btn_cancel)
    public void onCancelClick() {
        getSignUpHandler().onCancelSignUp();
        hideKeyboardOnSignup(emailField, passwordField);
    }

    @OnClick(R.id.btn_signup)
    public void onSignupClick() {
        if (emailField.getText().length() == 0 || passwordField.getText().length() == 0) {
            AndroidUtils.showToast(getContext(), R.string.authentication_error_incomplete_fields);
        } else if (!ScTextUtils.isEmail(emailField.getText())) {
            AndroidUtils.showToast(getContext(), R.string.authentication_error_invalid_email);
        } else if (!checkPassword(passwordField.getText())) {
            AndroidUtils.showToast(getContext(), R.string.authentication_error_password_too_short);
        } else {
            final String email = getEmail();
            final String password = getPassword();
            final BirthdayInfo birthday = BirthdayInfo.buildFrom(getMonth(), getYear());
            final @Nullable String gender = (selectedGenderOption != null) ? selectedGenderOption.getApiValue(getCustomGender()) : null;

            hideKeyboardOnSignup(emailField, passwordField);

            signUpHandler.onSignUp(email, password, birthday, gender);
        }
    }

    private String getEmail() {
        return emailField.getText().toString();
    }

    private String getPassword() {
        return passwordField.getText().toString();
    }

    private int getMonth() {
        Integer month = (Integer) monthOptionTextView.getTag(R.id.month_of_year_tag);
        return (month != null) ? month : 0;
    }

    private int getYear() {
        return (int) ScTextUtils.safeParseLong(yearEditText.getText().toString());
    }

    private String getCustomGender() {
        return customGenderEditText.getText().toString();
    }

    @OnClick(R.id.txt_choose_gender)
    public void onGenderClick() {
        final FragmentActivity activity = signUpHandler.getFragmentActivity();
        DialogFragment fragment = GenderPickerDialogFragment.build(selectedGenderOption);
        fragment.show(activity.getSupportFragmentManager(), INDICATE_GENDER_DIALOG_TAG);
    }

    @Override
    public void onGenderSelected(GenderOption gender) {
        setCurrentGender(gender);
    }

    private void setCurrentGender(GenderOption gender) {
        selectedGenderOption = gender;
        updateGenderLabel();
        updateCustomGenderVisibility();
    }

    private void updateCustomGenderVisibility() {
        if (selectedGenderOption == GenderOption.CUSTOM) {
            customGenderDivider.setVisibility(View.VISIBLE);
            customGenderEditText.setVisibility(View.VISIBLE);
        } else {
            customGenderDivider.setVisibility(View.GONE);
            customGenderEditText.setVisibility(View.GONE);
        }
    }

    private void updateGenderLabel() {
        String label = (selectedGenderOption != null) ? getResources().getString(selectedGenderOption.getResId()) : null;
        genderOptionTextView.setText(label);
    }

    private void hideKeyboardOnSignup(AutoCompleteTextView emailField, EditText passwordField) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(emailField.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(passwordField.getWindowToken(), 0);
    }

    private void validateForm() {
        boolean isValid = emailValid;
        isValid = isValid && passwordValid;
        isValid = isValid && (BirthdayInfo.buildFrom(getMonth(), getYear()) != null);
        signUpButton.setEnabled(isValid);
    }

    @VisibleForTesting
    static boolean checkPassword(CharSequence password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    public interface SignUpBasicsHandler {
        void onSignUp(String email, String password, BirthdayInfo birthday, @Nullable String gender);

        void onCancelSignUp();

        void onShowTermsOfUse();

        void onShowPrivacyPolicy();

        FragmentActivity getFragmentActivity();
    }

    private abstract class InputValidator extends ScTextUtils.TextValidator {
        public InputValidator(TextView textView) {
            super(textView);
        }

        @Override
        public void validate(TextView textView, String text) {
            if (validate(text)) {
                textView.setCompoundDrawablesWithIntrinsicBounds(null, null, validDrawable, null);
            } else {
                textView.setCompoundDrawables(null, null, placeholderDrawable, null);
            }
            validateForm();
        }

        abstract boolean validate(String text);
    }

    private class EmailValidator extends InputValidator {
        public EmailValidator() {
            super(SignupBasicsLayout.this.emailField);
        }

        @Override
        boolean validate(String text) {
            emailValid = ScTextUtils.isEmail(text);
            return emailValid;
        }
    }

    private class PasswordValidator extends InputValidator {
        public PasswordValidator() {
            super(SignupBasicsLayout.this.passwordField);
        }

        @Override
        boolean validate(String text) {
            passwordValid = checkPassword(text);
            return passwordValid;
        }
    }
}
