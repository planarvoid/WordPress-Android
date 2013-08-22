package com.soundcloud.android.screens.auth;

import static junit.framework.Assert.assertNotNull;

import com.jayway.android.robotium.solo.By;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.FacebookWebFlow;
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
        final FacebookWebFlow facebookWebFlow = solo.assertActivity(FacebookWebFlow.class);
        WebView webView = facebookWebFlow.getWebView();
        assertNotNull(webView);
        return waiter.waitForWebViewToLoad(webView);
    }

    public void typeEmail(String text) {
        solo.waitForWebElement(emailField());
        solo.clearTextInWebElement(emailField());
        solo.typeTextInWebElement(emailField(), text);
    }

    public void typePassword(String text) {
        solo.waitForWebElement(passwordField());
        solo.clearTextInWebElement(passwordField());
        solo.typeTextInWebElement(passwordField(), text);
    }

    public void submit() {
        solo.waitForWebElement(loginField());
        solo.clickOnWebElement(loginField());
        if (solo.searchText("Do you want the browser to remember this password?", true)) {
            solo.clickOnText("Never");
        }

        solo.waitForViewId(R.id.root_menu, 15000);
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
