package com.soundcloud.android.introductoryoverlay;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;

public class IntroductoryOverlayPresenterTest extends AndroidUnitTest {

    private IntroductoryOverlayPresenter presenter;
    private final String fakeKey = "key";

    @Mock private IntroductoryOverlayOperations operations;
    @Mock private View overlayTargetView;

    @Before
    public void setUp() {
        when(overlayTargetView.getContext()).thenReturn(activity());

        presenter = new IntroductoryOverlayPresenter(operations);
    }

    @Test
    public void noOpIfOverlayWasAlreadyShown() {
        when(operations.wasOverlayShown(fakeKey)).thenReturn(true);

        presenter.showIfNeeded(fakeKey, overlayTargetView, "tt", "ds");

        verify(operations, never()).setOverlayShown(fakeKey);
    }

    @Test
    public void markOverlayAsShownAfterFirstTimeShown() {
        when(operations.wasOverlayShown(fakeKey)).thenReturn(false);

        presenter.showIfNeeded(fakeKey, overlayTargetView, "tt", "ds");

        verify(operations).setOverlayShown(fakeKey);
    }

}