package com.soundcloud.android.view;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.ViewUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class NewItemsIndicatorScrollListenerTest extends AndroidUnitTest {

    @Mock NewItemsIndicatorScrollListener.Listener listener;

    private int threshold = ViewUtils.dpToPx(context(), 80) + 1;
    private NewItemsIndicatorScrollListener scrollListener;

    @Before
    public void setUp() {
        scrollListener = new NewItemsIndicatorScrollListener(context());
        scrollListener.setListener(listener);
    }

    @Test
    public void scrollDownBeyondThresholdEmitsHideToListener() {
        scrollListener.onScrolled(null, 0, threshold);

        verify(listener).onScrollHideOverlay();
    }

    @Test
    public void scrollUpBeyondThresholdEmitsHideToListener() {
        scrollListener.resetVisibility(false);

        scrollListener.onScrolled(null, 0, -threshold);

        verify(listener).onScrollShowOverlay();
    }

    @Test
    public void scrollUpAndDownBeyondThresholdEmitsHideAndShow() {
        scrollListener.onScrolled(null, 0, threshold);
        scrollListener.onScrolled(null, 0, -threshold);

        verify(listener).onScrollHideOverlay();
        verify(listener).onScrollShowOverlay();
    }

    @Test
    public void scrollDownWithinThresholdDoesNotHide() {
        scrollListener.onScrolled(null, 0, threshold-1);

        verify(listener, never()).onScrollHideOverlay();
    }

    @Test
    public void scrollUpWithinThresholdDoesNotShow() {
        scrollListener.resetVisibility(false);

        scrollListener.onScrolled(null, 0, -threshold+1);

        verify(listener, never()).onScrollShowOverlay();
    }

    @Test
    public void scrollAccumulatesBetweenThresholdEmits() {
        scrollListener.onScrolled(null, 0, threshold-2);
        scrollListener.onScrolled(null, 0, 1);
        scrollListener.onScrolled(null, 0, 1);

        verify(listener, times(1)).onScrollHideOverlay();
    }

    @Test
    public void scrollingThOtherDirectionResetsDistance() {
        scrollListener.onScrolled(null, 0, threshold-1);
        scrollListener.onScrolled(null, 0, -1);
        scrollListener.onScrolled(null, 0, threshold-1);

        verify(listener, never()).onScrollHideOverlay();
    }

    @Test
    public void scrollDownAndThenUpWithinThresholdDoesNotShowAgain() {
        scrollListener.onScrolled(null, 0, threshold);
        scrollListener.onScrolled(null, 0, -threshold+1);

        verify(listener).onScrollHideOverlay();
        verify(listener, never()).onScrollShowOverlay();
    }

    @Test
    public void disablingAutoResetDoesNotResetBetweenEmits() {
        scrollListener.disableAutoReset();

        scrollListener.onScrolled(null, 0, threshold);
        scrollListener.onScrolled(null, 0, -threshold);

        verify(listener).onScrollHideOverlay();
        verify(listener, never()).onScrollShowOverlay();
    }
}
