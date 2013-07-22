package com.soundcloud.android.screens.auth;

import com.jayway.android.robotium.solo.By;
import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;

public class FBWebViewScreen {
    public Han solo;

    public FBWebViewScreen(Han driver) {
        solo = driver;
    }

    public void typeEmail(String text) {
        solo.waitForWebElement(By.name("email"));
        solo.typeTextInWebElement(By.name("email"), text);
    }
    public void typePassword(String text) {
        solo.waitForWebElement(By.name("pass"));
        solo.typeTextInWebElement(By.name("pass"), text);
    }

    public void submit() {
        solo.waitForWebElement(By.name("login"));
        solo.clickOnWebElement(By.name("login"));
        if (solo.searchText("Do you want the browser to remember this password?", true)) {
            solo.clickOnText("Never");
        }

        solo.waitForViewId(R.id.root_menu, 5000);
    }
}
