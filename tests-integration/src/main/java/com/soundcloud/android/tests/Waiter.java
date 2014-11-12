package com.soundcloud.android.tests;

import com.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.tests.with.With;
import com.soundcloud.android.utils.Log;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Adapter;
import android.widget.ProgressBar;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class Waiter {
    private static final String TAG = Waiter.class.getSimpleName();
    private static Han solo;
    private static final int TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);
    private static final int NETWORK_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(2);
    private static final int FIVE_SECONDS = (int) TimeUnit.SECONDS.toMillis(5);
    private static final int ELEMENT_TIMEOUT = FIVE_SECONDS;
    private static final int SMALL_TIMEOUT = 500;

    public Waiter(Han driver) {
        solo = driver;
    }

    public boolean waitForTextToDisappear(String text) {
        return solo.waitForCondition(new NoTextCondition(text), NETWORK_TIMEOUT);
    }

    public boolean waitForElementToBeInvisible(With matcher) {
        return solo.waitForCondition(new InVisibleElementCondition(matcher), ELEMENT_TIMEOUT);
    }

    public boolean waitForElementToBeVisible(With matcher) {
        return waitForElementToBeVisible(matcher, ELEMENT_TIMEOUT);
    }

    public boolean waitForElementToBeVisible(With matcher, int timeoutMs) {
        return solo.waitForCondition(new VisibleElementCondition(matcher), timeoutMs);
    }

    public ExpectedConditions expect(ElementWithText view) {
        return new ExpectedConditions(this, view);
    }

    public ToastConditions expectToast() {
        return new ToastConditions(this, solo);
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

    public boolean waitForTextInView(ElementWithText textView, String text) {
        return solo.waitForCondition(new TextInViewCondition(textView, text), ELEMENT_TIMEOUT);
    }

    @Deprecated //"Need for improvement"
    public void waitForPlayerPage() {
        solo.sleep(SMALL_TIMEOUT);
    }

    public void waitFiveSeconds() {
        solo.sleep(FIVE_SECONDS);
    }

    public boolean waitForElementCondition(Condition condition) {
        return solo.waitForCondition(condition, NETWORK_TIMEOUT);
    }

    public boolean waitForElementToBeChecked(With matcher) {
        return solo.waitForCondition(new CheckedViewCondition(matcher), ELEMENT_TIMEOUT);
    }

    private boolean waitForListContent() {
        return solo.waitForCondition(new NoProgressBarCondition(), NETWORK_TIMEOUT);
    }

    public boolean waitForContentAndRetryIfLoadingFailed() {
        boolean success = waitForListContent();
        if (!success) {
            if (retryIfFailed()) {
                return waitForListContent();
            }
        }
        return success;
    }

    //TODO: We should have an error screen class defined
    private boolean retryIfFailed() {
        List<ViewElement> retryButtons = solo.findElements(With.id(R.id.btn_retry));
        if (!retryButtons.isEmpty())   {
            ViewElement button = retryButtons.get(0);
            if (button.isVisible()) {
                button.click();
                return true;
            }
        }
        return false;
    }

    public boolean waitForContent(final ViewPager viewPager) {
        return solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return viewPager.getAdapter().getCount() > 0;
            }
        }, TIMEOUT);
    }


    public boolean waitForItemCountToIncrease(final Adapter adapter, final int currentSize) {
        return solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return adapter.getCount() > currentSize;
            }
        }, TIMEOUT);
    }

    public boolean waitForPlaybackToBePlaying() {
        return solo.waitForCondition(new PlaybackPlayingCondition(), NETWORK_TIMEOUT);
    }

    public boolean waitForPlaybackToBeIdle() {
        return solo.waitForCondition(new PlaybackIdleCondition(), NETWORK_TIMEOUT);
    }

    public boolean waitForDrawerToClose() {
        return solo.waitForCondition(new DrawerStateCondition(false), TIMEOUT);
    }

    public boolean waitForDrawerToOpen() {
        return solo.waitForCondition(new DrawerStateCondition(true), TIMEOUT);
    }

    public void waitForDialogToClose() {
        solo.waitForDialogToClose(NETWORK_TIMEOUT);
    }

    public void waitForActivity(Class<? extends Activity> activityClass) {
        solo.waitForCondition(new CurrentActivityCondition(activityClass), TIMEOUT);
    }

    public void waitForTextInView(ViewElement viewElement) {
        solo.waitForCondition(new HasTextInViewCondition(viewElement), TIMEOUT);
    }

    public boolean waitForFragmentByTag(String fragmentTag) {
        return solo.waitForFragmentByTag(fragmentTag, TIMEOUT);
    }

    public boolean waitForElement(final int content) {
        return solo.waitForCondition(new VisibleElementCondition(content), ELEMENT_TIMEOUT);
    }

    public boolean waitForAdToBeComeSkippable(final With matcher) {
        return solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return solo.findElement(matcher).isVisible();
            }
        }, 15000);
    }

    public boolean waitForElement(final Class<? extends View> viewClass) {
        return solo.waitForCondition(new ByClassCondition(viewClass), TIMEOUT);
    }

    public boolean waitForElements(int elementId) {
        return solo.waitForCondition(new VisibleElementsCondition(elementId), NETWORK_TIMEOUT);
    }

    private class VisibleElementCondition implements Condition {
        private With matcher;

        VisibleElementCondition(With matcher) {
            this.matcher = matcher;
        }

        VisibleElementCondition(int id) {
            this.matcher = With.id(id);
        }

        @Override
        public boolean isSatisfied() {
            ViewElement view = solo.findElement(matcher);
            Log.i(TAG, "ViewID searched");
            return (view.isVisible());
        }
    }

    private class InVisibleElementCondition implements Condition {
        final private VisibleElementCondition visibleElementCondition;

        InVisibleElementCondition(With matcher) {
            visibleElementCondition = new VisibleElementCondition(matcher);
        }

        @Override
        public boolean isSatisfied() {
            return !visibleElementCondition.isSatisfied();
        }
    }

    private class VisibleElementsCondition implements Condition {
        private int viewId;

        VisibleElementsCondition(int id) {
            viewId = id;
        }

        @Override
        public boolean isSatisfied() {
            List<ViewElement> elements = solo.findElements(With.id(viewId));
            Log.i(TAG, "ViewID searched");
            return (!elements.isEmpty());
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
            solo.findElement(With.id(R.id.empty_view_progress));
        }

        @Override
        public boolean isSatisfied() {
            boolean progressBarNotDisplayed = true;
            progressBars = solo.getSolo().getCurrentViews(PROGRESS_CLASS);

            for(View progressBar : progressBars ){
                if  (progressBar.isShown() &&
                        progressBar.getVisibility() == View.VISIBLE &&
                        isOnScreen(progressBar) &&
                        progressBar.getClass().getSimpleName().equals("ProgressBar")
                    ) {
                    Log.i(TAG, String.format("[ %s ] Spinner view found",
                           new Timestamp( new java.util.Date().getTime())));
                    progressBarNotDisplayed = false;
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

    private class NoTextCondition implements Condition {
        private String searchedText;

        private NoTextCondition (String text) {
            searchedText = text;
        }

        @Override
        public boolean isSatisfied() {
            return !solo.findElement(With.text(searchedText)).isVisible();
        }
    }

    private class PlaybackPlayingCondition implements Condition {
        private final PlaybackStateProvider playbackState = new PlaybackStateProvider();

        @Override
        public boolean isSatisfied() {
            return playbackState.isPlaying();
        }
    }

    private class PlaybackIdleCondition implements Condition {
        private final PlaybackStateProvider playbackState = new PlaybackStateProvider();

        @Override
        public boolean isSatisfied() {
            return !playbackState.isPlaying();
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
        private final ElementWithText view;
        private final String text;

        private TextInViewCondition(ElementWithText view, String text) {
            this.view = view;
            this.text = text;
        }
        @Override
        public boolean isSatisfied() {
            return view.getText().equals(text);
        }
    }

    private class HasTextInViewCondition implements Condition {
        private final ViewElement viewElement;

        public HasTextInViewCondition(ViewElement viewElement) {
            this.viewElement = viewElement;
        }

        @Override
        public boolean isSatisfied() {
            return !TextUtils.isEmpty(viewElement.getText());
        }
    }

    private class CurrentActivityCondition implements Condition {

        private final Class<? extends Activity> activity;

        public CurrentActivityCondition(Class<? extends Activity> activityClass) {
            activity = activityClass;
        }

        @Override
        public boolean isSatisfied() {
            Log.i(TAG, String.format("Waiting for Activity: %s, current Activity: %s", activity.getSimpleName(), solo.getCurrentActivity().toString()));
            return solo.getCurrentActivity().getClass().getSimpleName().equals(activity.getSimpleName());
        }
    }

    private class CheckedViewCondition implements Condition {
        private final With matcher;

        public CheckedViewCondition(With matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean isSatisfied() {
            return solo.findElement(matcher).isChecked();
        }
    }
}
