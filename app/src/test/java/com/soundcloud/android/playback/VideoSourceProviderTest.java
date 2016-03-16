package com.soundcloud.android.playback;

import android.media.CamcorderProfile;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.ApiVideoSource;
import com.soundcloud.android.ads.VideoSource;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class VideoSourceProviderTest extends AndroidUnitTest {

    private static ApiVideoSource SOURCE_360P = createApiVideoSource(480, 360, PlaybackConstants.MIME_TYPE_MP4, 736);
    private static ApiVideoSource SOURCE_480P = createApiVideoSource(858, 480, PlaybackConstants.MIME_TYPE_MP4, 1000);
    private static ApiVideoSource SOURCE_720P = createApiVideoSource(1920, 720, PlaybackConstants.MIME_TYPE_MP4, 2128);
    private static ApiVideoSource SOURCE_1080P = createApiVideoSource(1280, 1080, PlaybackConstants.MIME_TYPE_MP4, 3628);
    private static final List<ApiVideoSource> VALID_SOURCES = Arrays.asList(SOURCE_480P, SOURCE_720P, SOURCE_1080P, SOURCE_360P);

    @Mock private ApplicationProperties applicationProperties;
    @Mock private DeviceHelper deviceHelper;
    @Mock private MediaCodecInfoProvider mediaCodecInfoProvider;
    @Mock private NetworkConnectionHelper networkConnectionHelper;

    private VideoSourceProvider videoSourceProvider;
    private VideoPlaybackItem videoPlaybackItem;

    @Before
    public void setUp() {
        when(deviceHelper.hasCamcorderProfile(anyInt())).thenReturn(false);
        when(applicationProperties.canAccessCodecInformation()).thenReturn(false);
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.WIFI);

        videoSourceProvider = new VideoSourceProvider(applicationProperties, deviceHelper, mediaCodecInfoProvider, networkConnectionHelper);
        videoPlaybackItem = VideoPlaybackItem.create(AdFixtures.getVideoAd(Urn.forTrack(123L), VALID_SOURCES));
    }

    @Test
    public void failsIfNoValidFormatsProvided() throws IllegalArgumentException {
        final ApiVideoSource invalidSource = createApiVideoSource(100, 200, "video/invalid", 12);
        videoPlaybackItem = VideoPlaybackItem.create(AdFixtures.getVideoAd(Urn.forTrack(123L), invalidSource));

        try {
            videoSourceProvider.selectOptimalSource(videoPlaybackItem);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isNotEmpty();
        }
    }

    @Test
    public void deviceCapableUpTo1080pViaCamcorderProfileAndOnWIFIReturns1080pSource() {
        when(deviceHelper.hasCamcorderProfile(CamcorderProfile.QUALITY_1080P)).thenReturn(true);
        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);

        assertVideoSource(videoSource, SOURCE_1080P);
    }

    @Test
    public void deviceCapableUpTo1080pViaCamcorderProfileAndOnUnknownConnectionReturnsLowestSourceAsDefault() {
        when(deviceHelper.hasCamcorderProfile(CamcorderProfile.QUALITY_1080P)).thenReturn(true);
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.UNKNOWN);
        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);

        assertVideoSource(videoSource, SOURCE_360P);
    }

    @Test
    public void deviceCapableUpTo720pViaCamcorderProfileAndOnWIFIReturns720pSource() {
        when(deviceHelper.hasCamcorderProfile(CamcorderProfile.QUALITY_720P)).thenReturn(true);
        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);

        assertVideoSource(videoSource, SOURCE_720P);
    }

    @Test
    public void deviceCapableUpTo480pViaCamcorderProfileAndOnWIFIReturns480pSource() {
        when(deviceHelper.hasCamcorderProfile(CamcorderProfile.QUALITY_480P)).thenReturn(true);
        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);

        assertVideoSource(videoSource, SOURCE_480P);
    }

    @Test
    public void deviceCapableUpTo720pViaCamcorderProfileAndOn3GReturns360pSource() {
        when(deviceHelper.hasCamcorderProfile(CamcorderProfile.QUALITY_720P)).thenReturn(true);
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.THREE_G);

        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);

        assertVideoSource(videoSource, SOURCE_360P);
    }

    @Test
    public void deviceCapableOfCamcorderProfile720pReturns360pSourceFor2GAsFallback() {
        when(deviceHelper.hasCamcorderProfile(CamcorderProfile.QUALITY_720P)).thenReturn(true);
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.TWO_G);

        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);
        assertVideoSource(videoSource, SOURCE_360P);
    }

    @Test
    public void deviceNotCapableOfAnyCamcorderProfileAndCantAccessDecoderCodecReturns360pAsDefault() {
        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);

        assertVideoSource(videoSource, SOURCE_360P);
    }

    @Test
    public void deviceNotCapableOfCamcorderProfileAndCodecCapableOf1080PReturns1080PSourceForWIFI() {
        when(applicationProperties.canAccessCodecInformation()).thenReturn(true);
        when(mediaCodecInfoProvider.maxResolutionSupportForAvcOnDevice()).thenReturn(PlaybackConstants.RESOLUTION_PX_1080P);

        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);
        assertVideoSource(videoSource, SOURCE_1080P);
    }

    @Test
    public void deviceNotCapableOfCamcorderProfileAndCodecCapableOf1080PReturns360PSourceFor3G() {
        when(applicationProperties.canAccessCodecInformation()).thenReturn(true);
        when(mediaCodecInfoProvider.maxResolutionSupportForAvcOnDevice()).thenReturn(PlaybackConstants.RESOLUTION_PX_1080P);
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.THREE_G);

        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);
        assertVideoSource(videoSource, SOURCE_360P);
    }

    @Test
    public void deviceNotCapableOfCamcorderProfileAndCodecCapableOf720PReturns720PSourceForWIFI() {
        when(applicationProperties.canAccessCodecInformation()).thenReturn(true);
        when(mediaCodecInfoProvider.maxResolutionSupportForAvcOnDevice()).thenReturn(PlaybackConstants.RESOLUTION_PX_720P);

        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);
        assertVideoSource(videoSource, SOURCE_720P);
    }

    @Test
    public void videoSourceProviderRecognizes1080x1920as1080p() {
        when(deviceHelper.hasCamcorderProfile(CamcorderProfile.QUALITY_1080P)).thenReturn(true);
        final ApiVideoSource VERTICAL_1080P = createApiVideoSource(1080, 1920, PlaybackConstants.MIME_TYPE_MP4, 12);
        videoPlaybackItem = VideoPlaybackItem.create(AdFixtures.getVideoAd(Urn.forTrack(123L), Collections.singletonList(VERTICAL_1080P)));

        final VideoSource videoSource = videoSourceProvider.selectOptimalSource(videoPlaybackItem);

        assertVideoSource(videoSource, VERTICAL_1080P);
    }

    private void assertVideoSource(VideoSource videoSource, ApiVideoSource apiVideoSource) {
        assertThat(videoSource.getBitRate()).isEqualTo(apiVideoSource.getBitRate());
        assertThat(videoSource.getWidth()).isEqualTo(apiVideoSource.getWidth());
        assertThat(videoSource.getHeight()).isEqualTo(apiVideoSource.getHeight());
        assertThat(videoSource.getType()).isEqualTo(apiVideoSource.getType());
    }

    private static ApiVideoSource createApiVideoSource(int width, int height, String type, int bitRate) {
       return AdFixtures.getApiVideoSource(width, height, type, bitRate);
    }
}
