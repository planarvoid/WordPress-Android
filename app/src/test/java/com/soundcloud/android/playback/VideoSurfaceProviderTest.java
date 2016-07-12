package com.soundcloud.android.playback;

import android.view.Surface;
import android.view.TextureView;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class VideoSurfaceProviderTest extends AndroidUnitTest {

    @Mock VideoSurfaceProvider.VideoTextureContainerFactory containerFactory;
    @Mock VideoTextureContainer textureContainer;
    @Mock VideoSurfaceProvider.Listener listener;
    @Mock TextureView textureView;
    @Mock Surface surface;

    private VideoSurfaceProvider videoSurfaceProvider;

    private static final Urn URN = Urn.forAd("dfp", "video-ad");
    private static final Urn URN2 = Urn.forAd("dfp", "video-ad-2");

    @Before
    public void setUp() {
        videoSurfaceProvider = new VideoSurfaceProvider(containerFactory);
        videoSurfaceProvider.setListener(listener);

        when(containerFactory.build(URN, textureView, listener)).thenReturn(textureContainer);
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
        verify(textureContainer).attachSurfaceTexture(textureView);
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
}