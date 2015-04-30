package com.soundcloud.android.screens.auth;

import com.robotium.solo.By;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.StreamScreen;

import android.webkit.WebView;

public class FBWebViewScreen {
    private final ViewElement webview;
    public Han solo;
    private Waiter waiter;

    public FBWebViewScreen(Han driver) {
        solo = driver;
        waiter = new Waiter(solo);
        waiter.waitForElement(WebView.class);
        webview = solo.findElement(With.className(WebView.class));
        waitForContent();
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

    public StreamScreen submit() {
        solo.waitForWebElement(loginField());
        solo.clickOnWebElement(loginField());
        if (solo.findElement(With.text("Do you want the browser to remember this password?")).isVisible()) {
            solo.findElement(With.text("Never")).click();
        }
        solo.clickOnWebElement(By.textContent("OK")); // confirm permissions
        waiter.waitForDialogToClose();
        return new StreamScreen(solo);

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
