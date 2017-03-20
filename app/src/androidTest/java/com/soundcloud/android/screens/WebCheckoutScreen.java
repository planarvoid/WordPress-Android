package com.soundcloud.android.screens;

import com.robotium.solo.By;
import com.robotium.solo.Solo;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.payments.WebCheckoutActivity;
import com.soundcloud.android.screens.go.GoOnboardingScreen;

import java.util.concurrent.TimeUnit;

public class WebCheckoutScreen extends Screen {

    private static final Class ACTIVITY = WebCheckoutActivity.class;
    private static final int WEBVIEW_WAIT = (int) TimeUnit.SECONDS.toMillis(2);

    private final Solo solo;

    WebCheckoutScreen(Han solo) {
        super(solo);

        // We're using Robotium directly to instrument the checkout form WebView content
        this.solo = solo.getSolo();
    }

    public WebCheckoutScreen populateTestCardData() {
        waitForWebViewToLoad();
        enterTestCardDetails();
        return this;
    }

    public GoOnboardingScreen subscribe() {
        clickBuyButton();
        return new GoOnboardingScreen(testDriver);
    }

    private void waitForWebViewToLoad() {
        solo.waitForView(solo.getView(R.id.payment_form), WEBVIEW_WAIT, false);
    }

    private void enterTestCardDetails() {
        solo.enterTextInWebElement(By.name("ccname"), "Test AVS Visa");
        solo.enterTextInWebElement(By.name("cardnumber"), "4400000000000008");
        solo.enterTextInWebElement(By.name("ccmonth"), "08");
        solo.enterTextInWebElement(By.name("ccyear"), "18");
        solo.enterTextInWebElement(By.name("cvc"), "737");
        solo.enterTextInWebElement(By.name("zip"), "12345");
    }

    private void clickBuyButton() {
        solo.clickOnWebElement(By.id("frmSubmitButton"));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
