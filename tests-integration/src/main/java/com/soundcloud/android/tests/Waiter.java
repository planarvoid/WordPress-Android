package com.soundcloud.android.tests;

import com.robotium.solo.Condition;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.tests.with.With;
import com.soundcloud.android.utils.Log;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Adapter;
import android.widget.ProgressBar;

import java.sql.Timestamp;
import java.util.ArrayList;


public class Waiter {
    private static final String TAG = Waiter.class.getSimpleName();
    private static Han solo;
    public final int TIMEOUT = 10 * 1000;
    public final int NETWORK_TIMEOUT = 120 * 1000;
    private final int ELEMENT_TIMEOUT = 5 * 1000;
    private int SMALL_TIMEOUT = 500;

    public Waiter(Han driver) {
        solo = driver;
    }

    public boolean waitForTextToDisappear(String text) {
        return solo.waitForCondition(new NoTextCondition(text), this.NETWORK_TIMEOUT);
    }

    public ExpectedConditions expect(ViewElement view) {
        return new ExpectedConditions(this, view);
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

    public boolean waitForKeyboardToBeShown() {
        return solo.waitForCondition(new KeyboardShownCondition(), ELEMENT_TIMEOUT);
    }

    public boolean waitForExpandedPlayer() {
        return solo.waitForCondition(new PlayerExpandedCondition(), ELEMENT_TIMEOUT);
    }

    public boolean waitForCollapsedPlayer() {
        return solo.waitForCondition(new PlayerCollapsedCondition(), ELEMENT_TIMEOUT);
    }

    public boolean waitForTextInView(ViewElement textView, String text) {
        return solo.waitForCondition(new TextInViewCondition(textView, text), ELEMENT_TIMEOUT);
    }

    @Deprecated //"Need for improvement"
    public void waitForPlayerPage() {
        solo.sleep(SMALL_TIMEOUT);
    }

    private boolean waitForListContent() {
        return solo.waitForCondition(new NoProgressBarCondition(), this.NETWORK_TIMEOUT);
    }

    public boolean waitForContentAndRetryIfLoadingFailed() {
        boolean success = waitForListContent();
        success = retryIfFailed();

        return success;
    }

    //TODO: We should have an error screen class defined
    private boolean retryIfFailed() {
        ViewElement retryButton = solo.findElement(With.id(R.id.btn_retry));
        if(retryButton.isVisible()){
            retryButton.click();
            waitForListContent();
        }
        return retryButton != null;
    }

    public boolean waitForContent(final ViewPager viewPager) {
        return solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return viewPager.getAdapter().getCount() > 0;
            }
        }, this.TIMEOUT);
    }


    public boolean waitForItemCountToIncrease(final Adapter adapter, final int currentSize) {
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

    public boolean waitForElement(final Class<? extends View> viewClass) {
        return solo.waitForCondition(new ByClassCondition(viewClass), this.TIMEOUT);
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
            View view = solo.getView(viewId);
            Log.i(TAG, "ViewID searched");
            return ( view != null && solo.getView(viewId).isShown());
        }
    }

    private class ByClassCondition implements Condition {
        private Class <? extends View> viewClass;

        ByClassCondition(Class<? extends View> viewClass) {
            this.viewClass = viewClass;
        }


        @Override
        public boolean isSatisfied() {
            Log.i(TAG, "FindViewByClass");
            ArrayList<? extends View> views = solo.getSolo().getCurrentViews(viewClass);

            return ( !views.isEmpty() && views.get(0).isShown());
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
                if  (progressBar.isShown() &&
                        progressBar.getVisibility() == View.VISIBLE &&
                        isOnScreen(progressBar) &&
                        progressBar.getClass().getSimpleName().toString().equals("ProgressBar")
                    ) {
                    Log.i(TAG, String.format("[ %s ] Spinner view found",
                           new Timestamp( new java.util.Date().getTime())));
                    progressBarNotDisplayed = progressBarNotDisplayed && false;
                }
            }
            return progressBarNotDisplayed;
        }

        private boolean isOnScreen(View progressBar) {
            boolean isOn = getLocation(progressBar)[0] >= 0 &&
                    getLocation(progressBar)[0] <= getScreenHeight() &&
                    getLocation(progressBar)[1] >= 0 &&
                    getLocation(progressBar)[1] <= getScreenWidth();

            Log.i(TAG, String.format("Onscreen: %b, Class: %s", isOn, progressBar.getClass().getSimpleName()));
            return isOn;
        }
        private int[] getLocation(View view) {
            int[] locationOnScreen = new int [2];
            view.getLocationOnScreen(locationOnScreen);
            return locationOnScreen;
        }
        private int getScreenWidth() {
            return getDisplay().getWidth();
        }

        private int getScreenHeight() {
            return getDisplay().getHeight();
        }

        private Display getDisplay() {
            return ((WindowManager) solo.getCurrentActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        }
    }

    private class PlayerExpandedCondition implements Condition {
        private final SlidingUpPanelLayout slidingPanel = (SlidingUpPanelLayout) solo.getView(R.id.sliding_layout);

        @Override
        public boolean isSatisfied() {
            return slidingPanel.isExpanded();
        }
    }

    private class PlayerCollapsedCondition implements Condition {
        private final SlidingUpPanelLayout slidingPanel = (SlidingUpPanelLayout) solo.getView(R.id.sliding_layout);

        @Override
        public boolean isSatisfied() {
            return !slidingPanel.isExpanded();
        }

    }

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
        private final MenuScreen menuScreen;
        private boolean state;

        DrawerStateCondition(boolean shouldBeOpen) {
            this.state = shouldBeOpen;
            menuScreen = new MenuScreen(solo);
        }

        @Override
        public boolean isSatisfied() {
            return menuScreen.isOpened() == state;
        }
    }

    private class KeyboardShownCondition implements Condition {
        @Override
        public boolean isSatisfied() {
            return solo.isKeyboardShown();
        }
    }

    private class TextInViewCondition implements Condition {
        private final ViewElement view;
        private final String text;

        private TextInViewCondition(ViewElement view, String text) {
            this.view = view;
            this.text = text;
        }
        @Override
        public boolean isSatisfied() {
            return view.getText().equals(text);
        }
    }
}
