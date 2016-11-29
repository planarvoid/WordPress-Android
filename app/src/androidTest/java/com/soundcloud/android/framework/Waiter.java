package com.soundcloud.android.framework;

import static org.hamcrest.MatcherAssert.assertThat;

import com.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.observers.ToastObserver;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.playback.PlaybackStateProvider;
import com.soundcloud.android.utils.Log;

import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.webkit.WebView;
import android.widget.Adapter;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Waiter {
    private static final String TAG = Waiter.class.getSimpleName();
    private static final int TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);
    private static final int NETWORK_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(1);
    private static final int TWO_SECONDS = (int) TimeUnit.SECONDS.toMillis(2);
    private static final int FIVE_SECONDS = (int) TimeUnit.SECONDS.toMillis(5);
    private static final int ELEMENT_TIMEOUT = FIVE_SECONDS;
    private static final int SMALL_TIMEOUT = 500;

    private final Han solo;

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

    public boolean waitForElementToBeVisible(ViewElement viewElement) {
        return solo.waitForCondition(new IsVisibleElementCondition(viewElement), ELEMENT_TIMEOUT);
    }

    public boolean waitForElementToBeVisible(With matcher, int timeoutMs) {
        return solo.waitForCondition(new VisibleElementCondition(matcher), timeoutMs);
    }

    public boolean expectToastWithText(ToastObserver toastObserver, String text) {
        return solo.waitForCondition(new ToastWithTextCondition(toastObserver, text), TIMEOUT);
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
                solo.waitForCondition(condition, NETWORK_TIMEOUT);
            }
        });

        return true;
    }

    public boolean waitForKeyboardToBeShown() {
        return solo.waitForCondition(new KeyboardShownCondition(), ELEMENT_TIMEOUT);
    }

    @Deprecated //"Need for improvement"
    public void waitForPlayerPage() {
        solo.sleep(SMALL_TIMEOUT);
    }

    public void waitFiveSeconds() {
        solo.sleep(FIVE_SECONDS);
    }

    public void waitTwoSeconds() {
        solo.sleep(TWO_SECONDS);
    }

    public boolean waitForElementCondition(Condition condition) {
        return waitForElementCondition(condition, ELEMENT_TIMEOUT);
    }

    public boolean waitForElementCondition(Condition condition, int timeout) {
        return solo.waitForCondition(condition, timeout);
    }

    public boolean waitForElementToBeChecked(With matcher) {
        return solo.waitForCondition(new CheckedViewCondition(matcher), ELEMENT_TIMEOUT);
    }

    public boolean waitForNetworkCondition(Condition condition) {
        return solo.waitForCondition(condition, NETWORK_TIMEOUT);
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
        List<ViewElement> retryButtons = solo.findOnScreenElements(With.id(R.id.ak_emptyview_btn_retry));
        if (!retryButtons.isEmpty()) {
            ViewElement button = retryButtons.get(0);
            if (button.isOnScreen()) {
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

    public boolean waitForItemCountToIncrease(final RecyclerView.Adapter adapter, final int currentSize) {
        return solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return adapter.getItemCount() > currentSize;
            }
        }, TIMEOUT);
    }

    public boolean waitForPlaybackToBePlaying() {
        return solo.waitForCondition(new PlaybackPlayingCondition(), NETWORK_TIMEOUT);
    }

    public boolean waitForPlaybackToBeIdle() {
        return solo.waitForCondition(new PlaybackIdleCondition(), NETWORK_TIMEOUT);
    }

    public void waitForDialogToClose() {
        solo.waitForDialogToClose(NETWORK_TIMEOUT);
    }

    public void waitForActivity(Class<? extends Activity> activityClass) {
        solo.waitForCondition(new CurrentActivityCondition(activityClass), TIMEOUT);
    }

    public void assertForFragmentByTag(String fragmentTag) {
        assertThat("Fragment should be visible. Tag:" + fragmentTag, solo.waitForFragmentByTag(fragmentTag, TIMEOUT));
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
                return solo.findOnScreenElement(matcher).isOnScreen();
            }
        }, 15000);
    }

    public boolean waitForElement(final Class<? extends View> viewClass) {
        return solo.waitForCondition(new ByClassCondition(viewClass), TIMEOUT);
    }

    public boolean waitForElement(TextElement textView, String text) {
        return solo.waitForCondition(new TextInViewCondition(textView, text), ELEMENT_TIMEOUT);
    }

    public boolean waitForElements(int elementId) {
        return solo.waitForCondition(new VisibleElementsCondition(elementId), NETWORK_TIMEOUT);
    }

    public boolean waitForElement(With with) {
        return solo.waitForCondition(new VisibleElementCondition(with), ELEMENT_TIMEOUT);
    }

    public boolean waitForElementToHaveText(TextElement textElement) {
        return solo.waitForCondition(new ElementHasTextCondition(textElement), TWO_SECONDS);
    }

    public boolean waitForElementTextToChange(TextElement textElement) {
        return solo.waitForCondition(new ElementTextChangeCondition(textElement), TWO_SECONDS);
    }

    public boolean waitForElementTextToChange(TextElement textElement, String text) {
        return solo.waitForCondition(new ElementTextChangeCondition(textElement, text), TWO_SECONDS);
    }

    public boolean waitForAnimationToFinish(ViewElement viewElement) {
        return solo.waitForCondition(new AnimationFinishedCondition(viewElement), TWO_SECONDS);
    }

    private class AnimationFinishedCondition implements Condition {
        private ViewElement viewElement;

        AnimationFinishedCondition(ViewElement viewElement) {
            this.viewElement = viewElement;
        }

        @Override
        public boolean isSatisfied() {
            return !(viewElement.isAnimating());
        }
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
            ViewElement view = solo.findOnScreenElement(matcher);
            Log.i(TAG, "ViewID searched");
            return (view.isOnScreen());
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
            List<ViewElement> elements = solo.findOnScreenElements(With.id(viewId));
            Log.i(TAG, "ViewID searched");
            return (!elements.isEmpty());
        }
    }

    private class ByClassCondition implements Condition {
        private Class<? extends View> viewClass;

        ByClassCondition(Class<? extends View> viewClass) {
            this.viewClass = viewClass;
        }


        @Override
        public boolean isSatisfied() {
            Log.i(TAG, "FindViewByClass");
            ArrayList<? extends View> views = solo.getSolo().getCurrentViews(viewClass);

            return (!views.isEmpty() && views.get(0).isShown());
        }
    }

    private class ToastWithTextCondition implements Condition {
        private final ToastObserver toastObserver;
        private final String text;

        public ToastWithTextCondition(ToastObserver toastObserver, String text) {
            this.toastObserver = toastObserver;
            this.text = text;
        }

        @Override
        public boolean isSatisfied() {
            return toastObserver.wasToastObserved(text);
        }
    }

    private class NoProgressBarCondition implements Condition {
        @Override
        public boolean isSatisfied() {
            return !solo.isElementDisplayed(With.classSimpleName(ProgressBar.class.getSimpleName()));
        }
    }

    private class NoTextCondition implements Condition {
        private String searchedText;

        private NoTextCondition(String text) {
            searchedText = text;
        }

        @Override
        public boolean isSatisfied() {
            return !solo.isElementDisplayed(With.text(searchedText));
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


    private class KeyboardShownCondition implements Condition {
        @Override
        public boolean isSatisfied() {
            return solo.isKeyboardShown();
        }
    }

    private class TextInViewCondition implements Condition {
        private final TextElement view;
        private final String text;

        private TextInViewCondition(TextElement view, String text) {
            this.view = view;
            this.text = text;
        }

        @Override
        public boolean isSatisfied() {
            return view.getText().equals(text);
        }
    }

    private class CurrentActivityCondition implements Condition {

        private final Class<? extends Activity> activity;

        public CurrentActivityCondition(Class<? extends Activity> activityClass) {
            activity = activityClass;
        }

        @Override
        public boolean isSatisfied() {
            Log.i(TAG,
                  String.format("Waiting for Activity: %s, current Activity: %s",
                                activity.getSimpleName(),
                                solo.getCurrentActivity().toString()));
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
            return solo.findOnScreenElement(matcher).isChecked();
        }
    }

    private class ElementTextChangeCondition implements Condition {

        private final TextElement view;
        private final String text;

        public ElementTextChangeCondition(TextElement textElement) {
            view = textElement;
            text = textElement.getText();
        }

        public ElementTextChangeCondition(TextElement textElement, String initialText) {
            view = textElement;
            this.text = initialText;
        }

        @Override
        public boolean isSatisfied() {
            return !view.getText().equals(text);
        }
    }

    private class ElementHasTextCondition implements Condition {

        private final TextElement view;

        ElementHasTextCondition(TextElement textElement) {
            view = textElement;
        }

        @Override
        public boolean isSatisfied() {
            return !view.getText().isEmpty();
        }
    }

    private class IsVisibleElementCondition implements Condition {
        private final ViewElement view;

        public IsVisibleElementCondition(ViewElement viewElement) {
            view = viewElement;
        }

        @Override
        public boolean isSatisfied() {
            return view.isOnScreen();
        }
    }
}
