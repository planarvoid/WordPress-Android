package com.soundcloud.android.screens.auth;

import com.robotium.solo.By;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.viewelements.ViewElement;
import com.soundcloud.android.tests.Waiter;
import com.soundcloud.android.tests.with.With;

public class FBWebViewScreen {
    private final ViewElement webview;
    public Han solo;
    private Waiter waiter;

    public FBWebViewScreen(Han driver) {
        solo = driver;
        waiter = new Waiter(solo);
        waiter.waitForDialogToClose();
        waiter.waitForElement(com.soundcloud.android.R.id.webview);
        webview = solo.findElement(With.id(com.soundcloud.android.R.id.webview));
        waiter.waitForWebViewToLoad(webview.toWebView());
    }

    public boolean waitForContent(){
        return waiter.waitForWebViewToLoad(webview.toWebView());
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
        if (solo.findElement(With.text("Do you want the browser to remember this password?")).isVisible()) {
            solo.findElement(With.text("Never")).click();
        }
        waiter.waitForDialogToClose();
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
