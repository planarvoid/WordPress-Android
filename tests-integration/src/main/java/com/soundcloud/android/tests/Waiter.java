package com.soundcloud.android.tests;

import com.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.main.NavigationDrawerFragment;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.utils.Log;

import android.view.View;
import android.webkit.WebView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;

import java.sql.Timestamp;
import java.util.ArrayList;


public class Waiter {
    private static final String TAG = Waiter.class.getSimpleName();
    public Han solo;
    public final int TIMEOUT = 10 * 1000;
    public final int NETWORK_TIMEOUT = 120 * 1000;
    private final int ELEMENT_TIMEOUT = 5 * 1000;

    public Waiter(Han driver) {
        solo = driver;
    }

    public boolean waitForTextToDisappear(String text) {
        return solo.waitForCondition(new NoTextCondition(text), this.NETWORK_TIMEOUT);
    }

    public boolean waitForWebViewToLoad(final WebView webViewToCheck) {
        solo.getCurrentActivity().runOnUiThread(new Runnable() {
            Condition condition = new Condition() {

                @Override
                public boolean isSatisfied() {
                    return (webViewToCheck.getUrl() != null);
                }
            };

            @Override
            public void run() {
                solo.waitForCondition(condition, NETWORK_TIMEOUT );
            }
        });

        return true;
    }

    private boolean waitForListContent() {
        return solo.waitForCondition(new NoProgressBarCondition(), this.NETWORK_TIMEOUT);
    }

    public boolean waitForContentAndRetryIfLoadingFailed() {
        boolean success = waitForListContent();
        if(!success) {
            success = retryIfFailed();
        }
        return success;
    }

    //TODO: We should have an error screen class defined
    private boolean retryIfFailed() {
        View retryButton = solo.waitForViewId(R.id.btn_retry, ELEMENT_TIMEOUT, false);
        if(retryButton != null){
            solo.clickOnButton(R.id.btn_retry);
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
        return solo.waitForCondition(new PlayerPlayingCondition(), this.NETWORK_TIMEOUT);
    }

    public void waitForViewId(int id) {
        solo.waitForViewId(id, TIMEOUT);
    }

    public boolean waitForDrawerToClose() {
        return solo.waitForCondition(new DrawerStateCondition(false), this.TIMEOUT);
    }

    public boolean waitForDrawerToOpen() {
        return solo.waitForCondition(new DrawerStateCondition(true), this.TIMEOUT);
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

    public boolean waitForFragmentByTag(String fragment_tag) {
        return solo.waitForFragmentByTag(fragment_tag, TIMEOUT);
    }

    public boolean waitForElement(final View view) {
        return solo.waitForCondition(new VisibleElementCondition(view), this.TIMEOUT);
    }

    public boolean waitForElement(final int content) {
        return solo.waitForCondition(new VisibleElementCondition(content), this.TIMEOUT);
    }

    private class VisibleElementCondition implements Condition {
        private int viewId;

        VisibleElementCondition(int id) {
            viewId = id;
        }

        VisibleElementCondition(View view) {
            viewId = view.getId();
        }
        @Override
        public boolean isSatisfied() {
            Log.i(TAG, String.format("ViewID visibility: %d", solo.getView(viewId).getVisibility()));
            return ( solo.getView(viewId).isShown());
        }
    }


    private class NoProgressBarCondition implements Condition {
        private final Class<ProgressBar> PROGRESS_CLASS = ProgressBar.class;
        private ArrayList<ProgressBar> progressBars;

        private NoProgressBarCondition() {
            solo.waitForViewId(R.id.empty_view_progress, ELEMENT_TIMEOUT, false);
        }

        @Override
        public boolean isSatisfied() {
            boolean progressBarNotDisplayed = true;
            progressBars = solo.getSolo().getCurrentViews(PROGRESS_CLASS);

            for(View progressBar : progressBars ){
                if(progressBar.isShown() && progressBar.getVisibility() == View.VISIBLE) {
                    Log.i(TAG, String.format("[ %s ] Spinner view found",
                           new Timestamp( new java.util.Date().getTime())));
                    progressBarNotDisplayed = progressBarNotDisplayed && false;
                }
            }
            return progressBarNotDisplayed;
        }
    };

    private class NoTextCondition implements Condition {
        private String searchedText;

        private NoTextCondition (String text) {
            searchedText = text;
        }

        @Override
        public boolean isSatisfied() {
            return !solo.searchTextWithoutScrolling(searchedText);
        }
    }

    private class PlayerPlayingCondition implements Condition {
        private final PlaybackStateProvider playbackState = new PlaybackStateProvider();

        @Override
        public boolean isSatisfied() {
            return playbackState.isPlaying();
        }
    }

    private class DrawerStateCondition implements Condition {
        private final NavigationDrawerFragment navigationDrawerFragment = solo.getCurrentNavigationDrawer();
        private boolean state;

        DrawerStateCondition(boolean shouldBeOpen) {
            this.state = shouldBeOpen;
        }

        @Override
        public boolean isSatisfied() {
            return state && navigationDrawerFragment.isDrawerOpen();
        }
    }
}
