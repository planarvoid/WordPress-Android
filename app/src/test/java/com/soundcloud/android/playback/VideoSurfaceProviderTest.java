package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.VideoSurfaceProvider.Listener;
import static com.soundcloud.android.playback.VideoSurfaceProvider.Origin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.Surface;
import android.view.TextureView;


public class VideoSurfaceProviderTest extends AndroidUnitTest {

    @Mock VideoTextureContainer.Factory containerFactory;
    @Mock VideoTextureContainer textureContainer;
    @Mock Listener surfaceProviderListener;
    @Mock TextureView textureView;
    @Mock Surface surface;

    private VideoSurfaceProvider videoSurfaceProvider;
    private Optional<VideoSurfaceProvider.Listener> listener;

    private static final String UUID = "111-1111-111";
    private static final String UUID2 = "222-2222-222";

    private static final Origin ORIGIN = Origin.STREAM;
    private static final Origin ORIGIN2 = Origin.PLAYER;

    @Before
    public void setUp() {
        listener = Optional.of(surfaceProviderListener);

        videoSurfaceProvider = new VideoSurfaceProvider(containerFactory);
        videoSurfaceProvider.setListener(surfaceProviderListener);

        when(containerFactory.build(UUID, ORIGIN, textureView, listener)).thenReturn(textureContainer);
        when(textureContainer.getOrigin()).thenReturn(ORIGIN);
    }

    @Test
    public void createsNewTextureViewContainerIfNewVideoUrn() {
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);

        verify(containerFactory).build(UUID, ORIGIN, textureView, listener);
    }

    @Test
    public void reusesTextureViewContainerIfContainerForUrnExists() {
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);

        verify(containerFactory).build(UUID, ORIGIN, textureView, listener);
        verify(textureContainer).reattachSurfaceTexture(textureView);
    }

    @Test
    public void releasesExistingContainerBeforeBuildingNewContainerIfRecyclingTextureView() {
        when(textureContainer.containsTextureView(textureView)).thenReturn(true);

        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);
        videoSurfaceProvider.setTextureView(UUID2, ORIGIN, textureView);

        verify(containerFactory).build(UUID, ORIGIN, textureView, listener);
        verify(textureContainer).release();
        verify(containerFactory).build(UUID2, ORIGIN, textureView, listener);
    }

    @Test
    public void settingSurfaceTextureForwardsUpdateToListener() {
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);

        verify(surfaceProviderListener).onTextureViewUpdate(UUID, textureView);
    }

    @Test
    public void canSetSurfaceTextureWithoutListener() {
        videoSurfaceProvider = new VideoSurfaceProvider(containerFactory);

        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);

        verify(surfaceProviderListener, never()).onTextureViewUpdate(UUID, textureView);
        verify(containerFactory).build(UUID, ORIGIN, textureView, Optional.absent());
    }

    @Test
    public void onConfigurationChangeReleasesTextureViewReferenceFromContainerForORIGIN() {
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);
        videoSurfaceProvider.onConfigurationChange(ORIGIN);

        verify(textureContainer).releaseTextureView();
    }

    @Test
    public void onConfigurationChangeDoesntReleasesTextureViewReferenceForOtherORIGINs() {
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);
        videoSurfaceProvider.onConfigurationChange(ORIGIN2);

        verify(textureContainer, never()).releaseTextureView();
    }

    @Test
    public void onDestroyReleasesAllContainersForORIGIN() {
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);
        videoSurfaceProvider.onDestroy(ORIGIN);

        verify(textureContainer).release();
    }

    @Test
    public void onDestroyDoesntReleaseContainerForOtherORIGINs() {
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);
        videoSurfaceProvider.onDestroy(ORIGIN2);

        verify(textureContainer, never()).release();
    }

    @Test
    public void getSurfaceReturnsSurfaceIfContainerForUrnExists() {
        when(textureContainer.getSurface()).thenReturn(surface);

        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);

        assertThat(videoSurfaceProvider.getSurface(UUID)).isEqualTo(surface);
    }

    @Test
    public void getSurfaceReturnsNullIfContainerDoesNotExist() {
        assertThat(videoSurfaceProvider.getSurface(UUID)).isNull();
    }

    @Test
    public void getTextureViewReturnsViewIfContainerForUrnExists() {
        when(textureContainer.getTextureView()).thenReturn(textureView);

        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);

        assertThat(videoSurfaceProvider.getTextureView(UUID)).isEqualTo(Optional.of(textureView));
    }

    @Test
    public void getTextureViewReturnsAbsentIfContainerDoesNotExist() {
        assertThat(videoSurfaceProvider.getTextureView(UUID)).isEqualTo(Optional.absent());
    }
}
