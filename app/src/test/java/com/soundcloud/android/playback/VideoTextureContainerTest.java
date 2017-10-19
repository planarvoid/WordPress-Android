package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.VideoSurfaceProvider.Origin;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class VideoTextureContainerTest {

    @Mock VideoTextureContainer.Listener textureContainerListener;
    @Mock View view;
    @Mock TextureView textureView;
    @Mock TextureView textureView2;
    @Mock SurfaceTexture surfaceTexture;

    private VideoTextureContainer textureContainer;

    private static final String UUID = "uid-uuid-uid";
    private static final Origin ORIGIN = Origin.PLAYER;

    @Before
    public void setUp() {
        textureContainer = new VideoTextureContainer(UUID, ORIGIN, textureView, null, textureContainerListener);
    }

    @Test
    public void containsTextureViewReturnsTrueForTextureViewWhenCreated() {
        assertThat(textureContainer.containsTextureView(textureView)).isTrue();
    }

    @Test
    public void getSurfaceReturnsSurfaceWhenSurfaceTextureReadyAndNullOtherwise() {
        assertThat(textureContainer.getSurface()).isNull();

        textureContainer.onSurfaceTextureAvailable(surfaceTexture, 0, 0);

        assertThat(textureContainer.getSurface()).isInstanceOf(Surface.class);
    }

    @Test
    public void surfaceTextureWillBeSetAtmostOnceOnSameTextureView() {
        when(textureView.getSurfaceTexture()).thenReturn(surfaceTexture);
        textureContainer.onSurfaceTextureAvailable(surfaceTexture, 0, 0);

        textureContainer.reattachSurfaceTexture(textureView, null);

        verify(textureView, never()).setSurfaceTexture(surfaceTexture);
    }

    @Test
    public void surfaceTextureWillBeSetToNewTextureView() {
        textureContainer.onSurfaceTextureAvailable(surfaceTexture, 0, 0);

        textureContainer.reattachSurfaceTexture(textureView2, null);

        verify(textureView2).setSurfaceTexture(surfaceTexture);
    }

    @Test
    public void surfaceTextureWithViewabilityViewWillBeSetToNewTextureView() {
        textureContainer.onSurfaceTextureAvailable(surfaceTexture, 0, 0);

        textureContainer.reattachSurfaceTexture(textureView2, view);

        verify(textureView2).setSurfaceTexture(surfaceTexture);
        assertThat(textureContainer.getViewabilityView()).isNotNull();
        assertThat(textureContainer.getViewabilityView()).isEqualTo(view);
    }

    @Test
    public void getUrnReturnsUrnOfContainer() {
        assertThat(textureContainer.getUuid()).isEqualTo(UUID);
    }

    @Test
    public void getScreenReturnsScreenOfContainer() {
        assertThat(textureContainer.getOrigin()).isEqualTo(ORIGIN);
    }

    @Test
    public void onSurfaceTextureAvailableAttemptsToSetSurfaceOnListener() {
        textureContainer.onSurfaceTextureAvailable(surfaceTexture, 0, 0);

        verify(textureContainerListener).attemptToSetSurface(UUID);
    }

    @Test
    public void onSurfaceTextureAvailableIsAbleToSetSurfaceWithoutListener() {
        textureContainer.onSurfaceTextureAvailable(surfaceTexture, 0, 0);

        assertThat(textureContainer.getSurface()).isNotNull();
    }

    @Test
    public void onSurfaceTextureDestroyedReturnsFalseIfSurfaceTextureSet() {
        textureContainer.onSurfaceTextureAvailable(surfaceTexture, 0, 0);

        assertThat(textureContainer.onSurfaceTextureDestroyed(surfaceTexture)).isFalse();
    }
}
