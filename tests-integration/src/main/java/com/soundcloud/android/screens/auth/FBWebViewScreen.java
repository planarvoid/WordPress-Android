package com.soundcloud.android.screens.auth;

import static junit.framework.Assert.assertNotNull;

import com.robotium.solo.By;
import com.soundcloud.android.onboarding.auth.FacebookWebFlowActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

import android.webkit.WebView;

public class FBWebViewScreen {
    public Han solo;
    private Waiter waiter;

    public FBWebViewScreen(Han driver) {
        solo = driver;
        waiter = new Waiter(solo);
    }

    public boolean waitForContent(){
        final FacebookWebFlowActivity facebookWebFlow = solo.assertActivity(FacebookWebFlowActivity.class);
        WebView webView = facebookWebFlow.getWebView();
        assertNotNull(webView);
        return waiter.waitForWebViewToLoad(webView);
    }

    public void typeEmail(String text) {
        solo.waitForWebElement(emailField());
        solo.clearTextInWebElement(emailField());
        solo.typeTextInWebElement(emailField(), text);
        solo.clickOnWebElement(emailField());
    }

    public void typePassword(String text) {
        solo.waitForWebElement(passwordField());
        solo.clearTextInWebElement(passwordField());
        solo.typeTextInWebElement(passwordField(), text);
    }

    public MainScreen submit() {
        solo.waitForWebElement(loginField());
        solo.clickOnWebElement(loginField());
        if (solo.searchText("Do you want the browser to remember this password?", true)) {
            solo.clickOnText("Never");
        }
        waiter.waitForLogInDialog();
        return new MainScreen(solo);

    }

    private By emailField() {
        return By.name("email");
    }

    private By passwordField() {
        return By.name("pass");
    }

    private By loginField() {
        return By.name("login");
    }
}
