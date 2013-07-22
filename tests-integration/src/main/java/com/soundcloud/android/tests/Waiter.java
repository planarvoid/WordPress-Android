package com.soundcloud.android.tests;

import com.jayway.android.robotium.solo.Condition;
import com.soundcloud.android.R;

import android.view.View;
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
                return !solo.searchText(text, true);
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

    public boolean waitForListContent(){
        View progress = solo.waitForViewId(com.soundcloud.android.R.id.list_loading, 3000);
        if (progress != null){
            return solo.waitForCondition(new Condition() {
                @Override
                public boolean isSatisfied() {
                    final View view = solo.getView(R.id.list_loading);
                    return view == null || !view.isShown();
                }
            }, 15000);
        } else {
            return false;
        }
    }
}
