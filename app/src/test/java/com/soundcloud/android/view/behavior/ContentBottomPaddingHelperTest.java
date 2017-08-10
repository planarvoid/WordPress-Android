package com.soundcloud.android.view.behavior;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class ContentBottomPaddingHelperTest {

    private static final int PLAYER_PEEK_HEIGHT = 50;
    private static final int DEFAULT_PADDING = 100;

    @Mock View playerView;
    @Mock View dependentView;
    @Mock PlayerBehaviorFactory playerBehaviorFactory;
    @Mock PlayerBehavior playerBehavior;

    ContentBottomPaddingHelper helper;

    @Before
    public void setUp() {
        setupPlayerBehavior();

        when(playerBehavior.getPeekHeight()).thenReturn(PLAYER_PEEK_HEIGHT);

        when(dependentView.getPaddingLeft()).thenReturn(DEFAULT_PADDING);
        when(dependentView.getPaddingTop()).thenReturn(DEFAULT_PADDING);
        when(dependentView.getPaddingRight()).thenReturn(DEFAULT_PADDING);
        when(dependentView.getPaddingBottom()).thenReturn(DEFAULT_PADDING);

        when(playerBehaviorFactory.create(any())).thenReturn(playerBehavior);

        helper = new ContentBottomPaddingHelper(playerBehaviorFactory);
    }

    @Test
    public void isPlayerReturningTrueWithThePlayerId() {
        when(playerView.getId()).thenReturn(R.id.player_root);

        boolean isPlayer = helper.isPlayer(playerView);

        assertThat(isPlayer).isTrue();
    }

    @Test
    public void removesBottomPaddingOnPlayerHidden() {
        givenPlayerIsHidden();
        givenDependentViewIsPadded();

        helper.onPlayerChanged(playerView, dependentView);

        verify(dependentView).setPadding(eq(DEFAULT_PADDING), eq(DEFAULT_PADDING), eq(DEFAULT_PADDING), eq(DEFAULT_PADDING));
    }

    @Test
    public void addsBottomPaddingOnPlayerNotHidden() {
        givenPlayerIsNotHidden();
        givenDependentViewIsNotPadded();

        helper.onPlayerChanged(playerView, dependentView);

        verify(dependentView).setPadding(eq(DEFAULT_PADDING), eq(DEFAULT_PADDING), eq(DEFAULT_PADDING), eq(DEFAULT_PADDING + PLAYER_PEEK_HEIGHT));
    }

    @Test
    public void doesNotAddBottomPaddingIfAlreadyPadded() {
        givenPlayerIsNotHidden();
        givenDependentViewIsPadded();

        helper.onPlayerChanged(playerView, dependentView);

        verify(dependentView, never()).setPadding(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void doesNotRemoveBottomPaddingIfNotPadded() {
        givenPlayerIsHidden();
        givenDependentViewIsNotPadded();

        helper.onPlayerChanged(playerView, dependentView);

        verify(dependentView, never()).setPadding(anyInt(), anyInt(), anyInt(), anyInt());
    }

    private void givenDependentViewIsNotPadded() {
        when(dependentView.getTag(R.id.content_view_bottom_padded)).thenReturn(null);
        when(dependentView.getPaddingBottom()).thenReturn(DEFAULT_PADDING);
    }

    private void givenDependentViewIsPadded() {
        when(dependentView.getTag(R.id.content_view_bottom_padded)).thenReturn(ContentBottomPaddingHelper.IS_PADDED);
        when(dependentView.getPaddingBottom()).thenReturn(DEFAULT_PADDING + PLAYER_PEEK_HEIGHT);
    }

    private void givenPlayerIsHidden() {
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void givenPlayerIsNotHidden() {
        when(playerBehavior.getState()).thenReturn(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @SuppressWarnings("unchecked")
    private void setupPlayerBehavior() {
        BottomSheetBehavior<View> bottomSheetBehavior = mock(BottomSheetBehavior.class);
        CoordinatorLayout.LayoutParams layoutParams = mock(CoordinatorLayout.LayoutParams.class);
        when(layoutParams.getBehavior()).thenReturn(bottomSheetBehavior);
        when(playerView.getLayoutParams()).thenReturn(layoutParams);
    }
}
