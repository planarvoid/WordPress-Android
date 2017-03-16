package com.soundcloud.android.playback;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.android.testsupport.AndroidUnitTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.soundcloud.android.playback.VideoSurfaceProvider.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VideoTextureContainerTest extends AndroidUnitTest {

    @Mock VideoTextureContainer.Listener textureContainerListener;
    @Mock TextureView textureView;
    @Mock TextureView textureView2;
    @Mock SurfaceTexture surfaceTexture;

    private VideoTextureContainer textureContainer;

    private static final String UUID = "uid-uuid-uid";
    private static final Origin ORIGIN = Origin.PLAYER;

    @Before
    public void setUp() {
        textureContainer = new VideoTextureContainer(UUID, ORIGIN, textureView, textureContainerListener);
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

        textureContainer.reattachSurfaceTexture(textureView);

        verify(textureView, never()).setSurfaceTexture(surfaceTexture);
    }

    @Test
    public void surfaceTextureWillBeSetToNewTextureView() {
        when(textureView.getSurfaceTexture()).thenReturn(surfaceTexture);
        textureContainer.onSurfaceTextureAvailable(surfaceTexture, 0, 0);

        textureContainer.reattachSurfaceTexture(textureView2);

        verify(textureView2).setSurfaceTexture(surfaceTexture);
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