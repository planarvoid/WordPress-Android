package com.soundcloud.android.tests;

import android.view.View;
import android.webkit.WebView;
import android.widget.ListAdapter;
import com.jayway.android.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.fragment.NavigationDrawerFragment;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.State;

public class Waiter {
    public Han solo;
    public final int TIMEOUT = 10000;
    public final int NETWORK_TIMEOUT = 20000;

    public Waiter(Han driver) {
        solo = driver;
    }

    public boolean waitForTextToDisappear(final String text) {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return !solo.searchTextWithoutScrolling(text);
            }
        };
        return solo.waitForCondition(condition, this.NETWORK_TIMEOUT);
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

    public boolean waitForListContent() {
        View progress = solo.waitForViewId(R.id.empty_view_progress, TIMEOUT);
        if (progress != null){
            return solo.waitForCondition(new Condition() {
                @Override
                public boolean isSatisfied() {
                    final View view = solo.getView(R.id.empty_view_progress);
                    return view == null || !view.isShown();
                }
            }, this.NETWORK_TIMEOUT);
        } else {
            return false;
        }
    }

    public boolean waitForItemCountToIncrease(final ListAdapter adapter, final int currentSize) {
        return solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return adapter.getCount() > currentSize;
            }
        }, this.TIMEOUT);
    }

    public boolean waitForPlayerPlaying() {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return (CloudPlaybackService.getPlaybackState() == State.PLAYING);
            }
        };
        return solo.waitForCondition(condition, this.NETWORK_TIMEOUT);
    }

    public void waitForViewId(int id) {
        solo.waitForViewId(id, TIMEOUT);
    }

    //TODO: Is there a better way of making sure that the drawer is opened or not?
    public boolean waitForDrawerToOpen() {
        final NavigationDrawerFragment navigationDrawerFragment = solo.getCurrentNavigationDrawer();

        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return navigationDrawerFragment.isDrawerOpen();
            }
        };

        return solo.waitForCondition(condition, this.TIMEOUT);
    }
}
