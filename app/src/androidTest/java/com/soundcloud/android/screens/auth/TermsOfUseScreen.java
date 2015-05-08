package com.soundcloud.android.screens.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.StreamScreen;

public class TermsOfUseScreen extends Screen {

    public TermsOfUseScreen(Han solo) {
        super(solo);
    }

    public FBWebViewScreen clickContinueWithFacebookWB() {
        continueButton().click();
        waiter.waitForTextToDisappear("Logging you in");
        return new FBWebViewScreen(testDriver);
    }

    public StreamScreen clickContinueWithGP() {
        continueButton().click();
        waiter.waitForTextToDisappear("Logging you in");
        return new StreamScreen(testDriver);

    }

    public String getDisclaimer() {
        return disclaimer().getText();
    }

    public String getTitle() {
        return title().getText();
    }

    @Override
    protected Class getActivity() {
        return OnboardActivity.class;
    }

    private ViewElement continueButton() {
        return testDriver.findElement(With.id(R.id.btn_accept_terms));
    }

    private TextElement disclaimer() {
        return new TextElement(testDriver.findElement(With.id(android.R.id.message)));
    }

    private TextElement title() {
        return new TextElement(testDriver.findElement(With.id(android.R.id.title)));
    }

    @Override
    public boolean isVisible() {
        return continueButton().isVisible();
    }

    public LoginErrorScreen failToLogin() {
        continueButton().click();
        waiter.waitForTextToDisappear("Logging you in");
        return new LoginErrorScreen(testDriver);
    }
}
