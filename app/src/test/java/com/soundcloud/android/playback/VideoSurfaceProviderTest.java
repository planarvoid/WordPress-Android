package com.soundcloud.android.playback;

import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class VideoSurfaceProviderTest extends AndroidUnitTest {

    @Mock ApplicationProperties applicationProperties;
    @Mock VideoTextureContainer.Factory containerFactory;
    @Mock VideoTextureContainer textureContainer;
    @Mock VideoSurfaceProvider.Listener surfaceProviderListener;
    @Mock TextureView textureView;
    @Mock Surface surface;

    private VideoSurfaceProvider videoSurfaceProvider;
    private Optional<VideoSurfaceProvider.Listener> listener;

    private static final Urn URN = Urn.forAd("dfp", "video-ad");
    private static final Urn URN2 = Urn.forAd("dfp", "video-ad-2");

    @Before
    public void setUp() {
        listener = Optional.of(surfaceProviderListener);

        videoSurfaceProvider = new VideoSurfaceProvider(applicationProperties, containerFactory);
        videoSurfaceProvider.setListener(surfaceProviderListener);

        when(containerFactory.build(URN, textureView, listener)).thenReturn(textureContainer);
        when(applicationProperties.canReattachSurfaceTexture()).thenReturn(true);
    }

    @Test
    public void createsNewTextureViewContainerIfNewVideoUrn() {
        videoSurfaceProvider.setTextureView(URN, textureView);

        verify(containerFactory).build(URN, textureView, listener);
    }

    @Test
    public void reusesTextureViewContainerIfContainerForUrnExists() {
        videoSurfaceProvider.setTextureView(URN, textureView);
        videoSurfaceProvider.setTextureView(URN, textureView);

        verify(containerFactory).build(URN, textureView, listener);
        verify(textureContainer).reattachSurfaceTexture(textureView);
    }

    @Test
    public void rebuildsTextureViewContainerForIceCreamSandwichIfContainerForUrnExists() {
        when(applicationProperties.canReattachSurfaceTexture()).thenReturn(false);

        videoSurfaceProvider.setTextureView(URN, textureView);
        videoSurfaceProvider.setTextureView(URN, textureView);

        verify(containerFactory, times(2)).build(URN, textureView, listener);
        verify(textureContainer, never()).reattachSurfaceTexture(textureView);
    }

    @Test
    public void releasesExistingContainerBeforeBuildingNewContainerIfRecyclingTextureView() {
        when(textureContainer.containsTextureView(textureView)).thenReturn(true);

        videoSurfaceProvider.setTextureView(URN, textureView);
        videoSurfaceProvider.setTextureView(URN2, textureView);

        verify(containerFactory).build(URN, textureView, listener);
        verify(textureContainer).release();
        verify(containerFactory).build(URN2, textureView, listener);
    }

    @Test
    public void settingSurfaceTextureForwardsUpdateToListener() {
        videoSurfaceProvider.setTextureView(URN, textureView);

        verify(surfaceProviderListener).onTextureViewUpdate(URN, textureView);
    }

    @Test
    public void canSetSurfaceTextureWithoutListener() {
        videoSurfaceProvider = new VideoSurfaceProvider(applicationProperties, containerFactory);

        videoSurfaceProvider.setTextureView(URN, textureView);

        verify(surfaceProviderListener, never()).onTextureViewUpdate(URN, textureView);
        verify(containerFactory).build(URN, textureView, Optional.<VideoSurfaceProvider.Listener>absent());
    }

    @Test
    public void onConfigurationChangeReleasesTextureViewReferenceFromContainer() {
        videoSurfaceProvider.setTextureView(URN, textureView);
        videoSurfaceProvider.onConfigurationChange();

        verify(textureContainer).releaseTextureView();
    }

    @Test
    public void onDestroyReleasesAllContainers() {
        videoSurfaceProvider.setTextureView(URN, textureView);
        videoSurfaceProvider.onDestroy();

        verify(textureContainer).release();
    }

    @Test
    public void getSurfaceReturnsSurfaceIfContainerForUrnExists() {
        when(textureContainer.getSurface()).thenReturn(surface);

        videoSurfaceProvider.setTextureView(URN, textureView);

        assertThat(videoSurfaceProvider.getSurface(URN)).isEqualTo(surface);
    }

    @Test
    public void getSurfaceReturnsNullIfContainerDoesNotExist() {
        assertThat(videoSurfaceProvider.getSurface(URN)).isNull();
    }

     @Test
    public void getTextureViewReturnsViewIfContainerForUrnExists() {
        when(textureContainer.getTextureView()).thenReturn(textureView);

        videoSurfaceProvider.setTextureView(URN, textureView);

        assertThat(videoSurfaceProvider.getTextureView(URN)).isEqualTo(Optional.of(textureView));
    }

    @Test
    public void getTextureViewReturnsAbsentIfContainerDoesNotExist() {
        assertThat(videoSurfaceProvider.getTextureView(URN)).isEqualTo(Optional.absent());
    }
}