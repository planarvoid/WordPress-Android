package com.soundcloud.android.tests;

import com.jayway.android.robotium.solo.Condition;
import com.soundcloud.android.R;

import android.view.View;
import android.webkit.WebView;

public class Waiter {
    public Han solo;
    public final int TIMEOUT = 5000;
    public final int NETWORK_TIMEOUT = 15000;

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
        return solo.waitForCondition(condition, this.TIMEOUT);
    }

    public boolean waitForWebViewToLoad(final WebView webViewToCheck) {
        Condition condition = new Condition() {

            @Override
            public boolean isSatisfied() {
                return (webViewToCheck.getUrl() != null);
            }
        };
        return solo.waitForCondition(condition,this.NETWORK_TIMEOUT );
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
            }, this.NETWORK_TIMEOUT);
        } else {
            return false;
        }
    }
}
