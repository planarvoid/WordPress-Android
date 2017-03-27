package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.VideoSurfaceProvider.Listener;
import static com.soundcloud.android.playback.VideoSurfaceProvider.Origin;
import static org.assertj.core.api.Java6Assertions.assertThat;
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
    @Mock Listener listener;
    @Mock Listener listener2;
    @Mock TextureView textureView;
    @Mock TextureView textureView2;
    @Mock Surface surface;

    private VideoSurfaceProvider videoSurfaceProvider;

    private static final String UUID = "111-1111-111";
    private static final String UUID2 = "222-2222-222";

    private static final Origin ORIGIN = Origin.STREAM;
    private static final Origin ORIGIN2 = Origin.PLAYER;

    @Before
    public void setUp() {
        videoSurfaceProvider = new VideoSurfaceProvider(containerFactory);
        videoSurfaceProvider.addListener(listener);
        videoSurfaceProvider.addListener(listener2);

        when(containerFactory.build(UUID, ORIGIN, textureView, videoSurfaceProvider)).thenReturn(textureContainer);
        when(textureContainer.getOrigin()).thenReturn(ORIGIN);
        when(textureContainer.getUuid()).thenReturn(UUID);
    }

    @Test
    public void createsNewTextureViewContainerIfNewVideoUrn() {
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);

        verify(containerFactory).build(UUID, ORIGIN, textureView, videoSurfaceProvider);
    }

    @Test
    public void reusesTextureViewContainerIfContainerForUrnExists() {
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);

        verify(containerFactory).build(UUID, ORIGIN, textureView, videoSurfaceProvider);
        verify(textureContainer).reattachSurfaceTexture(textureView);
    }

    @Test
    public void releasesExistingContainerBeforeBuildingNewContainerIfRecyclingTextureView() {
        when(textureContainer.containsTextureView(textureView)).thenReturn(true);

        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);
        videoSurfaceProvider.setTextureView(UUID2, ORIGIN, textureView);

        verify(containerFactory).build(UUID, ORIGIN, textureView, videoSurfaceProvider);
        verify(textureContainer).release();
        verify(containerFactory).build(UUID2, ORIGIN, textureView, videoSurfaceProvider);
    }

    @Test
    public void releasesExistingContainerBeforeBuildingNewContainerIfNewOriginForExistingVideo() {
        when(textureContainer.containsTextureView(textureView)).thenReturn(true);

        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);
        videoSurfaceProvider.setTextureView(UUID, ORIGIN2, textureView2);

        verify(containerFactory).build(UUID, ORIGIN, textureView, videoSurfaceProvider);
        verify(textureContainer).release();
        verify(containerFactory).build(UUID, ORIGIN2, textureView2, videoSurfaceProvider);
    }

    @Test
    public void settingSurfaceTextureForwardsUpdateToListeners() {
        videoSurfaceProvider.setTextureView(UUID, ORIGIN, textureView);

        verify(listener).onTextureViewUpdate(UUID, textureView);
        verify(listener2).onTextureViewUpdate(UUID, textureView);
    }

    @Test
    public void attemptToSetSurfaceForwardsCallToListeners() {
        videoSurfaceProvider.attemptToSetSurface(UUID);

        verify(listener).attemptToSetSurface(UUID);
        verify(listener2).attemptToSetSurface(UUID);
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
