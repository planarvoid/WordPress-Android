package com.soundcloud.android.tests;

import com.jayway.android.robotium.solo.Condition;

import android.webkit.WebView;

public class Waiter {
    public Han solo;

    public Waiter(Han driver) {
        solo = driver;
    }

    public boolean waitForTextToDisappear(final String text) {
        Condition condition = new Condition() {

            @Override
            public boolean isSatisfied() {
                return !solo._searchText(text, true);
            }
        };
        return solo.waitForCondition(condition, 5000);
    }

    public boolean waitForWebViewToLoad(final WebView webViewToCheck) {
        Condition condition = new Condition() {

            @Override
            public boolean isSatisfied() {
                return (webViewToCheck.getUrl() != null);
            }
        };
        return solo.waitForCondition(condition, 5000);
    }
}
