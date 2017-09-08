package com.soundcloud.android.screens.auth;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EditTextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.UpgradeScreen;

import android.support.test.espresso.action.ViewActions;

public class LoginScreen extends Screen {
    private static final Class ACTIVITY = OnboardActivity.class;

    public LoginScreen(Han testDriver) {
        super(testDriver);
    }

    private ViewElement googleSignInButton() {
        return testDriver.findOnScreenElement(With.id(R.id.google_plus_btn));
    }

    private ViewElement facebookSignInButton() {
        return testDriver.findOnScreenElement(With.id(R.id.facebook_btn));
    }

    private EditTextElement emailInputField() {
        return new EditTextElement(testDriver.findOnScreenElement(With.id(R.id.auto_txt_email_address)));
    }

    private EditTextElement passwordInputField() {
        return new EditTextElement(testDriver.findOnScreenElement(With.id(R.id.txt_password)));
    }

    private ViewElement loginButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_login));
    }

    private ViewElement forgotPasswordButton() {
        return testDriver.findOnScreenElement(With.id(R.id.txt_i_forgot_my_password));
    }

    public TermsOfUseScreen clickOnFBSignInButton() {
        facebookSignInButton().click();
        return new TermsOfUseScreen(testDriver);
    }

    public LoginScreen clickSignInWithGoogleButton() {
        googleSignInButton().click();
        return this;
    }

    public TermsOfUseScreen assertTermsOfUseScreen() {
        return new TermsOfUseScreen(testDriver);
    }

    public RecoverPasswordScreen clickForgotPassword() {
        forgotPasswordButton().click();
        return new RecoverPasswordScreen(testDriver);
    }

    public TermsOfUseScreen selectUserFromDialog(String username) {
        testDriver.findOnScreenElement(With.text(username)).click();
        waiter.waitForActivity(OnboardActivity.class);
        return new TermsOfUseScreen(testDriver);
    }

    public StreamScreen loginDefault(String username, String password) {
        tryToLogin(username, password);
        return new StreamScreen(testDriver);
    }

    public UpgradeScreen loginFromUpgradeDeepLink(String username, String password) {
        tryToLogin(username, password);
        return new UpgradeScreen(testDriver);
    }

    public LoginErrorScreen failToLoginAs(String username, String password) {
        tryToLogin(username, password);
        return new LoginErrorScreen(testDriver);
    }

    private void tryToLogin(String username, String password) {
        onView(withId(R.id.txt_password)).perform(ViewActions.clearText()).perform(ViewActions.typeText(password));
        onView(withId(R.id.auto_txt_email_address)).perform(ViewActions.clearText()).perform(ViewActions.typeText(username));
        onView(allOf(withId(R.id.btn_login), withText(R.string.btn_done))).perform(click());
    }

    @Override
    public boolean isVisible() {
        return loginButton().isOnScreen();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
