package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.graphics.Rect;
import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class PlayerViewVisibilityProviderTest {

    @Mock private PlayerTrackPager playerTrackPager;
    @Mock private View view;

    @Test
    public void isCurrentlyVisibleReturnsFalseIfReferenceToPagerLost() {
        assertThat(new PlayerViewVisibilityProvider(null).isCurrentlyVisible(view)).isFalse();
    }

    @Test
    public void isCurrentlyVisibleReturnsGetLocalVisibleRect() {
        when(view.getLocalVisibleRect(any(Rect.class))).thenReturn(true);
        assertThat(new PlayerViewVisibilityProvider(playerTrackPager).isCurrentlyVisible(view)).isTrue();
    }
}
