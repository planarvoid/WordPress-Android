package com.soundcloud.android.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;

public class NewItemsIndicatorTest extends AndroidUnitTest {

    private static final int TEXT_RESOURCE_ID = R.plurals.stream_new_posts;
    private static final int REFRESH_OVERLAY_TEXT_ID = R.id.refresh_overlay_text;
    private static final String NEW_POST = "1 new post";
    private static final String NEW_POSTS_3 = "3 new posts";
    private static final String NEW_POSTS_9_PLUS = "9+ new posts";


    @Mock NewItemsIndicatorScrollListener scrollListener;
    @Mock NewItemsIndicator.Listener listener;
    @Mock View view;
    @Mock TextView textView;
    @Mock Animation animation;

    @Captor private ArgumentCaptor<View.OnClickListener> onClickListenerCaptor;

    private NewItemsIndicator newItemsIndicator;

    @Before
    public void setUp() throws Exception {
        when(view.isShown()).thenReturn(true);
        when(view.getAnimation()).thenReturn(animation);
        when(view.findViewById(REFRESH_OVERLAY_TEXT_ID)).thenReturn(textView);

        newItemsIndicator = new NewItemsIndicator(context(), scrollListener);
        newItemsIndicator.setNewItems(2);
        newItemsIndicator.setClickListener(listener);
        newItemsIndicator.setTextResourceId(TEXT_RESOURCE_ID);
        newItemsIndicator.setView(view);
    }

    @Test
    public void shouldInvokeListenerOnRefreshableOverlayClicked() {
        verify(view).setOnClickListener(onClickListenerCaptor.capture());

        onClickListenerCaptor.getValue().onClick(view);

        verify(listener).onRefreshableOverlayClicked();
    }

    @Test
    public void shouldHideOnOverlayClicked() {
        verify(view).setOnClickListener(onClickListenerCaptor.capture());

        onClickListenerCaptor.getValue().onClick(view);

        verify(view).setVisibility(View.GONE);
    }

    @Test
    public void shouldResetNewItemsOnOverlayClicked() {
        verify(view).setOnClickListener(onClickListenerCaptor.capture());

        onClickListenerCaptor.getValue().onClick(view);

        assertThat(newItemsIndicator.getNewItems()).isEqualTo(0);
    }

    @Test
    public void shouldShowWhenNewItemsIsGreaterThanZeroOnUpdate() {
        when(view.isShown()).thenReturn(false);

        newItemsIndicator.update(3);

        verify(view).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldNotShowWhenNewItemsIsZeroOnUpdate() {
        when(view.isShown()).thenReturn(false);

        newItemsIndicator.update(0);

        verify(view, never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldNotShowWhenAlreadyVisibleOnUpdate() {
        when(view.isShown()).thenReturn(true);

        newItemsIndicator.update(3);

        verify(view, never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldSetTextWhenIsVisibleOnUpdate() {
        when(view.isShown()).thenReturn(true);

        newItemsIndicator.update(3);

        verify(textView).setText(NEW_POSTS_3);
    }

    @Test
    public void shouldSetTextToNinePlusWhenNewItemsIsGreaterThanNine() {
        when(view.isShown()).thenReturn(true);

        newItemsIndicator.update(12);

        verify(textView).setText(NEW_POSTS_9_PLUS);
    }

    @Test
    public void shouldSetQuantityText() {
        newItemsIndicator.setNewItems(0);

        newItemsIndicator.update(1);

        verify(textView).setText(NEW_POST);
    }

    @Test
    public void shouldAnimateWhenNotVisibleOnUpdate() {
        when(view.isShown()).thenReturn(false);

        newItemsIndicator.update(3);

        verify(view).setAnimation(any(Animation.class));
    }

    @Test
    public void shouldNotAnimateWhenVisibleOnUpdate() {
        when(view.isShown()).thenReturn(true);

        newItemsIndicator.update(3);

        verify(view, never()).setAnimation(any(Animation.class));
    }

    @Test
    public void shouldHideOnScrollDownWhenVisible() {
        when(view.isShown()).thenReturn(true);

        newItemsIndicator.onScrollHideOverlay();

        verify(view).setVisibility(View.GONE);
    }

    @Test
    public void shouldShowOnScrollUpWhenHidden() {
        when(view.isShown()).thenReturn(false);

        newItemsIndicator.onScrollShowOverlay();

        verify(view).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldHideWhenAnimationEndedOnScroll() {
        when(animation.hasStarted()).thenReturn(true);
        when(animation.hasEnded()).thenReturn(true);

        newItemsIndicator.onScrollHideOverlay();

        verify(view).setVisibility(View.GONE);
    }

    @Test
    public void shouldNotHideWhenIsAlreadyAnimatingOnScroll() {
        when(animation.hasStarted()).thenReturn(true);
        when(animation.hasEnded()).thenReturn(false);

        newItemsIndicator.onScrollHideOverlay();

        verify(view, never()).setVisibility(View.GONE);
    }

    @Test
    public void shouldClearScrollListenerOnClear() {
        newItemsIndicator.destroy();

        verify(scrollListener).destroy();
    }

    @Test
    public void shouldResetNewItemsOnClear() {
        newItemsIndicator.destroy();

        assertThat(newItemsIndicator.getNewItems()).isEqualTo(0);
    }

    @Test
    public void shouldHideOnClear() {
        newItemsIndicator.destroy();

        verify(view).setVisibility(View.GONE);
    }

    @Test
    public void shouldResetNewItemsOnReset() {
        newItemsIndicator.hideAndReset();

        assertThat(newItemsIndicator.getNewItems()).isEqualTo(0);
    }

    @Test
    public void shouldHideOnReset() {
        newItemsIndicator.hideAndReset();

        verify(view).setVisibility(View.GONE);
    }
}
