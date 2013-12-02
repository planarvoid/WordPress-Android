package com.soundcloud.android.tests;

import android.view.View;
import android.webkit.WebView;
import android.widget.ListAdapter;
import com.jayway.android.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.R.id;
import com.soundcloud.android.main.NavigationDrawerFragment;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackState;
import com.soundcloud.android.utils.Log;


public class Waiter {
    private static final String TAG = Waiter.class.getSimpleName();
    public Han solo;
    public final int TIMEOUT = 10 * 1000;
    public final int NETWORK_TIMEOUT = 120 * 1000;

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
                final View view = solo.getView(id.empty_view_progress);
                final boolean result = (view == null || !view.isShown());

                Log.i(TAG, String.format("Spinner view found: %b", result));
                return result;
            }
        }, this.NETWORK_TIMEOUT);
    }

    public boolean waitForListContentAndRetryIfLoadingFailed() {
        View progress = solo.waitForViewId(R.id.empty_view_progress, TIMEOUT);
        if (progress != null){
            return waitForListContent();
        } else {
            retryIfFailed();
            return false;
        }
    }

    //TODO: We should have an error screen class defined
    private void retryIfFailed() {
        if(solo.searchText("Retry", 0, false)){
            solo.clickOnButtonResId(id.btn_retry);
            waitForListContent();
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
                return (PlaybackService.getPlaybackState() == PlaybackState.PLAYING);
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
