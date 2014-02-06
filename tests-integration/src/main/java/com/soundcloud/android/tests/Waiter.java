package com.soundcloud.android.tests;

import com.robotium.solo.Condition;
import com.soundcloud.android.R.id;
import com.soundcloud.android.main.NavigationDrawerFragment;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.utils.Log;

import android.view.View;
import android.webkit.WebView;
import android.widget.ListAdapter;

import java.sql.Timestamp;


public class Waiter {
    private static final String TAG = Waiter.class.getSimpleName();
    public Han solo;
    public final int TIMEOUT = 10 * 1000;
    public final int NETWORK_TIMEOUT = 120 * 1000;
    private final int ELEMENT_TIMEOUT = 2 * 1000;

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

    private boolean waitForListContent() {
        return solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                final View view = solo.waitForViewId(id.empty_view_progress, ELEMENT_TIMEOUT, false);
                final boolean result = (view == null || view.getVisibility() != View.VISIBLE);
                java.util.Date date = new java.util.Date();
                Log.i(TAG, String.format("[ %s ] Spinner view found: %b", new Timestamp(date.getTime()), !result ));
                return result;
            }
        }, this.NETWORK_TIMEOUT);
    }

    public boolean waitForListContentAndRetryIfLoadingFailed() {
        waitForListContent();
        return retryIfFailed();
    }

    //TODO: We should have an error screen class defined
    private boolean retryIfFailed() {
        View retryButton = solo.waitForViewId(id.btn_retry, ELEMENT_TIMEOUT, false);
        if(retryButton != null){
            solo.clickOnButtonResId(id.btn_retry);
            waitForListContent();
        }
        return retryButton != null;
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
        final PlaybackStateProvider playbackState = new PlaybackStateProvider();
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return playbackState.isPlaying();
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

    public boolean waitForDrawerToClose() {
        final NavigationDrawerFragment navigationDrawerFragment = solo.getCurrentNavigationDrawer();

        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return !navigationDrawerFragment.isDrawerOpen();
            }
        };

        return solo.waitForCondition(condition, this.TIMEOUT);
    }

    public void waitForLogInDialog() {
        solo.waitForDialogToClose(NETWORK_TIMEOUT);
    }

    public void waitForActivity(Class activityClass) {
        solo.waitForActivity(activityClass, TIMEOUT);
    }

    public void waitForText(String text) {
        solo.waitForText(text, 1, TIMEOUT, false);
    }

    public void waitForFragmentByTag(String fragment_tag) {
        solo.waitForFragmentByTag(fragment_tag, TIMEOUT);

    }

    public boolean waitForElement(final View view) {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                Log.i(TAG, String.format("View visibility: %d", solo.getView(view.getId()).getVisibility()));
                return ( solo.getView(view.getId()).getVisibility() == View.VISIBLE);
            }
        };
        return solo.waitForCondition(condition, this.TIMEOUT);
    }

    public boolean waitForElement(final int content) {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                Log.i(TAG, String.format("ViewID visibility: %d", solo.getView(content).getVisibility()));
                return ( solo.getView(content).getVisibility() == View.VISIBLE);
            }
        };
        return solo.waitForCondition(condition, this.TIMEOUT);
    }
}
