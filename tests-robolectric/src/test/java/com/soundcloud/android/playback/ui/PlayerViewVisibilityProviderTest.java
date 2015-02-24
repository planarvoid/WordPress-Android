package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.graphics.Rect;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PlayerViewVisibilityProviderTest {

    @Mock private PlayerTrackPager playerTrackPager;
    @Mock private View view;

    @Test
    public void isCurrentlyVisibleReturnsFalseIfReferenceToPagerLost() {
        expect(new PlayerViewVisibilityProvider(null).isCurrentlyVisible(view)).toBeFalse();
    }

    @Test
    public void isCurrentlyVisibleReturnsGetLocalVisibleRect() {
        when(view.getLocalVisibleRect(any(Rect.class))).thenReturn(true);
        expect(new PlayerViewVisibilityProvider(playerTrackPager).isCurrentlyVisible(view)).toBeTrue();
    }
}